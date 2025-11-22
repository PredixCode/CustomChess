package com.predixcode.android

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.predixcode.GameConfig
import com.predixcode.ScenarioMeta
import com.predixcode.board.Board
import com.predixcode.rules.Rule
import com.predixcode.rules.RuleBuilder

// -----------------------------------------------------------------
// Constants / presets – mirror App.java
// -----------------------------------------------------------------

private const val STANDARD_FEN =
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

private const val BUREAUCRAT_FEN =
    "rnbqkbnr/pppppppp/3c4/8/8/4C3/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

// Presets only: they just pre-fill the config screen (same as PRESETS in App.java)
private val PRESETS: List<ScenarioMeta> = listOf(
    // name, defaultFEN, bureaucrat, multiMove, whiteMoves, blackMoves
    ScenarioMeta("Standard",           STANDARD_FEN,   false, false, 1, 1),
    ScenarioMeta("Double move x2",     STANDARD_FEN,   false, true,  2, 2),
    ScenarioMeta("Bureaucrat",         BUREAUCRAT_FEN, true,  false, 1, 1),
    ScenarioMeta("Bureaucrat + DM x2", BUREAUCRAT_FEN, true,  true,  2, 2)
)

// The last-used configuration (so the UI remembers choices)
// Mirrors App.currentConfig initial value.
private val DEFAULT_CONFIG = GameConfig(
    null,   // fenOverride
    false,  // bureaucratRule
    false,  // multipleMoveRule
    1,      // whiteMovesPerTurn
    1       // blackMovesPerTurn
)

sealed class AppScreen {
    object Menu : AppScreen()
    object Game : AppScreen()
}

// -----------------------------------------------------------------
// Top-level app composable
// -----------------------------------------------------------------

@Composable
fun ChessApp() {
    val activity = LocalContext.current as? Activity

    // Same top-level state as App.java
    var selectedPreset by remember { mutableStateOf(PRESETS[0]) }
    var currentConfig by remember { mutableStateOf(DEFAULT_CONFIG) }
    var activeBoard by remember { mutableStateOf<Board?>(null) }
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Menu) }

    when (screen) {
        is AppScreen.Menu -> {
            ConfigMenuScreen(
                presets = PRESETS,
                selected = selectedPreset,
                config = currentConfig,
                canResume = activeBoard != null,
                onStart = { preset, cfg ->
                    // mirrors App.showConfigMenu -> onStart
                    selectedPreset = preset
                    currentConfig = cfg
                    activeBoard = startNewBoardFromConfig(preset, cfg)
                    screen = AppScreen.Game
                },
                onResume = {
                    // mirrors App.resumeGame
                    if (activeBoard != null) {
                        screen = AppScreen.Game
                    }
                },
                onExit = {
                    // mirrors Platform::exit
                    activity?.finish()
                }
            )
        }

        is AppScreen.Game -> {
            val board = activeBoard
            if (board != null) {
                ChessGameScreen(board = board)
            } else {
                // Fallback: if something went wrong, go back to menu
                screen = AppScreen.Menu
            }
        }
    }
}

// -----------------------------------------------------------------
// Logic equivalent of App.startNewGame + createBoardFromConfig
// -----------------------------------------------------------------

private fun startNewBoardFromConfig(
    preset: ScenarioMeta,
    cfg: GameConfig
): Board {
    // App.createBoardFromConfig(preset, cfg)
    val fenOverride = cfg.fenOverride()
    val fenToUse = if (fenOverride == null || fenOverride.isBlank()) {
        preset.defaultFen
    } else {
        fenOverride
    }

    val board = Board.fromFen(fenToUse)

    // App.startNewGame: RuleBuilder.buildRules(currentConfig)
    val rules: List<Rule> = RuleBuilder.buildRules(cfg)
    board.setRules(rules)
    board.ensureRules()
    for (rule in board.rules) {
        rule.onGameStart(board)
    }

    return board
}

// -----------------------------------------------------------------
// Compose version of ConfigMenu.create(...)
// -----------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigMenuScreen(
    presets: List<ScenarioMeta>,
    selected: ScenarioMeta?,
    config: GameConfig?,                // may be null on first launch
    canResume: Boolean,
    onStart: (ScenarioMeta, GameConfig) -> Unit,
    onResume: () -> Unit,
    onExit: () -> Unit
) {
    val effectivePresets = if (presets.isNotEmpty()) presets else PRESETS
    val initialPreset = selected ?: effectivePresets.first()

    // --- Local preset selection (ComboBox equivalent) ---
    var preset by remember(selected, config) {
        mutableStateOf(initialPreset)
    }
    var presetMenuExpanded by remember(selected, config) {
        mutableStateOf(false)
    }

    // Mirror Java initial field computation using selected + config
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Custom Chess – Configuration",
            style = MaterialTheme.typography.headlineSmall
        )

        // --- Preset selector (ComboBox equivalent) ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Preset:",
                style = MaterialTheme.typography.labelMedium
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
                        .fillMaxWidth()
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

                                // When preset changes, reset fields to preset defaults
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

        // --- FEN input ---
        OutlinedTextField(
            value = fenText,
            onValueChange = { fenText = it },
            label = { Text("Start position (FEN)") },
            placeholder = { Text("Leave empty to use preset's default FEN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // --- Rule toggles ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Rules:",
                style = MaterialTheme.typography.labelMedium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = bureaucrat,
                    onCheckedChange = { bureaucrat = it }
                )
                Text(text = "Enable Bureaucrat rule")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = multiMove,
                    onCheckedChange = { multiMove = it }
                )
                Text(text = "Enable multiple moves per turn")
            }
        }

        // --- Moves per color ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Moves per turn (if multi-move enabled):",
                style = MaterialTheme.typography.labelMedium
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

        Spacer(modifier = Modifier.weight(1f))

        // --- Buttons: Start / Resume / Exit ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val meta = preset

                    // Java: if fen is blank, pass null (=> use preset FEN)
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
                }
            ) {
                Text("Start new game")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onResume,
                enabled = canResume
            ) {
                Text("Resume")
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(onClick = onExit) {
                Text("Exit")
            }
        }
    }
}

// Java-style parseIntOrDefault
private fun parseIntOrDefault(text: String, default: Int): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return default
    return trimmed.toIntOrNull() ?: default
}