package com.voaaguia.model

import java.time.Instant

enum class IdeaStatus {
    PENDENTE,
    EM_ANALISE,
    APROVADA,
    REJEITADA,
    EM_PROJETO
}

enum class IdeaPriority {
    BAIXA,
    MEDIA,
    ALTA,
    CRITICA
}

data class Idea(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val strategyId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val department: String = "",
    val status: IdeaStatus = IdeaStatus.PENDENTE,
    val priority: IdeaPriority = IdeaPriority.MEDIA,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val votes: Int = 0,
    val feedback: String = ""
)
