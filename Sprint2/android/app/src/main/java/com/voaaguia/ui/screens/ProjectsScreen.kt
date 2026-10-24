package com.voaaguia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.voaaguia.controller.AuthController
import com.voaaguia.controller.IdeaController
import com.voaaguia.controller.ProjectController
import com.voaaguia.model.*
import com.voaaguia.ui.components.*
import com.voaaguia.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    authController: AuthController,
    projectController: ProjectController,
    ideaController: IdeaController,
    onBack: () -> Unit
) {
    val currentUser by authController.currentUser.collectAsState()
    val projects by projectController.projects.collectAsState()
    val ideas by ideaController.ideas.collectAsState()
    val isLoading by projectController.isLoading.collectAsState()
    val message by projectController.message.collectAsState()
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf<Project?>(null) }
    var showResultsDialog by remember { mutableStateOf<Project?>(null) }

    LaunchedEffect(Unit) {
        projectController.loadProjects()
        ideaController.loadIdeas()
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            projectController.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projetos e Iniciativas") },
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
            if (currentUser?.role == UserRole.GESTOR) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = PrimaryBlue
                ) {
                    Icon(Icons.Default.Add, "Novo Projeto", tint = SurfaceWhite)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading && projects.isEmpty()) {
                LoadingOverlay(true)
            } else if (projects.isEmpty()) {
                EmptyState("Nenhum projeto cadastrado", Icons.Default.Assignment)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects) { project ->
                        ProjectCard(
                            project = project,
                            isManager = currentUser?.role == UserRole.GESTOR,
                            onUpdate = { showUpdateDialog = it },
                            onDelete = { scope.launch { projectController.deleteProject(it.id) } },
                            onAddResults = { showResultsDialog = it }
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
        }
    }

    // Create Project Dialog
    if (showCreateDialog) {
        val approvedIdeas = ideas.filter { it.status == IdeaStatus.APROVADA }
        CreateProjectDialog(
            approvedIdeas = approvedIdeas,
            onDismiss = { showCreateDialog = false },
            onSave = { title, description, ideaId, investment, expectedReturn, deadline ->
                scope.launch {
                    val succeeded = projectController.createProject(
                        title = title,
                        description = description,
                        ideaId = ideaId,
                        managerId = currentUser?.id ?: "",
                        managerName = currentUser?.name ?: "",
                        investment = investment,
                        expectedReturn = expectedReturn,
                        strategyId = ideas.first { it.id == ideaId }.strategyId,
                        expectedEndDate = java.time.LocalDate.parse(deadline).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
                    )
                    if (succeeded) showCreateDialog = false
                }
            }
        )
    }

    // Update Progress Dialog
    showUpdateDialog?.let { project ->
        UpdateProgressDialog(
            project = project,
            onDismiss = { showUpdateDialog = null },
            onSave = { progress, stage, status, notes ->
                scope.launch {
                    if (projectController.updateProgress(project.id, progress, stage, status, notes)) {
                        showUpdateDialog = null
                    }
                }
            }
        )
    }

    // Add Results Dialog
    showResultsDialog?.let { project ->
        AddResultsDialog(
            project = project,
            onDismiss = { showResultsDialog = null },
            onSave = { actualReturn, productivityGain, costReduction ->
                scope.launch {
                    if (projectController.addResults(project.id, actualReturn, productivityGain, costReduction)) {
                        showResultsDialog = null
                    }
                }
            }
        )
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isManager: Boolean,
    onUpdate: (Project) -> Unit,
    onDelete: (Project) -> Unit = {},
    onAddResults: (Project) -> Unit
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
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = project.description + "\nPrazo: " + (project.expectedEndDate?.toString()?.take(10) ?: "Não informado"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
                ProjectStatusChip(project.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progresso",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${project.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = project.progressPercent / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryBlue,
                    trackColor = BackgroundLight
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Investimento", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        "R$ ${String.format("%,.0f", project.investment)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text("Retorno", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        "R$ ${String.format("%,.0f", project.actualReturn)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (project.actualReturn > 0) StatusApproved else TextPrimary
                    )
                }
                Column {
                    Text("Etapa", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        when (project.stage) {
                            ProjectStage.IDEACAO -> "Ideação"
                            ProjectStage.VALIDACAO -> "Validação"
                            ProjectStage.DESENVOLVIMENTO -> "Desenv."
                            ProjectStage.PILOTO -> "Piloto"
                            ProjectStage.IMPLANTACAO -> "Implantação"
                            ProjectStage.OPERACAO -> "Operação"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Manager actions
            if (isManager) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { onDelete(project) }) { Icon(Icons.Default.Delete, "Excluir projeto") }
                    TextButton(onClick = { onUpdate(project) }) {
                        Icon(Icons.Default.Update, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atualizar")
                    }
                    TextButton(onClick = { onAddResults(project) }) {
                        Icon(Icons.Default.Assessment, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resultados")
                    }
                }
            }

            // Manager name
            Text(
                text = "Gestor: ${project.managerName}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    approvedIdeas: List<Idea>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIdeaId by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf(java.time.LocalDate.now().plusMonths(3).toString()) }
    var investment by remember { mutableStateOf("") }
    var expectedReturn by remember { mutableStateOf("") }
    var ideaExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Projeto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do Projeto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Prazo (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                // Linked idea
                if (approvedIdeas.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = ideaExpanded,
                        onExpandedChange = { ideaExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = approvedIdeas.find { it.id == selectedIdeaId }?.title ?: "Selecionar ideia...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ideia vinculada") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ideaExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = ideaExpanded,
                            onDismissRequest = { ideaExpanded = false }
                        ) {
                            approvedIdeas.forEach { idea ->
                                DropdownMenuItem(
                                    text = { Text(idea.title) },
                                    onClick = { selectedIdeaId = idea.id; ideaExpanded = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = investment,
                    onValueChange = { investment = it },
                    label = { Text("Investimento (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = expectedReturn,
                    onValueChange = { expectedReturn = it },
                    label = { Text("Retorno esperado (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        description,
                        selectedIdeaId,
                        investment.replace(',', '.').toDoubleOrNull() ?: 0.0,
                        expectedReturn.replace(',', '.').toDoubleOrNull() ?: 0.0,
                        deadline
                    )
                },
                enabled = title.isNotBlank() && description.isNotBlank() && selectedIdeaId.isNotBlank() && runCatching { java.time.LocalDate.parse(deadline) >= java.time.LocalDate.now() }.getOrDefault(false)
            ) {
                Text("Criar Projeto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProgressDialog(
    project: Project,
    onDismiss: () -> Unit,
    onSave: (Int, ProjectStage, ProjectStatus, String) -> Unit
) {
    var progress by remember { mutableStateOf(project.progressPercent.toFloat()) }
    var selectedStage by remember { mutableStateOf(project.stage) }
    var selectedStatus by remember { mutableStateOf(project.status) }
    var notes by remember { mutableStateOf(project.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atualizar Progresso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Progresso: ${progress.toInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    valueRange = 0f..100f,
                    steps = 9
                )

                Text("Etapa:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProjectStage.values().take(3).forEach { stage ->
                        FilterChip(
                            selected = selectedStage == stage,
                            onClick = { selectedStage = stage },
                            label = {
                                Text(
                                    when (stage) {
                                        ProjectStage.IDEACAO -> "Ideação"
                                        ProjectStage.VALIDACAO -> "Validação"
                                        ProjectStage.DESENVOLVIMENTO -> "Desenv."
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProjectStage.values().drop(3).forEach { stage ->
                        FilterChip(
                            selected = selectedStage == stage,
                            onClick = { selectedStage = stage },
                            label = {
                                Text(
                                    when (stage) {
                                        ProjectStage.PILOTO -> "Piloto"
                                        ProjectStage.IMPLANTACAO -> "Implant."
                                        ProjectStage.OPERACAO -> "Operação"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                Text("Status:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectStatus.values().forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = {
                                Text(
                                    when (status) {
                                        ProjectStatus.PLANEJAMENTO -> "Plan."
                                        ProjectStatus.EM_ANDAMENTO -> "Andamento"
                                        ProjectStatus.CONCLUIDO -> "Concluído"
                                        ProjectStatus.CANCELADO -> "Cancelado"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Observações") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(progress.toInt(), selectedStage, selectedStatus, notes) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddResultsDialog(
    project: Project,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double) -> Unit
) {
    var actualReturn by remember { mutableStateOf(project.actualReturn.toString()) }
    var productivityGain by remember { mutableStateOf(project.productivityGain.toString()) }
    var costReduction by remember { mutableStateOf(project.costReduction.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resultados do Projeto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(project.title, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = actualReturn,
                    onValueChange = { actualReturn = it },
                    label = { Text("Retorno financeiro real (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = productivityGain,
                    onValueChange = { productivityGain = it },
                    label = { Text("Ganho de produtividade (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = costReduction,
                    onValueChange = { costReduction = it },
                    label = { Text("Redução de custos (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    actualReturn.toDoubleOrNull() ?: 0.0,
                    productivityGain.toDoubleOrNull() ?: 0.0,
                    costReduction.toDoubleOrNull() ?: 0.0
                )
            }) {
                Text("Salvar Resultados")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
