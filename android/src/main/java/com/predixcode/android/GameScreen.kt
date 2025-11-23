package com.predixcode.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.predixcode.board.Board
import com.predixcode.ui.ClickOutcome
import com.predixcode.board.pieces.Piece
import com.predixcode.ui.BoardController
import com.predixcode.ui.BoardViewState

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
    controller: BoardController,
    onBack: () -> Unit
) {
    val board: Board = remember { controller.board }

    var viewState by remember { mutableStateOf(controller.viewState) }

    val onSquareClick: (Int, Int) -> Unit = { x, y ->
        val event: ClickOutcome = controller.handleClick(x, y)
        viewState = controller.viewState

        // If desired, you can react to MOVE_REJECTED with a Snackbar using viewState.lastError.
        when (event.type) {
            ClickOutcome.Type.MOVE_APPLIED -> {
                // Android currently doesn't animate pieces; the recomposition
                // caused by viewState update is enough.
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = BoardUiDefaults.AppBackground,
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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (viewState.moveHistory.isNotEmpty()) {
                MoveHistoryBar(viewState.moveHistory)
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BoardUiDefaults.AppBackground),
            contentAlignment = Alignment.Center
        ) {
            val width = board.width
            val height = board.height

            val tileSizeByWidth = maxWidth / width.toFloat()
            val tileSizeByHeight = maxHeight / height.toFloat()
            val tileSize = if (tileSizeByWidth < tileSizeByHeight) tileSizeByWidth else tileSizeByHeight

            BoardWithCoordinates(
                board = board,
                viewState = viewState,
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

@Composable
fun BoardWithCoordinates(
    board: Board,
    viewState: BoardViewState,
    onSquareClick: (x: Int, y: Int) -> Unit,
    tileSize: Dp
) {
    val width = board.width
    val height = board.height

    val selected = viewState.selectedSquare
    val lastFrom = viewState.lastFrom
    val lastTo = viewState.lastTo
    val legalTargets = viewState.legalTargets

    Column {
        for (y in 0 until height) {
            Row {
                for (x in 0 until width) {
                    val isLight = (x + y) % 2 == 0
                    val baseColor = if (isLight) BoardUiDefaults.LightSquare else BoardUiDefaults.DarkSquare

                    val isSelected =
                        selected != null && selected.size >= 2 && selected[0] == x && selected[1] == y
                    val isLastMoveSquare =
                        (lastFrom != null && lastFrom.size >= 2 && lastFrom[0] == x && lastFrom[1] == y) ||
                                (lastTo != null && lastTo.size >= 2 && lastTo[0] == x && lastTo[1] == y)

                    val thisAlg = board.toAlg(x, y)
                    val isTarget = legalTargets.contains(thisAlg)

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
        isSelected -> BoardUiDefaults.SelectHighlight
        isLastMove -> BoardUiDefaults.LastMoveHighlight
        else -> baseColor
    }

    val coordColor: Color = if (isLight) BoardUiDefaults.DarkSquare else BoardUiDefaults.LightSquare

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
                        color = BoardUiDefaults.TargetDot,
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
    val imagePath = remember(piece) { piece.getImagePath(BoardUiDefaults.PieceTheme) ?: "" }

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