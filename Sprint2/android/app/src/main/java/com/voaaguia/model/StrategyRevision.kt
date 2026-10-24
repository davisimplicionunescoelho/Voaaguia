package com.voaaguia.model

import java.time.Instant

data class StrategyRevision(
    val id: String = "",
    val date: Instant? = null,
    val category: String = "",
    val campaign: String = "",
    val title: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val changedBy: String = ""
)
