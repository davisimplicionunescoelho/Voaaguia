package com.voaaguia.controller

import com.voaaguia.model.StrategicGuideline
import com.voaaguia.model.StrategyRevision
import com.voaaguia.model.repository.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GuidelineController {
    private val repository = ApiRepository()

    private val _guidelines = MutableStateFlow<List<StrategicGuideline>>(emptyList())
    val guidelines: StateFlow<List<StrategicGuideline>> = _guidelines

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _history = MutableStateFlow<List<StrategyRevision>?>(null)
    val history: StateFlow<List<StrategyRevision>?> = _history

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading

    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError

    suspend fun loadGuidelines() {
        _isLoading.value = true
        try {
            _guidelines.value = repository.getGuidelines()
        } catch (e: Exception) {
            _message.value = "Erro ao carregar orientações: ${e.message}"
        }
        _isLoading.value = false
    }

    suspend fun createGuideline(title: String, description: String, category: String, authorId: String, authorName: String, campaign: String) {
        _isLoading.value = true
        val guideline = StrategicGuideline(
            title = title,
            description = description,
            category = category,
            campaign = campaign,
            authorId = authorId,
            authorName = authorName
        )
        val result = repository.createGuideline(guideline)
        result.onSuccess {
            _message.value = "Orientação criada com sucesso!"
            loadGuidelines()
        }.onFailure {
            _message.value = "Erro ao criar orientação: ${it.message}"
        }
        _isLoading.value = false
    }

    suspend fun updateGuideline(guideline: StrategicGuideline) {
        _isLoading.value = true
        val result = repository.updateGuideline(guideline)
        result.onSuccess {
            _message.value = "Orientação atualizada!"
            loadGuidelines()
        }.onFailure {
            _message.value = "Erro ao atualizar: ${it.message}"
        }
        _isLoading.value = false
    }

    suspend fun deleteGuideline(id: String) {
        _isLoading.value = true
        val result = repository.deleteGuideline(id)
        result.onSuccess {
            _message.value = "Orientação removida!"
            loadGuidelines()
        }.onFailure {
            _message.value = "Erro ao remover: ${it.message}"
        }
        _isLoading.value = false
    }

    suspend fun loadHistory(id: String) {
        _history.value = null
        _historyError.value = null
        _isHistoryLoading.value = true
        try {
            _history.value = repository.getHistory(id)
        } catch (e: Exception) {
            _historyError.value = e.message ?: "Não foi possível carregar o histórico"
        } finally {
            _isHistoryLoading.value = false
        }
    }

    fun clearHistory() {
        _history.value = null
        _historyError.value = null
        _isHistoryLoading.value = false
    }

    fun clearMessage() {
        _message.value = null
    }
}
