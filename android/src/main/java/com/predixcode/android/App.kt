package com.predixcode.android

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*
import com.predixcode.android.ui.ConfigMenuScreen
import com.predixcode.android.ui.GameScreen
import com.predixcode.core.GameConfig
import com.predixcode.core.ScenarioMeta
import com.predixcode.core.board.Board
import com.predixcode.core.rules.Rule
import com.predixcode.core.rules.RuleBuilder
import com.predixcode.core.ui.BoardController
import com.predixcode.core.fen.StartPositionService
// -----------------------------------------------------------------
// Constants / presets
// -----------------------------------------------------------------

private const val STANDARD_FEN =
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

private const val BUREAUCRAT_FEN =
    "rnbqkbnr/pppppppp/3c4/8/8/4C3/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

// Presets only: they just pre-fill the config screen (same as PRESETS in App.java)
val PRESETS: List<ScenarioMeta> = listOf(
    ScenarioMeta("Standard",           STANDARD_FEN,   false, 1, 1),
    ScenarioMeta("Double move x2",     STANDARD_FEN,   false,  2, 2),
    ScenarioMeta("Bureaucrat",         BUREAUCRAT_FEN, true, 1, 1),
    ScenarioMeta("Bureaucrat + DM x2", BUREAUCRAT_FEN, true,  2, 2)
)

// The last-used configuration (so the UI remembers choices)
// Mirrors App.currentConfig initial value.
private val DEFAULT_CONFIG = GameConfig(
    null,
    false,
    1,
    1
)

sealed class AppScreen {
    object Menu : AppScreen()
    object Game : AppScreen()
}

// -----------------------------------------------------------------
// Top-level app composable
// -----------------------------------------------------------------

@Composable
fun App() {
    val activity = LocalActivity.current

    // Same top-level state as App.java, but we now persist the controller,
    // not just the raw Board.
    var selectedPreset by remember { mutableStateOf(PRESETS[0]) }
    var currentConfig by remember { mutableStateOf(DEFAULT_CONFIG) }
    var activeController by remember { mutableStateOf<BoardController?>(null) }
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Menu) }

    when (screen) {
        is AppScreen.Menu -> {
            ConfigMenuScreen(
                presets = PRESETS,
                selected = selectedPreset,
                config = currentConfig,
                canResume = activeController != null,
                onStart = { preset, cfg ->
                    selectedPreset = preset
                    currentConfig = cfg
                    activeController = startNewBoardFromConfig(preset, cfg)
                    screen = AppScreen.Game
                },
                onResume = {
                    if (activeController != null) {
                        screen = AppScreen.Game
                    }
                },
                onExit = {
                    activity?.finish()
                }
            )
        }

        is AppScreen.Game -> {
            val controller = activeController
            if (controller != null) {
                GameScreen(
                    controller = controller,
                    onBack = { screen = AppScreen.Menu }
                )
            } else {
                // If somehow we got here without a controller, go back to menu
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
): BoardController {
    val fenOverride = cfg.fenOverride()
    val baseFen = if (fenOverride == null || fenOverride.isBlank()) {
        preset.defaultFen
    } else {
        fenOverride
    }

    // Apply board-size + Chess960 rules from core
    val finalFen = StartPositionService.buildStartingFen(baseFen, cfg)

    val board = Board.fromFen(finalFen)

    val rules: List<Rule> = RuleBuilder.buildRules(cfg)
    board.setRules(rules)
    board.ensureRules()
    for (rule in board.rules) {
        rule.onGameStart(board)
    }

    return BoardController(board)
}