package com.voaaguia.controller

import com.voaaguia.model.Project
import com.voaaguia.model.ProjectStage
import com.voaaguia.model.ProjectStatus
import com.voaaguia.model.repository.ApiRepository
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProjectController {
    private val repository = ApiRepository()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    suspend fun loadProjects() {
        _isLoading.value = true
        try {
            _projects.value = repository.getProjects()
        } catch (e: Exception) {
            _message.value = "Erro ao carregar projetos: ${e.message}"
        }
        _isLoading.value = false
    }

    suspend fun createProject(
        title: String,
        description: String,
        ideaId: String,
        managerId: String,
        managerName: String,
        investment: Double,
        expectedReturn: Double,
        strategyId: String,
        expectedEndDate: Instant
    ): Boolean {
        _isLoading.value = true
        val project = Project(
            title = title,
            description = description,
            ideaId = ideaId,
            strategyId = strategyId,
            expectedEndDate = expectedEndDate,
            managerId = managerId,
            managerName = managerName,
            status = ProjectStatus.PLANEJAMENTO,
            stage = ProjectStage.IDEACAO,
            startDate = Instant.now(),
            investment = investment,
            expectedReturn = expectedReturn
        )
        val result = repository.createProject(project)
        var succeeded = false
        result.onSuccess {
            succeeded = true
            _message.value = "Projeto criado com sucesso!"
            loadProjects()
        }.onFailure {
            _message.value = "Erro ao criar projeto: ${it.message}"
        }
        _isLoading.value = false
        return succeeded
    }

    suspend fun updateProject(project: Project): Boolean {
        _isLoading.value = true
        val result = repository.updateProject(project)
        var succeeded = false
        result.onSuccess {
            succeeded = true
            _message.value = "Projeto atualizado!"
            loadProjects()
        }.onFailure {
            _message.value = "Erro ao atualizar: ${it.message}"
        }
        _isLoading.value = false
        return succeeded
    }

    suspend fun updateProgress(projectId: String, progress: Int, stage: ProjectStage, status: ProjectStatus, notes: String): Boolean {
        val project = _projects.value.find { it.id == projectId } ?: return false
        val updated = project.copy(
            progressPercent = if (status == ProjectStatus.CONCLUIDO) 100 else progress,
            actualEndDate = if (status == ProjectStatus.CONCLUIDO) Instant.now() else null,
            stage = stage,
            status = status,
            notes = notes,
            updatedAt = Instant.now()
        )
        return updateProject(updated)
    }

    suspend fun addResults(projectId: String, actualReturn: Double, productivityGain: Double, costReduction: Double): Boolean {
        val project = _projects.value.find { it.id == projectId } ?: return false
        val updated = project.copy(
            actualReturn = actualReturn,
            productivityGain = productivityGain,
            costReduction = costReduction,
            updatedAt = Instant.now()
        )
        return updateProject(updated)
    }

    suspend fun deleteProject(id: String) {
        repository.deleteProject(id).onSuccess { loadProjects(); _message.value = "Projeto excluído" }.onFailure { _message.value = it.message }
    }
    fun clearMessage() {
        _message.value = null
    }
}
