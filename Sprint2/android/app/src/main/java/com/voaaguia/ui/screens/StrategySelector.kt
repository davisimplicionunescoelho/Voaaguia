package com.voaaguia.ui.screens
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.voaaguia.model.StrategicGuideline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategySelector(strategies: List<StrategicGuideline>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = strategies.find { it.id == selected }?.title ?: "Selecione a estratégia vigente",
            onValueChange = {}, readOnly = true, label = { Text("Estratégia") },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            strategies.forEach { strategy -> DropdownMenuItem(text = { Text(strategy.title + " • " + strategy.campaign) }, onClick = { onSelect(strategy.id); expanded = false }) }
        }
    }
    if (strategies.isEmpty()) Text("Nenhuma estratégia vigente. Solicite o cadastro à liderança.", style = MaterialTheme.typography.bodySmall)
}
