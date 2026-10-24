package com.voaaguia.security;

import com.voaaguia.domain.*;
import com.voaaguia.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates opt-in demonstration accounts and persistent sample records. */
@Component
@Order(20)
@ConditionalOnProperty(name = "app.seed-enabled", havingValue = "true")
public class DevelopmentSeed implements CommandLineRunner {
    private final UserRepository users;
    private final GuidelineRepository strategies;
    private final IdeaRepository ideas;
    private final ProjectRepository projects;
    private final PasswordEncoder passwords;
    private final String password;
    private final boolean sampleData;

    public DevelopmentSeed(
        UserRepository users,
        GuidelineRepository strategies,
        IdeaRepository ideas,
        ProjectRepository projects,
        PasswordEncoder passwords,
        @Value("${app.seed-password}") String password,
        @Value("${app.seed-sample-data:false}") boolean sampleData
    ) {
        this.users = users;
        this.strategies = strategies;
        this.ideas = ideas;
        this.projects = projects;
        this.passwords = passwords;
        this.password = password;
        this.sampleData = sampleData;
    }

    @Override
    public void run(String... args) {
        if (password.length() < 12) throw new IllegalArgumentException("SEED_PASSWORD precisa de pelo menos 12 caracteres");
        for (String role : new String[]{"OPERADOR", "GESTOR", "LIDER"}) ensureUser(role);
        if (sampleData) seedSampleData();
    }

    private User ensureUser(String role) {
        String email = role.toLowerCase(Locale.ROOT) + "@voaaguia.local";
        return users.findByEmail(email).orElseGet(() -> {
            var user = new User();
            user.email = email;
            user.name = switch (role) {
                case "OPERADOR" -> "Marina Operações";
                case "GESTOR" -> "Carlos Gestor";
                default -> "Renata Liderança";
            };
            user.role = role;
            user.department = role.equals("OPERADOR") ? "Operações" : "Inovação";
            user.passwordHash = passwords.encode(password);
            return users.save(user);
        });
    }

