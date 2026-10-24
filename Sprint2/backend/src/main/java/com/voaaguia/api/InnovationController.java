package com.voaaguia.api;

import com.voaaguia.api.Requests.*;
import com.voaaguia.domain.*;
import com.voaaguia.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class InnovationController {
    private final InnovationService service; private final AuthService auth;
    public InnovationController(InnovationService service, AuthService auth) { this.service=service; this.auth=auth; }
    private User user(Jwt jwt) { return auth.user(jwt.getSubject()); }
    @GetMapping("/strategies") public List<Guideline> strategies() { return service.strategies(); }
    @GetMapping("/strategies/{id}") public Guideline strategy(@PathVariable String id) { return service.strategy(id); }
    @GetMapping("/strategies/{id}/history") public List<Revision> history(@PathVariable String id) { return service.history(id); }
    @PostMapping("/strategies") @PreAuthorize("hasRole('LIDER')") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Guideline createStrategy(@Valid @RequestBody Strategy input,@AuthenticationPrincipal Jwt jwt) { return service.saveStrategy(null,input,user(jwt)); }
    @PutMapping("/strategies/{id}") @PreAuthorize("hasRole('LIDER')")
    public Guideline updateStrategy(@PathVariable String id,@Valid @RequestBody Strategy input,@AuthenticationPrincipal Jwt jwt) { return service.saveStrategy(id,input,user(jwt)); }
    @DeleteMapping("/strategies/{id}") @PreAuthorize("hasRole('LIDER')") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteStrategy(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) { service.deleteStrategy(id,user(jwt)); }
    @GetMapping("/ideas") public List<Idea> ideas(@AuthenticationPrincipal Jwt jwt) { return service.ideas(user(jwt)); }
    @GetMapping("/ideas/{id}") public Idea idea(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) { return service.idea(id,user(jwt)); }
    @PostMapping("/ideas") @PreAuthorize("hasRole('OPERADOR')") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Idea createIdea(@Valid @RequestBody IdeaInput input,@AuthenticationPrincipal Jwt jwt) { return service.saveIdea(null,input,user(jwt)); }
    @PutMapping("/ideas/{id}") @PreAuthorize("hasRole('OPERADOR')")
    public Idea updateIdea(@PathVariable String id,@Valid @RequestBody IdeaInput input,@AuthenticationPrincipal Jwt jwt) { return service.saveIdea(id,input,user(jwt)); }
    @DeleteMapping("/ideas/{id}") @PreAuthorize("hasRole('OPERADOR')") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteIdea(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) { service.deleteIdea(id,user(jwt)); }
    @PatchMapping("/ideas/{id}/review") @PreAuthorize("hasRole('GESTOR')")
    public Idea review(@PathVariable String id,@Valid @RequestBody Review input,@AuthenticationPrincipal Jwt jwt) { return service.review(id,input,user(jwt)); }
    @PostMapping("/ideas/{id}/vote") public Idea vote(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) { return service.vote(id,user(jwt)); }
    @GetMapping("/projects") @PreAuthorize("hasAnyRole('GESTOR','LIDER')") public List<Project> projects() { return service.projects(); }
    @GetMapping("/projects/{id}") @PreAuthorize("hasAnyRole('GESTOR','LIDER')") public Project project(@PathVariable String id) { return service.project(id); }
    @PostMapping("/projects") @PreAuthorize("hasRole('GESTOR')") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Project createProject(@Valid @RequestBody ProjectInput input,@AuthenticationPrincipal Jwt jwt) { return service.saveProject(null,input,user(jwt)); }
    @PutMapping("/projects/{id}") @PreAuthorize("hasRole('GESTOR')")
    public Project updateProject(@PathVariable String id,@Valid @RequestBody ProjectInput input,@AuthenticationPrincipal Jwt jwt) { return service.saveProject(id,input,user(jwt)); }
    @DeleteMapping("/projects/{id}") @PreAuthorize("hasRole('GESTOR')") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable String id) { service.deleteProject(id); }
}
