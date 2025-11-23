package com.predixcode.android

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*
import com.predixcode.android.ui.ConfigMenuScreen
import com.predixcode.android.ui.GameScreen
import com.predixcode.core.GameConfig
import com.predixcode.core.GameFactory
import com.predixcode.core.GamePresets
import com.predixcode.core.ScenarioMeta
import com.predixcode.core.ui.BoardController

private val PRESETS: List<ScenarioMeta> = GamePresets.PRESETS
private val DEFAULT_CONFIG: GameConfig = GamePresets.DEFAULT_CONFIG

sealed class AppScreen { object Menu : AppScreen(); object Game : AppScreen() }

@Composable
fun App() {
    val activity = LocalActivity.current

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
                    activeController = GameFactory.createGame(preset, cfg)
                    screen = AppScreen.Game
                },
                onResume = {
                    if (activeController != null) screen = AppScreen.Game
                },
                onExit = { activity?.finish() }
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
                screen = AppScreen.Menu
            }
        }
    }
}