package com.voaaguia.model

import java.time.Instant

enum class ProjectStatus {
    PLANEJAMENTO,
    EM_ANDAMENTO,
    CONCLUIDO,
    CANCELADO
}

enum class ProjectStage {
    IDEACAO,
    VALIDACAO,
    DESENVOLVIMENTO,
    PILOTO,
    IMPLANTACAO,
    OPERACAO
}

data class Project(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val ideaId: String = "",
    val strategyId: String = "",
    val managerId: String = "",
    val managerName: String = "",
    val status: ProjectStatus = ProjectStatus.PLANEJAMENTO,
    val stage: ProjectStage = ProjectStage.IDEACAO,
    val startDate: Instant? = null,
    val expectedEndDate: Instant? = null,
    val actualEndDate: Instant? = null,
    val investment: Double = 0.0,
    val expectedReturn: Double = 0.0,
    val actualReturn: Double = 0.0,
    val productivityGain: Double = 0.0,
    val costReduction: Double = 0.0,
    val progressPercent: Int = 0,
    val notes: String = "",
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
