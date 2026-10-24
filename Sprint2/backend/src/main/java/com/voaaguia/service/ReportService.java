package com.voaaguia.service;
import com.voaaguia.domain.*;
import com.voaaguia.repository.*;
import org.springframework.stereotype.Service;
import java.math.*;
import java.util.*;
import java.util.function.Function;

@Service
public class ReportService {
    private final ProjectRepository projects; private final IdeaRepository ideas; private final UserRepository users;
    public ReportService(ProjectRepository projects,IdeaRepository ideas,UserRepository users) { this.projects=projects; this.ideas=ideas; this.users=users; }
    private BigDecimal sum(List<Project> list,Function<Project,BigDecimal> field) { return list.stream().map(field).reduce(BigDecimal.ZERO,BigDecimal::add); }
    public Map<String,Object> dashboard(String strategyId,String projectId) {
        var selected=projects.findByDeletedFalseOrderByCreatedAtDesc().stream().filter(p -> strategyId==null || p.strategyId.equals(strategyId)).filter(p -> projectId==null || p.id.equals(projectId)).toList();
        var selectedIdeas=ideas.findByDeletedFalseOrderByCreatedAtDesc().stream().filter(i -> strategyId==null || i.strategyId.equals(strategyId)).filter(i -> projectId==null || selected.stream().anyMatch(p -> p.ideaId.equals(i.id))).toList();
        var investment=sum(selected,p -> p.investment); var returns=sum(selected,p -> p.actualReturn); var profit=returns.subtract(investment);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("totalInvestment",investment); result.put("totalReturn",returns); result.put("profit",profit);
        result.put("roi",investment.signum()==0?null:profit.multiply(BigDecimal.valueOf(100)).divide(investment,2,RoundingMode.HALF_UP));
        result.put("roiAvailable",investment.signum()>0);
        result.put("totalCostReduction",sum(selected,p -> p.costReduction));
        result.put("avgProductivityGain",selected.stream().mapToDouble(p -> p.productivityGain).average().orElse(0));
        result.put("activeProjects",selected.stream().filter(p -> p.status.equals("EM_ANDAMENTO")).count());
        result.put("completedProjects",selected.stream().filter(p -> p.status.equals("CONCLUIDO")).count());
        result.put("overdueProjects",selected.stream().filter(p -> !Set.of("CONCLUIDO","CANCELADO").contains(p.status) && p.expectedEndDate!=null && p.expectedEndDate.isBefore(java.time.Instant.now())).count());
        result.put("totalIdeas",selectedIdeas.size()); result.put("approvedIdeas",selectedIdeas.stream().filter(i -> Set.of("APROVADA","EM_PROJETO").contains(i.status)).count());
        result.put("projects",selected); return result;
    }
    public List<Map<String,Object>> ranking() {
        var all=ideas.findByDeletedFalseOrderByCreatedAtDesc();
        return users.findAll().stream().filter(u -> u.role.equals("OPERADOR")).map(u -> {
            long count=all.stream().filter(i -> i.authorId.equals(u.id)).count(); long approved=all.stream().filter(i -> i.authorId.equals(u.id)&&i.everApproved).count();
            Map<String,Object> row=new LinkedHashMap<>(); row.put("id",u.id); row.put("name",u.name); row.put("department",u.department); row.put("points",count*10+approved*50); row.put("ideasCount",count); row.put("approvedIdeasCount",approved); return row;
        }).sorted(Comparator.<Map<String,Object>>comparingLong(m -> ((Number)m.get("points")).longValue()).reversed()).limit(10).toList();
    }
}
