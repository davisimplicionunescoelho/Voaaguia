package com.voaaguia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voaaguia.controller.DashboardController
import com.voaaguia.controller.GeminiController
import com.voaaguia.model.ProjectStatus
import com.voaaguia.ui.components.*
import com.voaaguia.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardController: DashboardController,
    geminiController: GeminiController = GeminiController(),
    onBack: () -> Unit
) {
    val dashboardData by dashboardController.dashboardData.collectAsState()
    val ranking by dashboardController.ranking.collectAsState()
    val isLoading by dashboardController.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val error by dashboardController.error.collectAsState()
    var strategies by remember { mutableStateOf<List<com.voaaguia.model.StrategicGuideline>>(emptyList()) }
    var allProjects by remember { mutableStateOf<List<com.voaaguia.model.Project>>(emptyList()) }
    var strategyId by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }
    var projectExpanded by remember { mutableStateOf(false) }

    var aiInsights by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dashboardController.loadDashboard()
        try {
            val repository = com.voaaguia.model.repository.ApiRepository()
            strategies = repository.getGuidelines()
            allProjects = repository.getProjects()
        } catch (_: Exception) { }
        dashboardController.loadRanking()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = SurfaceWhite,
                    navigationIconContentColor = SurfaceWhite
                ),
                actions = {
                    IconButton(onClick = { scope.launch { dashboardController.loadDashboard(strategyId, projectId) } }) {
                        Icon(Icons.Default.Refresh, "Atualizar", tint = SurfaceWhite)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            LoadingOverlay(true)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    StrategySelector(strategies, strategyId ?: "") {
                        strategyId = it; projectId = null
                        scope.launch { dashboardController.loadDashboard(strategyId, null) }
                    }
                    ExposedDropdownMenuBox(projectExpanded, { projectExpanded = it }) {
                        OutlinedTextField(value = allProjects.find { it.id == projectId }?.title ?: "Todos os projetos", onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("Projeto") })
                        ExposedDropdownMenu(projectExpanded, { projectExpanded = false }) {
                            allProjects.filter { strategyId == null || it.strategyId == strategyId }.forEach { p ->
                                DropdownMenuItem(text = { Text(p.title) }, onClick = {
                                    projectId = p.id; projectExpanded = false
                                    scope.launch { dashboardController.loadDashboard(strategyId, projectId) }
                                })
                            }
                        }
                    }
                    TextButton(onClick = { strategyId = null; projectId = null; scope.launch { dashboardController.loadDashboard() } }) { Text("Limpar filtros") }
                }
                item {
                    Text("Investimento e retorno realizado", style = MaterialTheme.typography.titleMedium)
                    val max = maxOf(dashboardData.totalInvestment, dashboardData.totalReturn, 1.0).toFloat()
                    Text("Investimento: R$ ${formatCurrency(dashboardData.totalInvestment)}")
                    LinearProgressIndicator(progress = (dashboardData.totalInvestment.toFloat() / max).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
                    Spacer(Modifier.height(8.dp))
                    Text("Retorno: R$ ${formatCurrency(dashboardData.totalReturn)}")
                    LinearProgressIndicator(progress = (dashboardData.totalReturn.toFloat() / max).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth(), color = StatusApproved)
                    Text("Lucro: R$ ${formatCurrency(dashboardData.profit)} • Projetos atrasados: ${dashboardData.overdueProjects}")
                }
                // Summary KPIs
                item {
                    Text(
                        text = "Resumo Geral",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "ROI Geral",
                            value = if (dashboardData.roiAvailable) "${String.format("%.1f", dashboardData.roi)}%" else "N/A",
                            icon = Icons.Default.TrendingUp,
                            color = if (dashboardData.roi > 0) StatusApproved else StatusRejected,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Investimento",
                            value = "R$ ${formatCurrency(dashboardData.totalInvestment)}",
                            icon = Icons.Default.AccountBalance,
                            color = PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Retorno Total",
                            value = "R$ ${formatCurrency(dashboardData.totalReturn)}",
                            icon = Icons.Default.Payments,
                            color = StatusApproved,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Red. Custos",
                            value = "R$ ${formatCurrency(dashboardData.totalCostReduction)}",
                            icon = Icons.Default.Savings,
                            color = SecondaryGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Produtividade",
                            value = "${String.format("%.1f", dashboardData.avgProductivityGain)}%",
                            icon = Icons.Default.Speed,
                            color = StatusAnalysis,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Ideias",
                            value = "${dashboardData.approvedIdeas}/${dashboardData.totalIdeas}",
                            icon = Icons.Default.Lightbulb,
                            color = SecondaryGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Projects summary
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader("Projetos por Status")
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ProjectStatusRow("Em Andamento", dashboardData.activeProjects, StatusAnalysis)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            ProjectStatusRow("Concluídos", dashboardData.completedProjects, StatusApproved)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            val planning = dashboardData.projects.count { it.status == ProjectStatus.PLANEJAMENTO }
                            ProjectStatusRow("Planejamento", planning, StatusPending)
                        }
                    }
                }

                // Individual project results
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader("Resultados por Projeto")
                }

                items(dashboardData.projects.filter { it.actualReturn > 0 || it.costReduction > 0 }) { project ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                ProjectStatusChip(project.status)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ResultItem("Investido", "R$ ${formatCurrency(project.investment)}")
                                ResultItem("Retorno", "R$ ${formatCurrency(project.actualReturn)}")
                                val projectRoi = if (project.investment > 0) ((project.actualReturn - project.investment) / project.investment) * 100 else 0.0
                                ResultItem("ROI", "${String.format("%.1f", projectRoi)}%")
                            }
                            if (project.productivityGain > 0 || project.costReduction > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (project.productivityGain > 0) {
                                        ResultItem("Produtividade", "+${String.format("%.1f", project.productivityGain)}%")
                                    }
                                    if (project.costReduction > 0) {
                                        ResultItem("Red. Custos", "R$ ${formatCurrency(project.costReduction)}")
                                    }
                                }
                            }
                        }
                    }
                }

                // Ranking
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader("Top Inovadores")
                }

                if (ranking.isNotEmpty()) {
                    items(ranking.take(5)) { user ->
                        RankingItem(
                            position = ranking.indexOf(user) + 1,
                            name = user.name,
                            points = user.points,
                            ideasCount = user.ideasCount
                        )
                    }
                }

                // AI Insights section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader("Insights IA (Gemini)")
                }

                item {
                    if (aiInsights != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = SecondaryGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Análise Executiva", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(aiInsights!!, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    aiLoading = true
                                    aiInsights = geminiController.generateDashboardInsights(dashboardData, strategyId, projectId)
                                    aiLoading = false
                                }
                            },
                            enabled = !aiLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (aiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Gerar Insights com IA")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectStatusRow(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = RoundedCornerShape(3.dp),
                color = color
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ResultItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatCurrency(value: Double): String {
    return when {
        value >= 1_000_000 -> "${String.format("%.1f", value / 1_000_000)}M"
        value >= 1_000 -> "${String.format("%.0f", value / 1_000)}K"
        else -> String.format("%.0f", value)
    }
}
