package com.voaaguia.model

enum class UserRole {
    OPERADOR,
    GESTOR,
    LIDER
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.OPERADOR,
    val department: String = "",
    val points: Int = 0,
    val ideasCount: Int = 0,
    val approvedIdeasCount: Int = 0
)
