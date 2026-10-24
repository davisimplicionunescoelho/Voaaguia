package com.voaaguia;

import com.voaaguia.domain.User;
import com.voaaguia.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP and MongoDB integration. Uses an isolated, randomly named database. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest {
    static final String DB="voaaguia_test_"+UUID.randomUUID().toString().replace("-","");
    @DynamicPropertySource static void settings(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/"+DB);
        registry.add("app.jwt-secret", () -> "integration-test-secret-only-at-least-32-bytes");
        registry.add("app.seed-enabled", () -> "false");
        registry.add("app.gemini-key", () -> "");
    }
    @Autowired TestRestTemplate http;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired MongoTemplate mongo;
    String operator,other,manager,leader;
    @BeforeAll void setup() {
        operator=create("operator","OPERADOR"); other=create("other","OPERADOR"); manager=create("manager","GESTOR"); leader=create("leader","LIDER");
    }
    String create(String name,String role) {
        var u=new User(); u.name=name; u.email=name+"@test.local"; u.role=role; u.department="Testes"; u.passwordHash=passwords.encode("Password12345!"); users.save(u);
        return (String)call("POST","/auth/login",null,Map.of("email",u.email,"password","Password12345!")).getBody().get("token");
    }
    ResponseEntity<Map> call(String method,String path,String token,Object body) {
        var headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON); if(token!=null) headers.setBearerAuth(token);
        return http.exchange("/api"+path,HttpMethod.valueOf(method),new HttpEntity<>(body,headers),Map.class);
    }
    Map<String,Object> ideaBody(String strategy) { return Map.of("title","Reduzir consumo","description","Otimizar o planejamento das rotas","category","Logística","strategyId",strategy); }
    String strategy() {
        var result=call("POST","/strategies",leader,Map.of("title","Eficiência","description","Reduzir desperdício","category","Operações","campaign","2026","isActive",true));
        assertThat(result.getStatusCode().value()).isEqualTo(201); return (String)result.getBody().get("id");
    }
    @Test void authenticationAndRoleBoundaries() {
        assertThat(call("GET","/auth/me",null,null).getStatusCode().value()).isEqualTo(401);
        assertThat(call("POST","/auth/login",null,Map.of("email","operator@test.local","password","wrong")).getStatusCode().value()).isEqualTo(401);
        assertThat(call("POST","/strategies",operator,Map.of("title","x","description","y","category","z","campaign","a","isActive",true)).getStatusCode().value()).isEqualTo(403);
        assertThat(call("GET","/reports/dashboard",manager,null).getStatusCode().value()).isEqualTo(403);
        assertThat(call("GET","/reports/dashboard",operator,null).getStatusCode().value()).isEqualTo(403);
        assertThat(call("GET","/auth/me",operator,null).getBody()).doesNotContainKey("passwordHash");
    }
    @Test void registrationCannotEscalateRole() {
        var result=call("POST","/auth/register",null,Map.of("email","registered@test.local","password","Password12345!","name","Pessoa","department","Operação","role","LIDER"));
        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat((Map)result.getBody().get("user")).containsEntry("role","OPERADOR");
        assertThat(call("POST","/auth/register",null,Map.of("email","registered@test.local","password","Password12345!","name","Pessoa","department","Operação")).getStatusCode().value()).isEqualTo(409);
    }
    @Test void ownershipValidationAndSoftDelete() {
        var sid=strategy(); var created=call("POST","/ideas",operator,ideaBody(sid)); var id=(String)created.getBody().get("id");
        assertThat(call("GET","/ideas/"+id,other,null).getStatusCode().value()).isEqualTo(404);
        assertThat(call("PUT","/ideas/"+id,other,ideaBody(sid)).getStatusCode().value()).isEqualTo(404);
        assertThat(call("PUT","/ideas/"+id,operator,ideaBody(sid)).getStatusCode().value()).isEqualTo(200);
        assertThat(call("POST","/ideas",operator,Map.of("title","","description","","category","","strategyId",sid)).getStatusCode().value()).isEqualTo(400);
        assertThat(call("DELETE","/ideas/"+id,operator,null).getStatusCode().value()).isEqualTo(204);
        assertThat(call("GET","/ideas/"+id,operator,null).getStatusCode().value()).isEqualTo(404);
    }
    @Test void fullInnovationFlowAndReports() {
        var sid=strategy(); var idea=call("POST","/ideas",operator,ideaBody(sid)).getBody(); var iid=(String)idea.get("id");
        var review=Map.of("status","APROVADA","priority","ALTA","feedback","Alinhada à campanha");
        assertThat(call("PATCH","/ideas/"+iid+"/review",operator,review).getStatusCode().value()).isEqualTo(403);
        assertThat(call("PATCH","/ideas/"+iid+"/review",manager,review).getStatusCode().value()).isEqualTo(200);
        assertThat(call("PATCH","/ideas/"+iid+"/review",manager,review).getStatusCode().value()).isEqualTo(200);
        assertThat(call("POST","/ideas/"+iid+"/vote",manager,Map.of()).getBody()).containsEntry("votes",1);
        assertThat(call("POST","/ideas/"+iid+"/vote",manager,Map.of()).getBody()).containsEntry("votes",1);
        Map<String,Object> p=new HashMap<>(); p.put("title","Rotas eficientes"); p.put("description","Projeto piloto"); p.put("ideaId",iid); p.put("strategyId",sid); p.put("status","EM_ANDAMENTO"); p.put("stage","PILOTO"); p.put("startDate","2026-01-01T00:00:00Z"); p.put("expectedEndDate","2027-01-01T00:00:00Z"); p.put("investment",1000); p.put("expectedReturn",1800); p.put("actualReturn",1500); p.put("costReduction",200); p.put("productivityGain",10); p.put("progressPercent",50); p.put("notes","Primeiro resultado");
        var created=call("POST","/projects",manager,p); assertThat(created.getStatusCode().value()).isEqualTo(201); var pid=created.getBody().get("id");
        assertThat(call("POST","/projects",manager,p).getStatusCode().value()).isEqualTo(409);
        assertThat(call("PUT","/projects/"+pid,leader,p).getStatusCode().value()).isEqualTo(403);
        var dashboard=call("GET","/reports/dashboard?strategyId="+sid,leader,null).getBody();
        assertThat(((Number)dashboard.get("roi")).doubleValue()).isEqualTo(50); assertThat(((Number)dashboard.get("profit")).doubleValue()).isEqualTo(500);
        p.put("status","CONCLUIDO"); p.put("progressPercent",100); p.put("actualEndDate","2026-06-01T00:00:00Z");
        assertThat(call("PUT","/projects/"+pid,manager,p).getStatusCode().value()).isEqualTo(200);
        assertThat(call("DELETE","/projects/"+pid,manager,null).getStatusCode().value()).isEqualTo(204);
        assertThat(call("GET","/projects/"+pid,leader,null).getStatusCode().value()).isEqualTo(404);
    }
    @Test void historySurvivesStrategyDeletion() {
        String id=strategy(); assertThat(call("DELETE","/strategies/"+id,leader,null).getStatusCode().value()).isEqualTo(204);
        assertThat(call("GET","/strategies/"+id,leader,null).getBody()).containsEntry("isActive",false);
        assertThat(call("POST","/ideas",operator,ideaBody(id)).getStatusCode().value()).isEqualTo(409);
        var headers=new HttpHeaders(); headers.setBearerAuth(leader);
        var history=http.exchange("/api/strategies/"+id+"/history",HttpMethod.GET,new HttpEntity<>(headers),List.class);
        assertThat(history.getBody()).hasSize(2);
        assertThat((Map)history.getBody().get(0)).containsKeys("date","category","campaign","title","description","isActive","changedBy");
    }
    @Test void logoutRevokesToken() {
        String token=create("logout","OPERADOR"); assertThat(call("POST","/auth/logout",token,Map.of()).getStatusCode().value()).isEqualTo(204);
        assertThat(call("GET","/auth/me",token,null).getStatusCode().value()).isEqualTo(401);
    }
    @Test void aiFailureIsExplicitAndProtected() {
        assertThat(call("POST","/ai/dashboard/insights",operator,Map.of()).getStatusCode().value()).isEqualTo(403);
        assertThat(call("POST","/ai/dashboard/insights",leader,Map.of()).getStatusCode().value()).isEqualTo(503);
    }
    @AfterAll void cleanup() { if(mongo.getDb().getName().equals(DB) && DB.startsWith("voaaguia_test_")) mongo.getDb().drop(); }
}
