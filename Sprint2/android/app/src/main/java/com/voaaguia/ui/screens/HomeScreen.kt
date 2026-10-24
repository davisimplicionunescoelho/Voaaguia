package com.voaaguia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voaaguia.controller.AuthController
import com.voaaguia.controller.DashboardController
import com.voaaguia.model.UserRole
import com.voaaguia.ui.components.*
import com.voaaguia.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authController: AuthController,
    dashboardController: DashboardController,
    onNavigateToGuidelines: () -> Unit,
    onNavigateToIdeas: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by authController.currentUser.collectAsState()
    val dashboardData by dashboardController.dashboardData.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        authController.loadCurrentUser()
        if (currentUser?.role == UserRole.LIDER) dashboardController.loadDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "VoaÁguia",
                            fontWeight = FontWeight.Bold
                        )
                        currentUser?.let {
                            Text(
                                text = "Olá, ${it.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SurfaceWhite.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = SurfaceWhite
                ),
                actions = {
                    // Points badge
                    currentUser?.let { user ->
                        Surface(
                            color = SecondaryGold.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${user.points}", color = SecondaryGold, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = {
                        scope.launch { authController.logout(); onLogout() }
                    }) {
                        Icon(Icons.Default.Logout, "Sair", tint = SurfaceWhite)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Stats for Leaders
            if (currentUser?.role == UserRole.LIDER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "ROI",
                        value = "${String.format("%.1f", dashboardData.roi)}%",
                        icon = Icons.Default.TrendingUp,
                        color = StatusApproved,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Projetos Ativos",
                        value = "${dashboardData.activeProjects}",
                        icon = Icons.Default.Folder,
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Menu Cards
            Text(
                text = "Menu Principal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Orientações Estratégicas - todos os perfis
            MenuCard(
                title = "Orientações Estratégicas",
                subtitle = if (currentUser?.role == UserRole.LIDER) "Gerenciar diretrizes" else "Consultar diretrizes",
                icon = Icons.Default.Flag,
                color = PrimaryBlue,
                onClick = onNavigateToGuidelines
            )

            // Ideias - Operadores e Gestores
            if (currentUser?.role == UserRole.OPERADOR || currentUser?.role == UserRole.GESTOR) {
                MenuCard(
                    title = "Ideias de Inovação",
                    subtitle = if (currentUser?.role == UserRole.OPERADOR) "Cadastrar e acompanhar ideias" else "Avaliar e priorizar ideias",
                    icon = Icons.Default.Lightbulb,
                    color = SecondaryGold,
                    onClick = onNavigateToIdeas
                )
            }

            // Projetos - Gestores e Líderes
            if (currentUser?.role == UserRole.GESTOR || currentUser?.role == UserRole.LIDER) {
                MenuCard(
                    title = "Projetos",
                    subtitle = if (currentUser?.role == UserRole.GESTOR) "Gerenciar projetos" else "Acompanhar andamento",
                    icon = Icons.Default.Assignment,
                    color = StatusProject,
                    onClick = onNavigateToProjects
                )
            }

            // Dashboard - Líderes
            if (currentUser?.role == UserRole.LIDER) {
                MenuCard(
                    title = "Dashboard",
                    subtitle = "Resultados e indicadores",
                    icon = Icons.Default.Dashboard,
                    color = StatusApproved,
                    onClick = onNavigateToDashboard
                )
            }

            // Ranking - todos
            MenuCard(
                title = "Ranking de Inovadores",
                subtitle = "Top colaboradores",
                icon = Icons.Default.EmojiEvents,
                color = GoldBadge,
                onClick = onNavigateToRanking
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = TextSecondary
            )
        }
    }
}
