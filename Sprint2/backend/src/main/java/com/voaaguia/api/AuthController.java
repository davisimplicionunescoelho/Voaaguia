package com.voaaguia.api;
import com.voaaguia.service.AuthService;
import com.voaaguia.api.Requests.*;
import com.voaaguia.domain.User;
import jakarta.validation.Valid;
import java.util.Map;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth; private final MongoTemplate mongo;
    public AuthController(AuthService auth, MongoTemplate mongo) { this.auth=auth; this.mongo=mongo; }
    @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Login input) { return auth.login(input); }
    @PostMapping("/register") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public Map<String,Object> register(@Valid @RequestBody Register input) { return auth.register(input); }
    @GetMapping("/me") public User me(@AuthenticationPrincipal Jwt jwt) { return auth.user(jwt.getSubject()); }
    @PostMapping("/logout") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void logout(@AuthenticationPrincipal Jwt jwt) {
        mongo.save(new Document("_id",jwt.getId()).append("expiresAt", java.util.Date.from(jwt.getExpiresAt())),"revoked_tokens");
    }
}
