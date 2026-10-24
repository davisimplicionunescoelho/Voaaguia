package com.voaaguia

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voaaguia.model.*
import com.voaaguia.model.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Runs the actual Android HTTP client against a real backend and MongoDB. */
@RunWith(AndroidJUnit4::class)
class ApiIntegrationTest {
    @Test fun nativeClientCompletesInnovationFlow() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val password = requireNotNull(arguments.getString("demoPassword")) { "Informe demoPassword nas opções do runner" }
        ApiSession.baseUrl = arguments.getString("apiUrl") ?: "http://10.0.2.2:18081/api/"
        val repository = ApiRepository()
        try {
            assertEquals(UserRole.LIDER, repository.login("lider@voaaguia.local", password).getOrThrow().role)
            val strategyId = repository.createGuideline(StrategicGuideline(title = "Validação Android", description = "Integração nativa com backend real", category = "Tecnologia", campaign = "Teste integrado")).getOrThrow()
            assertTrue(repository.getGuidelines().any { it.id == strategyId })
            val history = repository.getHistory(strategyId)
            assertEquals(1, history.size)
            assertEquals("Validação Android", history.single().title)
            assertEquals("Teste integrado", history.single().campaign)
            repository.logout()
            val operator = repository.login("operador@voaaguia.local", password).getOrThrow()
            val ideaId = repository.createIdea(Idea(title = "Otimizar rotas", description = "Planejar entregas por região", category = "Logística", strategyId = strategyId)).getOrThrow()
            val ownIdea = repository.getIdeas().first { it.id == ideaId }
            assertEquals(operator.id, ownIdea.authorId)
            assertNotNull(ownIdea.createdAt)
            assertTrue(runCatching { repository.getDashboardData() }.isFailure)
            repository.logout()
            assertEquals(UserRole.GESTOR, repository.login("gestor@voaaguia.local", password).getOrThrow().role)
            repository.updateIdeaStatus(ideaId, IdeaStatus.APROVADA, IdeaPriority.ALTA, "Aprovada para piloto").getOrThrow()
            val projectId = repository.createProject(Project(title = "Piloto de rotas", description = "Execução em uma região", ideaId = ideaId, strategyId = strategyId, startDate = Instant.now(), expectedEndDate = Instant.now().plusSeconds(86400 * 90), investment = 1000.0, expectedReturn = 1800.0, actualReturn = 1500.0, productivityGain = 10.0)).getOrThrow()
            assertEquals(IdeaStatus.EM_PROJETO, repository.getIdeas().first { it.id == ideaId }.status)
            assertTrue(repository.getProjects().any { it.id == projectId && it.expectedEndDate != null })
            repository.logout()
            repository.login("lider@voaaguia.local", password).getOrThrow()
            val dashboard = repository.getDashboardData(strategyId, projectId)
            assertEquals(50.0, dashboard.roi, 0.001)
            assertEquals(500.0, dashboard.profit, 0.001)
            assertTrue(dashboard.roiAvailable)
            assertEquals(1, dashboard.projects.size)
            assertTrue(repository.getRanking().any { it.id == operator.id && it.points >= 60 })
            repository.logout()
            assertNull(ApiSession.token)
        } finally { ApiSession.clear(); ApiSession.baseUrl = BuildConfig.API_BASE_URL }
    }
}
