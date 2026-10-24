package com.voaaguia.service;

import com.voaaguia.api.Requests.*;
import com.voaaguia.audit.AuditService;
import com.voaaguia.domain.User;
import com.voaaguia.repository.UserRepository;
import com.voaaguia.repository.IdeaRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AuthService {
    private final UserRepository users; private final IdeaRepository ideas; private final AuditService audit; private final PasswordEncoder passwords; private final JwtEncoder encoder; private final long minutes;
    public AuthService(UserRepository users, IdeaRepository ideas, AuditService audit, PasswordEncoder passwords, JwtEncoder encoder, @Value("${app.jwt-minutes}") long minutes) {
        this.users=users; this.ideas=ideas; this.audit=audit; this.passwords=passwords; this.encoder=encoder; this.minutes=minutes;
    }
    public Map<String,Object> login(Login input) {
        var user=users.findByEmail(input.email().trim().toLowerCase(Locale.ROOT)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciais inválidas"));
        if(!passwords.matches(input.password(),user.passwordHash)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciais inválidas");
        audit.record("LOGIN", user.id);
        return session(user);
    }
    public Map<String,Object> register(Register input) {
        var user=new User(); user.email=input.email().trim().toLowerCase(Locale.ROOT); user.name=input.name().trim(); user.department=input.department().trim(); user.passwordHash=passwords.encode(input.password());
        user=users.save(user); audit.record("REGISTER", user.id);
        return session(user);
    }
    private Map<String,Object> session(User user) {
        var now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("voaaguia").subject(user.id).issuedAt(now).expiresAt(now.plusSeconds(minutes*60)).id(UUID.randomUUID().toString()).claim("role",user.role).build();
        var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return Map.of("token",token,"expiresAt",claims.getExpiresAt(),"user",enrich(user));
    }
    private User enrich(User u) {
        var own=ideas.findByDeletedFalseOrderByCreatedAtDesc().stream().filter(i -> i.authorId.equals(u.id)).toList();
        u.ideasCount=own.size(); u.approvedIdeasCount=own.stream().filter(i -> i.everApproved).count(); u.points=u.ideasCount*10+u.approvedIdeasCount*50; return u;
    }
    public User user(String id) { return enrich(users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sessão inválida"))); }
}