    private void seedSampleData() {
        User operator = ensureUser("OPERADOR");
        User manager = ensureUser("GESTOR");
        User leader = ensureUser("LIDER");
        Instant now = Instant.now();

        if (!strategies.existsById("demo-strategy-efficiency")) {
            var strategy = new Guideline();
            strategy.id = "demo-strategy-efficiency";
            strategy.title = "Eficiência Operacional e Segurança";
            strategy.description = "Reduzir paradas, desperdícios e riscos operacionais por meio de processos digitais e decisões orientadas por dados.";
            strategy.category = "Excelência Operacional";
            strategy.campaign = "Inovar para Avançar 2026";
            strategy.authorId = leader.id;
            strategy.authorName = leader.name;
            strategy.createdAt = now.minus(90, ChronoUnit.DAYS);
            strategy.updatedAt = now.minus(30, ChronoUnit.DAYS);
            strategy.history.add(new Revision(strategy.id, strategy.createdAt, "Eficiência", strategy.campaign, "Eficiência Operacional", "Reduzir desperdícios e melhorar a produtividade das operações.", true, leader.id));
            strategy.history.add(new Revision(strategy.id, strategy.updatedAt, strategy.category, strategy.campaign, strategy.title, strategy.description, true, leader.id));
            strategies.save(strategy);
        }

        if (!strategies.existsById("demo-strategy-sustainability")) {
            var strategy = new Guideline();
            strategy.id = "demo-strategy-sustainability";
            strategy.title = "Mobilidade Sustentável";
            strategy.description = "Diminuir emissões e consumo de combustível com tecnologia, capacitação e melhoria contínua.";
            strategy.category = "Sustentabilidade";
            strategy.campaign = "Frota Verde 2026";
            strategy.authorId = leader.id;
            strategy.authorName = leader.name;
            strategy.createdAt = now.minus(60, ChronoUnit.DAYS);
            strategy.updatedAt = strategy.createdAt;
            strategy.history.add(new Revision(strategy.id, strategy.createdAt, strategy.category, strategy.campaign, strategy.title, strategy.description, true, leader.id));
            strategies.save(strategy);
        }

        saveIdeaIfMissing("demo-idea-checklist", "Checklist inteligente de manutenção", "Digitalizar inspeções e emitir alertas para reduzir paradas não planejadas da frota.", "Tecnologia", "demo-strategy-efficiency", operator, "PENDENTE", "MEDIA", "", false, now.minus(12, ChronoUnit.DAYS));
        saveIdeaIfMissing("demo-idea-routes", "Otimização de rotas com dados operacionais", "Combinar histórico de viagens, ocupação e trânsito para apoiar o planejamento diário das rotas.", "Logística", "demo-strategy-efficiency", operator, "APROVADA", "ALTA", "Aprovada para implantação piloto em uma unidade.", true, now.minus(45, ChronoUnit.DAYS));
        saveIdeaIfMissing("demo-idea-eco", "Programa de direção econômica", "Usar telemetria e capacitação para reduzir consumo de combustível e emissões.", "Sustentabilidade", "demo-strategy-sustainability", operator, "APROVADA", "ALTA", "Iniciativa alinhada às metas ambientais.", true, now.minus(75, ChronoUnit.DAYS));

        if (!projects.existsById("demo-project-routes")) {
            var project = baseProject("demo-project-routes", "Piloto de rotas inteligentes", "Implantação do planejamento orientado por dados em uma unidade operacional.", "demo-idea-routes", "demo-strategy-efficiency", manager, now.minus(35, ChronoUnit.DAYS), now.plus(55, ChronoUnit.DAYS));
            project.status = "EM_ANDAMENTO";
            project.stage = "PILOTO";
            project.investment = new BigDecimal("50000");
            project.expectedReturn = new BigDecimal("95000");
            project.actualReturn = new BigDecimal("60000");
            project.costReduction = new BigDecimal("12000");
            project.productivityGain = 15;
            project.progressPercent = 65;
            project.notes = "Piloto ativo, com redução inicial no tempo de planejamento.";
            projects.save(project);
        }

        if (!projects.existsById("demo-project-eco")) {
            var project = baseProject("demo-project-eco", "Direção econômica e telemetria", "Capacitação de condutores e acompanhamento de indicadores de consumo.", "demo-idea-eco", "demo-strategy-sustainability", manager, now.minus(150, ChronoUnit.DAYS), now.minus(20, ChronoUnit.DAYS));
            project.status = "CONCLUIDO";
            project.stage = "OPERACAO";
            project.actualEndDate = now.minus(18, ChronoUnit.DAYS);
            project.investment = new BigDecimal("30000");
            project.expectedReturn = new BigDecimal("50000");
            project.actualReturn = new BigDecimal("55000");
            project.costReduction = new BigDecimal("8000");
            project.productivityGain = 22;
            project.progressPercent = 100;
            project.notes = "Meta superada e prática incorporada à operação.";
            projects.save(project);
        }
    }

    private void saveIdeaIfMissing(String id, String title, String description, String category, String strategyId, User operator, String status, String priority, String feedback, boolean everApproved, Instant createdAt) {
        if (ideas.existsById(id)) return;
        var idea = new Idea();
        idea.id = id;
        idea.title = title;
        idea.description = description;
        idea.category = category;
        idea.strategyId = strategyId;
        idea.authorId = operator.id;
        idea.authorName = operator.name;
        idea.department = operator.department;
        idea.status = status;
        idea.priority = priority;
        idea.feedback = feedback;
        idea.everApproved = everApproved;
        idea.createdAt = createdAt;
        idea.updatedAt = createdAt;
        ideas.save(idea);
    }

    private Project baseProject(String id, String title, String description, String ideaId, String strategyId, User manager, Instant start, Instant expectedEnd) {
        var project = new Project();
        project.id = id;
        project.title = title;
        project.description = description;
        project.ideaId = ideaId;
        project.strategyId = strategyId;
        project.managerId = manager.id;
        project.managerName = manager.name;
        project.startDate = start;
        project.expectedEndDate = expectedEnd;
        project.createdAt = start;
        project.updatedAt = Instant.now();
        return project;
    }
}
