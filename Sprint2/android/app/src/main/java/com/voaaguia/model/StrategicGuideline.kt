package com.voaaguia.model

import java.time.Instant

data class StrategicGuideline(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val campaign: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val isActive: Boolean = true
)
