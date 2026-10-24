package com.voaaguia.api;
import com.voaaguia.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Map;

@RestController @RequestMapping("/api/ai")
public class AiController {
    private final AiService ai; private final InnovationService innovation; private final AuthService auth; private final ReportService reports;
    public AiController(AiService ai,InnovationService innovation,AuthService auth,ReportService reports) { this.ai=ai; this.innovation=innovation; this.auth=auth; this.reports=reports; }
    @PostMapping("/ideas/{id}/analysis") @PreAuthorize("hasRole('GESTOR')")
    public Map<String,String> analyze(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) {
        var idea=innovation.idea(id,auth.user(jwt.getSubject()));
        return ai.generate("Pontue de 0 a 100 em impacto, viabilidade e alinhamento estratégico. Apresente média, prioridade sugerida, justificativa, riscos e melhoria concreta. Não aprove automaticamente.",Map.of("idea",idea,"strategy",innovation.strategy(idea.strategyId)));
    }
    @PostMapping("/ideas/{id}/project-suggestion") @PreAuthorize("hasRole('GESTOR')")
    public Map<String,String> suggest(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) { return ai.generate("Sugira objetivo, etapas, prazo e KPIs para um projeto. Identifique estimativas como hipóteses.",innovation.idea(id,auth.user(jwt.getSubject()))); }
    @PostMapping("/ideas/{id}/feedback") @PreAuthorize("hasRole('GESTOR')")
    public Map<String,String> feedback(@PathVariable String id,@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="true") boolean approving) { return ai.generate("Sugira feedback construtivo para a decisão de "+(approving?"aprovação":"rejeição"),innovation.idea(id,auth.user(jwt.getSubject()))); }
    @PostMapping("/dashboard/insights") @PreAuthorize("hasRole('LIDER')")
    public Map<String,String> insights(@RequestParam(required=false) String strategyId,@RequestParam(required=false) String projectId) { return ai.generate("Analise os resultados. Destaque ROI, riscos de prazo, qualidade dos dados e três ações recomendadas. ROI nulo significa investimento zero, e não rentabilidade zero.",reports.dashboard(strategyId,projectId)); }
}
