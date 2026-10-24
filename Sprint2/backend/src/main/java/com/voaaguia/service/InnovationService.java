package com.voaaguia.service;

import com.voaaguia.domain.*;
import com.voaaguia.repository.*;
import com.voaaguia.api.Requests.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class InnovationService {
    private final GuidelineRepository strategies; private final IdeaRepository ideas; private final ProjectRepository projects;
    public InnovationService(GuidelineRepository strategies, IdeaRepository ideas, ProjectRepository projects) { this.strategies=strategies; this.ideas=ideas; this.projects=projects; }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro não encontrado"); }
    private void require(boolean condition, String message) { if(!condition) throw new ResponseStatusException(HttpStatus.CONFLICT,message); }
    public List<Guideline> strategies() { return strategies.findByIsActiveTrueOrderByCreatedAtDesc(); }
    public Guideline strategy(String id) { return strategies.findById(id).orElseThrow(this::missing); }
    private void activeStrategy(String id) { require(strategy(id).isActive,"Selecione uma estratégia vigente"); }
    public Guideline saveStrategy(String id, Strategy input, User actor) {
        var value=id==null?new Guideline():strategy(id);
        value.title=input.title().trim(); value.description=input.description().trim(); value.category=input.category().trim(); value.campaign=input.campaign().trim(); value.isActive=input.isActive();
        if(id==null) { value.id=UUID.randomUUID().toString(); value.authorId=actor.id; value.authorName=actor.name; }
        value.updatedAt=Instant.now();
        value.history.add(new Revision(value.id,value.updatedAt,value.category,value.campaign,value.title,value.description,value.isActive,actor.id));
        return strategies.save(value);
    }
    public void deleteStrategy(String id, User actor) { var g=strategy(id); saveStrategy(id,new Strategy(g.title,g.description,g.category,g.campaign,false),actor); }
    public List<Revision> history(String id) { return List.copyOf(strategy(id).history); }
    private Idea withProjectStatus(Idea idea) { if(projects.existsByIdeaId(idea.id)) idea.status="EM_PROJETO"; return idea; }
    public List<Idea> ideas(User actor) { return ideas.findByDeletedFalseOrderByCreatedAtDesc().stream().filter(i -> !actor.role.equals("OPERADOR") || i.authorId.equals(actor.id)).map(this::withProjectStatus).toList(); }
    public Idea idea(String id, User actor) {
        var i=ideas.findById(id).filter(v -> !v.deleted).orElseThrow(this::missing);
        if(actor.role.equals("OPERADOR") && !actor.id.equals(i.authorId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ideia não encontrada");
        return withProjectStatus(i);
    }
    public Idea saveIdea(String id, IdeaInput input, User actor) {
        activeStrategy(input.strategyId());
        var value=id==null?new Idea():idea(id,actor);
        if(id!=null) require(value.status.equals("PENDENTE"),"Somente ideias pendentes podem ser editadas");
        value.title=input.title().trim(); value.description=input.description().trim(); value.category=input.category().trim(); value.strategyId=input.strategyId();
        if(id==null) { value.authorId=actor.id; value.authorName=actor.name; value.department=actor.department; }
        value.updatedAt=Instant.now(); return ideas.save(value);
    }
    public void deleteIdea(String id, User actor) { var i=idea(id,actor); require(i.status.equals("PENDENTE"),"Somente ideias pendentes podem ser excluídas"); i.deleted=true; ideas.save(i); }
    public Idea review(String id, Review input, User actor) {
        var i=idea(id,actor); require(!i.status.equals("EM_PROJETO"),"Ideia já vinculada a um projeto");
        i.status=input.status(); i.priority=input.priority(); i.feedback=input.feedback(); i.everApproved|=input.status().equals("APROVADA"); i.updatedAt=Instant.now(); return ideas.save(i);
    }
    public Idea vote(String id, User actor) { var i=idea(id,actor); i.voters.add(actor.id); return ideas.save(i); }
    public List<Project> projects() { return projects.findByDeletedFalseOrderByCreatedAtDesc(); }
    public Project project(String id) { return projects.findById(id).filter(p -> !p.deleted).orElseThrow(this::missing); }
    public Project saveProject(String id, ProjectInput input, User actor) {
        require(!input.expectedEndDate().isBefore(input.startDate()),"Prazo final deve ser posterior ao início");
        require(input.actualEndDate()==null || !input.actualEndDate().isBefore(input.startDate()),"Data de conclusão inválida");
        require(!input.status().equals("CONCLUIDO") || (input.progressPercent()==100 && input.actualEndDate()!=null),"Projeto concluído exige 100% de progresso e data de conclusão");
        var p=id==null?new Project():project(id);
        if(id==null) {
            activeStrategy(input.strategyId()); var idea=idea(input.ideaId(),actor);
            require(idea.status.equals("APROVADA"),"Projeto exige uma ideia aprovada");
            require(idea.strategyId.equals(input.strategyId()),"Projeto deve seguir a estratégia da ideia");
            p.id=UUID.randomUUID().toString(); p.ideaId=idea.id; p.strategyId=idea.strategyId; p.managerId=actor.id; p.managerName=actor.name;
        } else {
            require(p.ideaId.equals(input.ideaId()) && p.strategyId.equals(input.strategyId()),"Vínculos originais do projeto não podem ser alterados");
        }
        p.title=input.title().trim(); p.description=input.description().trim(); p.status=input.status(); p.stage=input.stage(); p.startDate=input.startDate(); p.expectedEndDate=input.expectedEndDate(); p.actualEndDate=input.actualEndDate();
        p.investment=input.investment(); p.expectedReturn=input.expectedReturn(); p.actualReturn=input.actualReturn(); p.costReduction=input.costReduction(); p.productivityGain=input.productivityGain(); p.progressPercent=input.progressPercent(); p.notes=input.notes(); p.updatedAt=Instant.now();
        // Unique project-per-idea index prevents duplicate initiatives under concurrent requests.
        // Status EM_PROJETO is derived from this durable relationship on reads.
        // No second document write: an interrupted request cannot leave a half-created linkage.
        return projects.save(p);
    }
    public void deleteProject(String id) { var p=project(id); p.deleted=true; p.updatedAt=Instant.now(); projects.save(p); }
}
