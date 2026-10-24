package com.voaaguia.controller

import com.voaaguia.model.Idea
import com.voaaguia.model.IdeaPriority
import com.voaaguia.model.IdeaStatus
import com.voaaguia.model.repository.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IdeaController {
    private val repository = ApiRepository()

    private val _ideas = MutableStateFlow<List<Idea>>(emptyList())
    val ideas: StateFlow<List<Idea>> = _ideas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    suspend fun loadIdeas(filterByAuthor: String? = null) {
        _isLoading.value = true
        try {
            _ideas.value = repository.getIdeas(filterByAuthor)
        } catch (e: Exception) {
            _message.value = "Erro ao carregar ideias: ${e.message}"
        }
        _isLoading.value = false
    }

    suspend fun createIdea(
        title: String,
        description: String,
        category: String,
        authorId: String,
        authorName: String,
        department: String,
        strategyId: String
    ): Boolean {
        _isLoading.value = true
        val idea = Idea(
            title = title,
            description = description,
            category = category,
            strategyId = strategyId,
            authorId = authorId,
            authorName = authorName,
            department = department,
            status = IdeaStatus.PENDENTE,
            priority = IdeaPriority.MEDIA
        )
        val result = repository.createIdea(idea)
        var succeeded = false
        result.onSuccess {
            succeeded = true
            _message.value = "Ideia cadastrada com sucesso! +10 pontos"
            loadIdeas(authorId)
        }.onFailure {
            _message.value = "Erro ao cadastrar ideia: ${it.message}"
        }
        _isLoading.value = false
        return succeeded
    }

    suspend fun approveIdea(ideaId: String, priority: IdeaPriority, feedback: String) {
        _isLoading.value = true
        val result = repository.updateIdeaStatus(ideaId, IdeaStatus.APROVADA, priority, feedback)
        result.onSuccess {
            _message.value = "Ideia aprovada!"
            loadIdeas()
        }.onFailure {
            _message.value = "Erro: ${it.message}"
        }
        _isLoading.value = false
    }

    suspend fun rejectIdea(ideaId: String, feedback: String) {
        _isLoading.value = true
        val result = repository.updateIdeaStatus(ideaId, IdeaStatus.REJEITADA, feedback = feedback)
        result.onSuccess {
            _message.value = "Ideia rejeitada."
            loadIdeas()
        }.onFailure {
            _message.value = "Erro: ${it.message}"
        }
        _isLoading.value = false
    }

    suspend fun setInAnalysis(ideaId: String) {
        val result = repository.updateIdeaStatus(ideaId, IdeaStatus.EM_ANALISE)
        result.onSuccess { loadIdeas() }
    }

    suspend fun voteIdea(ideaId: String) {
        val result = repository.voteIdea(ideaId)
        result.onSuccess {
            _message.value = "Voto registrado!"
            loadIdeas()
        }
    }

    suspend fun updateIdea(idea: Idea): Boolean {
        val result = repository.updateIdea(idea)
        result.onSuccess { loadIdeas(); _message.value = "Ideia atualizada" }.onFailure { _message.value = "Erro ao atualizar ideia: ${it.message}" }
        return result.isSuccess
    }
    suspend fun deleteIdea(id: String) {
        repository.deleteIdea(id).onSuccess { loadIdeas(); _message.value = "Ideia excluída" }.onFailure { _message.value = it.message }
    }
    fun clearMessage() {
        _message.value = null
    }
}
