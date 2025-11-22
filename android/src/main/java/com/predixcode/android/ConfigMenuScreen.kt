package com.predixcode.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.predixcode.GameConfig
import com.predixcode.ScenarioMeta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigMenuScreen(
    presets: List<ScenarioMeta>,
    selected: ScenarioMeta?,
    config: GameConfig?,
    canResume: Boolean,
    onStart: (ScenarioMeta, GameConfig) -> Unit,
    onResume: () -> Unit,
    onExit: () -> Unit
) {
    val effectivePresets = presets.ifEmpty { PRESETS }
    val initialPreset = selected ?: effectivePresets.first()

    var preset by remember(selected, config) {
        mutableStateOf(initialPreset)
    }
    var presetMenuExpanded by remember(selected, config) {
        mutableStateOf(false)
    }

    val initialFen: String = when {
        config?.fenOverride() != null && !config.fenOverride().isBlank() ->
            config.fenOverride()
        else ->
            preset.defaultFen
    }

    val initialBureaucrat: Boolean =
        config?.bureaucratRule() ?: preset.isDefaultBureaucratRule

    val initialMultiMove: Boolean =
        config?.multipleMoveRule() ?: preset.isDefaultMultipleMoveRule

    val initialWhiteMoves: Int =
        config?.whiteMovesPerTurn() ?: preset.defaultWhiteMovesPerTurn

    val initialBlackMoves: Int =
        config?.blackMovesPerTurn() ?: preset.defaultBlackMovesPerTurn

    var fenText by remember(selected, config) { mutableStateOf(initialFen) }
    var bureaucrat by remember(selected, config) { mutableStateOf(initialBureaucrat) }
    var multiMove by remember(selected, config) { mutableStateOf(initialMultiMove) }
    var whiteMoves by remember(selected, config) {
        mutableStateOf(initialWhiteMoves.toString())
    }
    var blackMoves by remember(selected, config) {
        mutableStateOf(initialBlackMoves.toString())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1F1E1B),
                        Color(0xFF262522)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Custom Chess",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Configure a game with variants and custom FEN, chess.com style.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main cards
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f, fill = true)
                    .padding(top = 8.dp)
            ) {
                // Preset + FEN card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Preset & position",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Preset selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Preset",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            ExposedDropdownMenuBox(
                                expanded = presetMenuExpanded,
                                onExpandedChange = { presetMenuExpanded = !presetMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = preset.name,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Preset") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = presetMenuExpanded
                                        )
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true
                                )

                                ExposedDropdownMenu(
                                    expanded = presetMenuExpanded,
                                    onDismissRequest = { presetMenuExpanded = false }
                                ) {
                                    effectivePresets.forEach { meta ->
                                        DropdownMenuItem(
                                            text = { Text(meta.name) },
                                            onClick = {
                                                preset = meta
                                                presetMenuExpanded = false

                                                fenText = meta.defaultFen
                                                bureaucrat = meta.isDefaultBureaucratRule
                                                multiMove = meta.isDefaultMultipleMoveRule
                                                whiteMoves = meta.defaultWhiteMovesPerTurn.toString()
                                                blackMoves = meta.defaultBlackMovesPerTurn.toString()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // FEN input
                        OutlinedTextField(
                            value = fenText,
                            onValueChange = { fenText = it },
                            label = { Text("Start position (FEN)") },
                            placeholder = { Text("Leave empty to use preset's default FEN") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Rules card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Rules",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Toggle additional rules similar to chess.com variants.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Bureaucrat rule
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Bureaucrat rule",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Special capture behavior for the bureaucrat piece.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = bureaucrat,
                                onCheckedChange = { bureaucrat = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Multi-move rule
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Multiple moves per turn",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Allow each side to move multiple times each turn.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = multiMove,
                                onCheckedChange = { multiMove = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Moves per color
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Moves per turn (used when multi-move is enabled):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = whiteMoves,
                                    onValueChange = { whiteMoves = it },
                                    label = { Text("White") },
                                    modifier = Modifier.width(96.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = blackMoves,
                                    onValueChange = { blackMoves = it },
                                    label = { Text("Black") },
                                    modifier = Modifier.width(96.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom action bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val meta = preset

                            val fenOverride: String? =
                                fenText.trim().takeIf { it.isNotEmpty() }

                            val wMoves = parseIntOrDefault(whiteMoves, 1)
                            val bMoves = parseIntOrDefault(blackMoves, 1)

                            val cfg = GameConfig(
                                fenOverride,
                                bureaucrat,
                                multiMove,
                                wMoves,
                                bMoves
                            )

                            onStart(meta, cfg)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Start new game")
                    }

                    OutlinedButton(
                        onClick = onResume,
                        enabled = canResume,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Resume")
                    }

                    TextButton(
                        onClick = onExit,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Exit")
                    }
                }
            }
        }
    }
}

// Unchanged helper
private fun parseIntOrDefault(text: String, default: Int): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return default
    return trimmed.toIntOrNull() ?: default
}