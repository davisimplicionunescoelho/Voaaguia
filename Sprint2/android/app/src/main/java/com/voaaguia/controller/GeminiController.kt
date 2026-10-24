package com.voaaguia.controller

import com.voaaguia.model.Idea
import com.voaaguia.model.repository.ApiRepository
import com.voaaguia.model.repository.DashboardData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GeminiController {
    private val repository = ApiRepository()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _lastResponse = MutableStateFlow<String?>(null)
    val lastResponse: StateFlow<String?> = _lastResponse
    private suspend fun call(path: String): String {
        _isLoading.value = true
        return try { repository.ai(path).also { _lastResponse.value = it } }
        catch (e: Exception) { (e.message ?: "Não foi possível consultar a IA").also { _lastResponse.value = it } }
        finally { _isLoading.value = false }
    }
    suspend fun analyzeIdea(idea: Idea): String = call("ideas/${idea.id}/analysis")
    suspend fun generateDashboardInsights(data: DashboardData, strategyId: String? = null, projectId: String? = null): String {
        val params = listOfNotNull(strategyId?.let { "strategyId=$it" }, projectId?.let { "projectId=$it" }).joinToString("&")
        return call("dashboard/insights" + if (params.isEmpty()) "" else "?$params")
    }
    suspend fun suggestProjectStructure(idea: Idea): String = call("ideas/${idea.id}/project-suggestion")
    suspend fun generateFeedback(idea: Idea, isApproving: Boolean): String = call("ideas/${idea.id}/feedback?approving=$isApproving")
    fun clearResponse() { _lastResponse.value = null }
}
