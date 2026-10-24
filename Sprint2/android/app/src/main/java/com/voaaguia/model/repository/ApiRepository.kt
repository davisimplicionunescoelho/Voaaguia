package com.voaaguia.model.repository

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import com.voaaguia.BuildConfig
import com.voaaguia.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.time.Instant
import java.util.concurrent.TimeUnit

/** JWT kept only in memory: process death requires a fresh login; no plaintext token on disk. */
object ApiSession {
    @Volatile var baseUrl: String = BuildConfig.API_BASE_URL
    @Volatile var token: String? = null
    @Volatile var user: User? = null
    val expired = MutableStateFlow(false)
    fun clear() { token = null; user = null }
}

class ApiRepository {
    companion object {
        val gson = GsonBuilder()
            .registerTypeAdapter(Instant::class.java, JsonSerializer<Instant> { value, _, _ -> JsonPrimitive(value.toString()) })
            .registerTypeAdapter(Instant::class.java, JsonDeserializer { json, _, _ -> Instant.parse(json.asString) })
            .create()
        private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    }
    suspend fun request(method: String, path: String, body: Any? = null): String = withContext(Dispatchers.IO) {
        val payload = if (method in listOf("POST", "PUT", "PATCH")) gson.toJson(body ?: emptyMap<String, String>()).toRequestBody("application/json".toMediaType()) else null
        val builder = Request.Builder().url(ApiSession.baseUrl + path).method(method, payload)
        ApiSession.token?.let { builder.header("Authorization", "Bearer $it") }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 401 && !path.startsWith("auth/login")) { ApiSession.clear(); ApiSession.expired.value = true }
                val error = runCatching { gson.fromJson(text, com.google.gson.JsonObject::class.java).get("message")?.asString }.getOrNull()
                throw IllegalStateException(error ?: when (response.code) { 401 -> "Sessão expirada ou credenciais inválidas"; 403 -> "Seu perfil não tem acesso a esta operação"; else -> "Erro do servidor (${response.code})" })
            }
            text
        }
    }
    private suspend fun <T> result(block: suspend () -> T): Result<T> = try { Result.success(block()) } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }
    private data class LoginResponse(val token: String, val user: User)
    private suspend fun authenticate(path: String, body: Any): User {
        val response = gson.fromJson(request("POST", path, body), LoginResponse::class.java)
        ApiSession.token = response.token; ApiSession.user = response.user; ApiSession.expired.value = false
        return response.user
    }
    suspend fun login(email: String, password: String) = result { authenticate("auth/login", mapOf("email" to email, "password" to password)) }
    suspend fun register(email: String, password: String, name: String, role: UserRole, department: String) = result {
        authenticate("auth/register", mapOf("email" to email, "password" to password, "name" to name, "department" to department))
    }
    suspend fun logout() { try { request("POST", "auth/logout") } finally { ApiSession.clear() } }
    fun getCurrentUserId(): String? = ApiSession.user?.id
    suspend fun getUser(uid: String): User = gson.fromJson(request("GET", "auth/me"), User::class.java).also { ApiSession.user = it }
    suspend fun getGuidelines(): List<StrategicGuideline> = gson.fromJson(request("GET", "strategies"), object : TypeToken<List<StrategicGuideline>>() {}.type)
    suspend fun getHistory(id: String): List<StrategyRevision> = gson.fromJson(
        request("GET", "strategies/$id/history"),
        object : TypeToken<List<StrategyRevision>>() {}.type
    )
    suspend fun createGuideline(g: StrategicGuideline) = result { gson.fromJson(request("POST", "strategies", g), StrategicGuideline::class.java).id }
    suspend fun updateGuideline(g: StrategicGuideline) = result { request("PUT", "strategies/${g.id}", g); Unit }
    suspend fun deleteGuideline(id: String) = result { request("DELETE", "strategies/$id"); Unit }
    suspend fun getIdeas(filterByAuthor: String? = null): List<Idea> = gson.fromJson(request("GET", "ideas"), object : TypeToken<List<Idea>>() {}.type)
    suspend fun createIdea(idea: Idea) = result { gson.fromJson(request("POST", "ideas", idea), Idea::class.java).id }
    suspend fun updateIdea(idea: Idea) = result { request("PUT", "ideas/${idea.id}", idea); Unit }
    suspend fun deleteIdea(id: String) = result { request("DELETE", "ideas/$id"); Unit }
    suspend fun updateIdeaStatus(ideaId: String, status: IdeaStatus, priority: IdeaPriority? = null, feedback: String? = null) = result {
        request("PATCH", "ideas/$ideaId/review", mapOf("status" to status.name, "priority" to (priority ?: IdeaPriority.MEDIA).name, "feedback" to (feedback ?: ""))); Unit
    }
    suspend fun voteIdea(ideaId: String) = result { request("POST", "ideas/$ideaId/vote"); Unit }
    suspend fun getProjects(): List<Project> = gson.fromJson(request("GET", "projects"), object : TypeToken<List<Project>>() {}.type)
    suspend fun createProject(project: Project) = result { gson.fromJson(request("POST", "projects", project), Project::class.java).id }
    suspend fun updateProject(project: Project) = result { request("PUT", "projects/${project.id}", project); Unit }
    suspend fun deleteProject(id: String) = result { request("DELETE", "projects/$id"); Unit }
    suspend fun getDashboardData(strategyId: String? = null, projectId: String? = null): DashboardData {
        val params = listOfNotNull(strategyId?.let { "strategyId=$it" }, projectId?.let { "projectId=$it" }).joinToString("&")
        return gson.fromJson(request("GET", "reports/dashboard" + if (params.isEmpty()) "" else "?$params"), DashboardData::class.java)
    }
    suspend fun getRanking(): List<User> = gson.fromJson(request("GET", "ranking"), object : TypeToken<List<User>>() {}.type)
    suspend fun ai(path: String): String = gson.fromJson(request("POST", "ai/$path"), com.google.gson.JsonObject::class.java).get("text").asString
}

data class DashboardData(
    val totalInvestment: Double = 0.0,
    val totalReturn: Double = 0.0,
    val profit: Double = 0.0,
    val roi: Double = 0.0,
    val roiAvailable: Boolean = false,
    val avgProductivityGain: Double = 0.0,
    val totalCostReduction: Double = 0.0,
    val completedProjects: Int = 0,
    val activeProjects: Int = 0,
    val overdueProjects: Int = 0,
    val totalIdeas: Int = 0,
    val approvedIdeas: Int = 0,
    val projects: List<Project> = emptyList()
)
