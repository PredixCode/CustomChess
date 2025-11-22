package com.predixcode.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.predixcode.board.Board
import com.predixcode.board.ClickOutcome
import com.predixcode.board.pieces.Piece

// ---------------------- Constants / visuals -----------------------

private const val THEME = "neo/upscale"

private val APP_BG = Color(0xFF262522)
private val BOARD_LIGHT = Color(0xFFECECD0)
private val BOARD_DARK = Color(0xFF749552)

// Highlights
private val SELECT_COLOR = Color(0xBFE1E16E)
private val LAST_MOVE_COLOR = Color(0xBFC3CD5A)
private val TARGET_DOT_COLOR = Color(0x2A616161)

// ---------------------- State models -----------------------------

data class SquarePos(val x: Int, val y: Int)

data class UiState(
    val selected: SquarePos? = null,
    val legalTargets: Set<SquarePos> = emptySet(),
    val lastFrom: SquarePos? = null,
    val lastTo: SquarePos? = null,
    val moveHistory: List<String> = emptyList()
)

// ---------------------- Piece image cache ------------------------

private object PieceImageCache {
    private const val TAG = "PieceImage"
    private val cache: MutableMap<String, Bitmap?> = mutableMapOf()

    fun getBitmap(context: Context, corePath: String): Bitmap? {
        val normalized = corePath.removePrefix("/")

        return cache.getOrPut(normalized) {
            try {
                Log.d(TAG, "Loading piece image from assets: $normalized")
                context.assets.open(normalized).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load piece image: $normalized", e)
                null
            }
        }
    }
}

// ---------------------- Game screen ------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    board: Board,
    onBack: () -> Unit          // <‑‑ NEW PARAM
) {
    var uiState by remember { mutableStateOf(UiState()) }
    var boardVersion by remember { mutableIntStateOf(0) }

    val onSquareClick: (Int, Int) -> Unit = { x, y ->
        val out: ClickOutcome = board.handleSquareClick(x, y)

        when (out.type) {
            ClickOutcome.Type.SELECT -> {
                val selectedArray = out.selected
                val selected = selectedArray?.let { SquarePos(it[0], it[1]) }

                val targetStrings: Set<String> = out.legalTargets ?: emptySet()
                val targetSquares: Set<SquarePos> = targetStrings
                    .map { alg ->
                        val coords = board.fromAlg(alg)
                        SquarePos(coords[0], coords[1])
                    }
                    .toSet()

                uiState = uiState.copy(
                    selected = selected,
                    legalTargets = targetSquares
                )
            }

            ClickOutcome.Type.MOVE_APPLIED -> {
                val fromArray = out.from
                val toArray = out.to
                val from = fromArray?.let { SquarePos(it[0], it[1]) }
                val to = toArray?.let { SquarePos(it[0], it[1]) }

                val fromAlg = from?.let { board.toAlg(it.x, it.y) }
                val toAlg = to?.let { board.toAlg(it.x, it.y) }

                val newHistory =
                    if (fromAlg != null && toAlg != null)
                        uiState.moveHistory + "$fromAlg-$toAlg"
                    else
                        uiState.moveHistory

                uiState = uiState.copy(
                    selected = null,
                    legalTargets = emptySet(),
                    lastFrom = from,
                    lastTo = to,
                    moveHistory = newHistory
                )

                boardVersion++
            }

            ClickOutcome.Type.MOVE_REJECTED,
            ClickOutcome.Type.NOOP -> {
                uiState = uiState.copy(
                    selected = null,
                    legalTargets = emptySet()
                )
            }
        }
    }

    // Force recomposition when the board changes
    @Suppress("UNUSED_VARIABLE", "unused")
    val boardVersionSnapshot = boardVersion

    Scaffold(
        containerColor = APP_BG,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Custom Chess",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Playing with custom rules",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {                      // <‑‑ NEW
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2F2E29)
                )
            )
        },
        bottomBar = {
            if (uiState.moveHistory.isNotEmpty()) {
                MoveHistoryBar(uiState.moveHistory)
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(APP_BG),
            contentAlignment = Alignment.Center
        ) {
            val width = board.width
            val height = board.height

            val tileSizeByWidth = maxWidth / width.toFloat()
            val tileSizeByHeight = maxHeight / height.toFloat()
            val tileSize = if (tileSizeByWidth < tileSizeByHeight) tileSizeByWidth else tileSizeByHeight

            BoardWithCoordinates(
                board = board,
                uiState = uiState,
                onSquareClick = onSquareClick,
                tileSize = tileSize
            )
        }
    }
}

@Composable
private fun MoveHistoryBar(moves: List<String>) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Text(
                    text = "Moves",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            itemsIndexed(moves) { index, move ->
                AssistChip(
                    onClick = { /* no-op */ },
                    label = { Text("${index + 1}. $move") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

// ---------------------- Board / piece composables ----------------
// (unchanged, except for imports) – keep your existing implementations.

@Composable
fun BoardWithCoordinates(
    board: Board,
    uiState: UiState,
    onSquareClick: (x: Int, y: Int) -> Unit,
    tileSize: Dp
) {
    val width = board.width
    val height = board.height

    Column {
        for (y in 0 until height) {
            Row {
                for (x in 0 until width) {
                    val pos = SquarePos(x, y)
                    val isLight = (x + y) % 2 == 0
                    val baseColor = if (isLight) BOARD_LIGHT else BOARD_DARK

                    val isSelected = uiState.selected == pos
                    val isLastMoveSquare =
                        (uiState.lastFrom == pos) || (uiState.lastTo == pos)
                    val isTarget = uiState.legalTargets.contains(pos)

                    val piece = board.getPieceAt(x, y)

                    val rankLabel: String? =
                        if (x == 0) (height - y).toString() else null
                    val fileChar: Char? =
                        if (y == height - 1) ('a'.code + x).toChar() else null

                    BoardSquare(
                        piece = piece,
                        baseColor = baseColor,
                        isLight = isLight,
                        isSelected = isSelected,
                        isLastMove = isLastMoveSquare,
                        isTarget = isTarget,
                        tileSize = tileSize,
                        fileChar = fileChar,
                        rankLabel = rankLabel,
                        onClick = { onSquareClick(x, y) }
                    )
                }
            }
        }
    }
}

@Composable
fun BoardSquare(
    piece: Piece?,
    baseColor: Color,
    isLight: Boolean,
    isSelected: Boolean,
    isLastMove: Boolean,
    isTarget: Boolean,
    tileSize: Dp,
    fileChar: Char?,
    rankLabel: String?,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> SELECT_COLOR
        isLastMove -> LAST_MOVE_COLOR
        else -> baseColor
    }

    val coordColor: Color = if (isLight) BOARD_DARK else BOARD_LIGHT

    Box(
        modifier = Modifier
            .size(tileSize)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (piece != null) {
            PieceImage(
                piece = piece,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isTarget) {
            Box(
                modifier = Modifier
                    .size(tileSize * 0.25f)
                    .background(
                        color = TARGET_DOT_COLOR,
                        shape = CircleShape
                    )
            )
        }

        if (rankLabel != null) {
            Text(
                text = rankLabel,
                color = coordColor,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
            )
        }

        if (fileChar != null) {
            Text(
                text = fileChar.toString(),
                color = coordColor,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
            )
        }
    }
}

@Composable
fun PieceImage(
    piece: Piece,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imagePath = remember(piece) { piece.getImagePath(THEME) ?: "" }

    if (imagePath.isEmpty()) return

    val bitmap = remember(imagePath) {
        PieceImageCache.getBitmap(context, imagePath)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.FillBounds
        )
    }
}
