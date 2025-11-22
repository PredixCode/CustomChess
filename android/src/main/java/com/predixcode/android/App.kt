package com.predixcode.android

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*
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
val PRESETS: List<ScenarioMeta> = listOf(
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
    val activity = LocalActivity.current

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
                    selectedPreset = preset
                    currentConfig = cfg
                    activeBoard = startNewBoardFromConfig(preset, cfg)
                    screen = AppScreen.Game
                },
                onResume = {
                    if (activeBoard != null) {
                        screen = AppScreen.Game
                    }
                },
                onExit = {
                    activity?.finish()
                }
            )
        }

        is AppScreen.Game -> {
            val board = activeBoard
            if (board != null) {
                GameScreen(
                    board = board,
                    onBack = { screen = AppScreen.Menu }   // <‑‑ NEW
                )
            } else {
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