package com.voaaguia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.voaaguia.controller.GeminiController
import com.voaaguia.controller.IdeaController
import com.voaaguia.model.*
import com.voaaguia.ui.components.*
import com.voaaguia.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasScreen(
    authController: AuthController,
    ideaController: IdeaController,
    geminiController: GeminiController = GeminiController(),
    onBack: () -> Unit
) {
    val currentUser by authController.currentUser.collectAsState()
    val ideas by ideaController.ideas.collectAsState()
    val isLoading by ideaController.isLoading.collectAsState()
    val message by ideaController.message.collectAsState()
    val geminiLoading by geminiController.isLoading.collectAsState()
    val scope = rememberCoroutineScope()

    var editingIdea by remember { mutableStateOf<Idea?>(null) }
    var strategies by remember { mutableStateOf<List<StrategicGuideline>>(emptyList()) }
    var strategiesLoading by remember { mutableStateOf(false) }
    var strategiesError by remember { mutableStateOf<String?>(null) }
    var strategiesReloadKey by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf<Idea?>(null) }
    var showAiAnalysis by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf<IdeaStatus?>(null) }

    LaunchedEffect(currentUser?.id) {
        val filterByAuthor = if (currentUser?.role == UserRole.OPERADOR) currentUser?.id else null
        ideaController.loadIdeas(filterByAuthor)
    }

    LaunchedEffect(currentUser?.id, strategiesReloadKey) {
        strategiesLoading = true
        strategiesError = null
        try {
            strategies = com.voaaguia.model.repository.ApiRepository().getGuidelines()
            if (strategies.isEmpty()) strategiesError = "Nenhuma estratégia vigente foi encontrada."
        } catch (e: Exception) {
            strategies = emptyList()
            strategiesError = e.message ?: "Não foi possível carregar as estratégias."
        } finally {
            strategiesLoading = false
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            ideaController.clearMessage()
        }
    }

    val filteredIdeas = if (selectedFilter != null) {
        ideas.filter { it.status == selectedFilter }
    } else ideas

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ideias de Inovação") },
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
            if (currentUser?.role == UserRole.OPERADOR) {
                FloatingActionButton(
                    onClick = {
                        editingIdea = null
                        showCreateDialog = true
                        if (strategies.isEmpty()) strategiesReloadKey++
                    },
                    containerColor = SecondaryGold
                ) {
                    Icon(Icons.Default.Add, "Nova Ideia", tint = TextPrimary)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter chips
            if (currentUser?.role == UserRole.GESTOR) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Todas") }
                    )
                    FilterChip(
                        selected = selectedFilter == IdeaStatus.PENDENTE,
                        onClick = { selectedFilter = IdeaStatus.PENDENTE },
                        label = { Text("Pendentes") }
                    )
                    FilterChip(
                        selected = selectedFilter == IdeaStatus.APROVADA,
                        onClick = { selectedFilter = IdeaStatus.APROVADA },
                        label = { Text("Aprovadas") }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && ideas.isEmpty()) {
                    LoadingOverlay(true)
                } else if (filteredIdeas.isEmpty()) {
                    EmptyState(
                        if (currentUser?.role == UserRole.OPERADOR) "Nenhuma ideia cadastrada.\nToque + para adicionar!" else "Nenhuma ideia encontrada.",
                        Icons.Default.Lightbulb
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredIdeas) { idea ->
                            IdeaCard(
                                idea = idea,
                                userRole = currentUser?.role ?: UserRole.OPERADOR,
                                onReview = { showReviewDialog = it },
                                onEdit = { editingIdea = it; showCreateDialog = true },
                                onDelete = { scope.launch { ideaController.deleteIdea(it.id) } },
                                onVote = { scope.launch { ideaController.voteIdea(it.id) } },
                                onAiAnalyze = { selectedIdea ->
                                    scope.launch {
                                        val result = geminiController.analyzeIdea(selectedIdea)
                                        showAiAnalysis = result
                                    }
                                }
                            )
                        }
                    }
                }

                message?.let {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text(it)
                    }
                }

                if (geminiLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = SecondaryGold
                    )
                }
            }
        }
    }

    // Create Idea Dialog
    if (showCreateDialog) {
        CreateIdeaDialog(
            strategies = strategies,
            isStrategiesLoading = strategiesLoading,
            strategyError = strategiesError,
            submissionError = message?.takeIf { it.startsWith("Erro") },
            onRetryStrategies = { strategiesReloadKey++ },
            initial = editingIdea,
            onDismiss = { showCreateDialog = false },
            onSave = { title, description, category, strategyId ->
                scope.launch {
                    val succeeded = if (editingIdea != null) {
                        ideaController.updateIdea(editingIdea!!.copy(title = title, description = description, category = category, strategyId = strategyId))
                    } else {
                        ideaController.createIdea(
                            title = title,
                            description = description,
                            category = category,
                            authorId = currentUser?.id ?: "",
                            authorName = currentUser?.name ?: "",
                            department = currentUser?.department ?: "",
                            strategyId = strategyId
                        )
                    }
                    if (succeeded) showCreateDialog = false
                }
            }
        )
    }

    // Review Dialog (for Gestores)
    showReviewDialog?.let { idea ->
        ReviewIdeaDialog(
            idea = idea,
            geminiController = geminiController,
            onDismiss = { showReviewDialog = null },
            onApprove = { priority, feedback ->
                scope.launch {
                    ideaController.approveIdea(idea.id, priority, feedback)
                    showReviewDialog = null
                }
            },
            onReject = { feedback ->
                scope.launch {
                    ideaController.rejectIdea(idea.id, feedback)
                    showReviewDialog = null
                }
            }
        )
    }

    // AI Analysis Dialog
    showAiAnalysis?.let { analysis ->
        AlertDialog(
            onDismissRequest = { showAiAnalysis = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = SecondaryGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Análise Gemini AI")
                }
            },
            text = { Text(analysis, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(onClick = { showAiAnalysis = null }) { Text("Fechar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaCard(
    idea: Idea,
    userRole: UserRole,
    onReview: (Idea) -> Unit,
    onVote: (Idea) -> Unit,
    onEdit: (Idea) -> Unit = {},
    onDelete: (Idea) -> Unit = {},
    onAiAnalyze: ((Idea) -> Unit)? = null
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
                    Text(
                        text = idea.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = idea.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(idea.status)
                    PriorityChip(idea.priority)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ThumbUp, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${idea.votes}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${idea.authorName} • ${idea.department}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Row {
                    // AI analyze button
                    if (onAiAnalyze != null && userRole == UserRole.GESTOR) {
                        IconButton(onClick = { onAiAnalyze(idea) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.AutoAwesome, "IA", tint = SecondaryGold, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (userRole == UserRole.OPERADOR && idea.status == IdeaStatus.PENDENTE) {
                        IconButton(onClick = { onEdit(idea) }) { Icon(Icons.Default.Edit, "Editar") }
                        IconButton(onClick = { onDelete(idea) }) { Icon(Icons.Default.Delete, "Excluir") }
                    }
                    // Vote button
                    IconButton(onClick = { onVote(idea) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ThumbUp, "Votar", tint = PrimaryBlueLight, modifier = Modifier.size(18.dp))
                    }
                    // Review button for Gestores
                    if (userRole == UserRole.GESTOR && idea.status == IdeaStatus.PENDENTE) {
                        IconButton(onClick = { onReview(idea) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.RateReview, "Avaliar", tint = SecondaryGold, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Feedback
            if (idea.feedback.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = BackgroundLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Icon(Icons.Default.Comment, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = idea.feedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateIdeaDialog(
    strategies: List<StrategicGuideline>,
    isStrategiesLoading: Boolean = false,
    strategyError: String? = null,
    submissionError: String? = null,
    onRetryStrategies: () -> Unit = {},
    initial: Idea? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var strategyId by remember { mutableStateOf(initial?.strategyId ?: "") }

    LaunchedEffect(strategies, initial?.strategyId) {
        if (strategyId.isBlank() && strategies.isNotEmpty()) strategyId = strategies.first().id
    }

    val categories = listOf("Processo", "Tecnologia", "Produto", "Atendimento", "Logística", "Segurança", "Outro")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Ideia / Problema") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título da ideia") },
                    placeholder = { Text("Ex: Automatizar processo X") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descreva a ideia ou problema") },
                    placeholder = { Text("Detalhe o problema encontrado ou sua sugestão de melhoria...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                if (isStrategiesLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Carregando estratégias...", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    StrategySelector(strategies, strategyId) { strategyId = it }
                    if (strategyError != null) {
                        Text(strategyError, color = StatusRejected, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRetryStrategies) { Text("Tentar novamente") }
                    }
                }
                Text("Categoria:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                submissionError?.let {
                    Text(it, color = StatusRejected, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.drop(4).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description, category, strategyId) },
                enabled = title.isNotBlank() && description.isNotBlank() && category.isNotBlank() && strategyId.isNotBlank()
            ) {
                Text("Enviar Ideia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewIdeaDialog(
    idea: Idea,
    geminiController: GeminiController,
    onDismiss: () -> Unit,
    onApprove: (IdeaPriority, String) -> Unit,
    onReject: (String) -> Unit
) {
    var feedback by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(IdeaPriority.MEDIA) }
    var aiLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avaliar Ideia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(idea.title, fontWeight = FontWeight.Bold)
                Text(idea.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Divider()
                Text("Prioridade:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IdeaPriority.values().forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = {
                                Text(
                                    when (priority) {
                                        IdeaPriority.BAIXA -> "Baixa"
                                        IdeaPriority.MEDIA -> "Média"
                                        IdeaPriority.ALTA -> "Alta"
                                        IdeaPriority.CRITICA -> "Crítica"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Feedback") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                // AI Feedback suggestion button
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            aiLoading = true
                            val suggestion = geminiController.generateFeedback(idea, true)
                            if (suggestion != null) feedback = suggestion
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
                    Text("Sugerir Feedback (IA)")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onReject(feedback) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected)
                ) {
                    Text("Rejeitar")
                }
                Button(
                    onClick = { onApprove(selectedPriority, feedback) },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusApproved)
                ) {
                    Text("Aprovar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
