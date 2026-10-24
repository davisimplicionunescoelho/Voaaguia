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
import com.voaaguia.controller.AuthController
import com.voaaguia.controller.GuidelineController
import com.voaaguia.model.StrategicGuideline
import com.voaaguia.model.StrategyRevision
import com.voaaguia.model.UserRole
import com.voaaguia.ui.components.*
import com.voaaguia.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidelinesScreen(
    authController: AuthController,
    guidelineController: GuidelineController,
    onBack: () -> Unit
) {
    val currentUser by authController.currentUser.collectAsState()
    val guidelines by guidelineController.guidelines.collectAsState()
    val isLoading by guidelineController.isLoading.collectAsState()
    val message by guidelineController.message.collectAsState()
    val history by guidelineController.history.collectAsState()
    val isHistoryLoading by guidelineController.isHistoryLoading.collectAsState()
    val historyError by guidelineController.historyError.collectAsState()
    val scope = rememberCoroutineScope()
    var historyGuideline by remember { mutableStateOf<StrategicGuideline?>(null) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingGuideline by remember { mutableStateOf<StrategicGuideline?>(null) }

    LaunchedEffect(Unit) {
        guidelineController.loadGuidelines()
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            guidelineController.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orientações Estratégicas") },
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
        },
        floatingActionButton = {
            if (currentUser?.role == UserRole.LIDER) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = PrimaryBlue
                ) {
                    Icon(Icons.Default.Add, "Adicionar", tint = SurfaceWhite)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading && guidelines.isEmpty()) {
                LoadingOverlay(true)
            } else if (guidelines.isEmpty()) {
                EmptyState("Nenhuma orientação estratégica cadastrada", Icons.Default.Flag)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(guidelines) { guideline ->
                        GuidelineCard(
                            guideline = guideline,
                            isLeader = currentUser?.role == UserRole.LIDER,
                            onEdit = { editingGuideline = it },
                            onHistory = { guideline ->
                                historyGuideline = guideline
                                scope.launch { guidelineController.loadHistory(guideline.id) }
                            },
                            onDelete = {
                                scope.launch { guidelineController.deleteGuideline(it.id) }
                            }
                        )
                    }
                }
            }

            // Snackbar message
            message?.let {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(it)
                }
            }
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        GuidelineFormDialog(
            title = "Nova Orientação",
            onDismiss = { showCreateDialog = false },
            onSave = { title, description, category, campaign ->
                scope.launch {
                    guidelineController.createGuideline(
                        title = title,
                        description = description,
                        category = category,
                        authorId = currentUser?.id ?: "",
                        authorName = currentUser?.name ?: "",
                        campaign = campaign
                    )
                    showCreateDialog = false
                }
            }
        )
    }

    historyGuideline?.let { guideline ->
        StrategyHistoryDialog(
            guideline = guideline,
            revisions = history,
            isLoading = isHistoryLoading,
            error = historyError,
            onRetry = { scope.launch { guidelineController.loadHistory(guideline.id) } },
            onDismiss = {
                historyGuideline = null
                guidelineController.clearHistory()
            }
        )
    }
    // Edit Dialog
    editingGuideline?.let { guideline ->
        GuidelineFormDialog(
            title = "Editar Orientação",
            initialTitle = guideline.title,
            initialDescription = guideline.description,
            initialCategory = guideline.category,
            initialCampaign = guideline.campaign,
            onDismiss = { editingGuideline = null },
            onSave = { title, description, category, campaign ->
                scope.launch {
                    guidelineController.updateGuideline(
                        guideline.copy(title = title, description = description, category = category, campaign = campaign)
                    )
                    editingGuideline = null
                }
            }
        )
    }
}

@Composable
private fun StrategyHistoryDialog(
    guideline: StrategicGuideline,
    revisions: List<StrategyRevision>?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Histórico da estratégia")
                Text(guideline.title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        },
        text = {
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Não foi possível carregar o histórico: $error", color = StatusRejected)
                    OutlinedButton(onClick = onRetry) { Text("Tentar novamente") }
                }
                revisions.isNullOrEmpty() -> Text("Nenhuma versão registrada para esta estratégia.")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(revisions.asReversed()) { revision ->
                        Card(colors = CardDefaults.cardColors(containerColor = BackgroundLight)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val date = revision.date?.toString()?.replace('T', ' ')?.take(16) ?: "Data não informada"
                                Text("$date • ${revision.campaign}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(revision.title, fontWeight = FontWeight.Bold)
                                Text(revision.description, style = MaterialTheme.typography.bodySmall)
                                Text("Categoria: ${revision.category} • ${if (revision.isActive) "Ativa" else "Inativa"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
fun GuidelineCard(
    guideline: StrategicGuideline,
    isLeader: Boolean,
    onEdit: (StrategicGuideline) -> Unit,
    onDelete: (StrategicGuideline) -> Unit,
    onHistory: (StrategicGuideline) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (guideline.category.isNotEmpty()) {
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = guideline.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = guideline.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isLeader) {
                    Row {
                        IconButton(onClick = { onEdit(guideline) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, "Editar", tint = PrimaryBlueLight, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDelete(guideline) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Excluir", tint = StatusRejected, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = guideline.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            TextButton(onClick = { onHistory(guideline) }) { Text("Ver histórico") }
            if (guideline.authorName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Campanha: ${guideline.campaign} • Por: ${guideline.authorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun GuidelineFormDialog(
    title: String,
    initialTitle: String = "",
    initialDescription: String = "",
    initialCategory: String = "",
    initialCampaign: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var formTitle by remember { mutableStateOf(initialTitle) }
    var formDescription by remember { mutableStateOf(initialDescription) }
    var formCategory by remember { mutableStateOf(initialCategory) }
    var campaign by remember { mutableStateOf(initialCampaign) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formTitle,
                    onValueChange = { formTitle = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(value = campaign, onValueChange = { campaign = it }, label = { Text("Campanha") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = formCategory,
                    onValueChange = { formCategory = it },
                    label = { Text("Categoria") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = formDescription,
                    onValueChange = { formDescription = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(formTitle, formDescription, formCategory, campaign) },
                enabled = formTitle.isNotBlank() && formDescription.isNotBlank() && campaign.isNotBlank() && formCategory.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
