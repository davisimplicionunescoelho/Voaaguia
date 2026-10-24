package com.voaaguia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voaaguia.controller.DashboardController
import com.voaaguia.ui.components.*
import com.voaaguia.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    dashboardController: DashboardController,
    onBack: () -> Unit
) {
    val ranking by dashboardController.ranking.collectAsState()
    val isLoading by dashboardController.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val error by dashboardController.error.collectAsState()

    LaunchedEffect(Unit) {
        dashboardController.loadRanking()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking de Inovadores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = SurfaceWhite,
                    navigationIconContentColor = SurfaceWhite
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            LoadingOverlay(true)
        } else if (ranking.isEmpty()) {
            EmptyState(error ?: "Nenhum colaborador no ranking ainda", Icons.Default.EmojiEvents)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                itemsIndexed(ranking) { index, user ->
                    RankingItem(
                        position = index + 1,
                        name = user.name,
                        points = user.points,
                        ideasCount = user.ideasCount
                    )
                    if (index < ranking.size - 1) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = DividerColor
                        )
                    }
                }
            }
        }
    }
}
