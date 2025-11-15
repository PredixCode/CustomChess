package com.predixcode.ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.rules.Rule;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public abstract class GUI extends Application {

    private static final int SIZE = 8;
    private static final double TILE = 88; // tile size in px
    private static final double PADDING = 24; // space for coordinates
    private static final String THEME = "neo"; // folder under /pieces/

    protected Board board;
    private final Rectangle[][] squares = new Rectangle[SIZE][SIZE];
    private final Group highlightLayer = new Group();
    private final Group pieceLayer = new Group();
    private final Group overlayLayer = new Group();
    private final Map<Piece, ImageView> pieceNodes = new HashMap<>();

    // Selection and last move tracking
    private int[] selected = null; // [x, y] board coords
    private Set<String> legalTargets = new HashSet<>();
    private int[] lastFrom = null, lastTo = null;

    private final Paint selectColor = Paint.valueOf("#F6F66980"); // translucent yellow
    private final Paint lastMoveColor = Paint.valueOf("#f6f6693b");;
    private final Paint targetDotColor = Paint.valueOf("#6161612a");

    private final List<String> moveHistory = new ArrayList<>();
    private GameInfoPanel infoPanel;

    @Override
    public void start(Stage stage) throws Exception {
        if (this.board == null) {
            this.board = Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        }

        for (Rule r : board.rules) {
            System.out.println("Board has rule: " + r.getClass().getSimpleName());
        }
        ensureRules();

        final double baseW = PADDING + SIZE * TILE + PADDING;
        final double baseH = PADDING + SIZE * TILE + PADDING;

        // Center area: scalable board content
        javafx.scene.layout.StackPane center = new javafx.scene.layout.StackPane();

        Group content = new Group();

        Group boardGroup = new Group();
        boardGroup.setLayoutX(PADDING);
        boardGroup.setLayoutY(PADDING);

        // Layers
        boardGroup.getChildren().add(buildSquares());
        boardGroup.getChildren().add(highlightLayer);
        boardGroup.getChildren().add(pieceLayer);
        boardGroup.getChildren().add(overlayLayer);
        highlightLayer.setMouseTransparent(true);
        overlayLayer.setMouseTransparent(true);

        content.getChildren().add(boardGroup);
        content.getChildren().add(buildCoordinates());

        center.getChildren().add(content);

        // Right panel
        double panelWidth = 400;
        infoPanel = new GameInfoPanel(panelWidth);

        // Root with right panel
        BorderPane root = new BorderPane();
        root.setCenter(center);
        root.setRight(infoPanel);

        // Scene with extra width for the panel
        Scene scene = new Scene(root, baseW + panelWidth, baseH);
        stage.setTitle("Predix Chess • JavaFX");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

        // Scale the board content to fit center area
        javafx.beans.binding.DoubleBinding scale = javafx.beans.binding.Bindings.createDoubleBinding(
            () -> {
                double w = center.getWidth();
                double h = center.getHeight();
                if (w <= 0 || h <= 0) return 1.0;
                return Math.min(w / baseW, h / baseH);
            },
            center.widthProperty(), center.heightProperty()
        );
        content.scaleXProperty().bind(scale);
        content.scaleYProperty().bind(scale);

        loadPieces();
        refreshPieces();

        // Initial panel refresh
        infoPanel.refresh(board, moveHistory);
    }

    private void ensureRules() {
        if (board.rules == null) {
            board.rules = new java.util.ArrayList<>();
        }
        boolean hasStandard = board.rules.stream()
            .anyMatch(r -> r instanceof com.predixcode.core.rules.Standard);
        if (!hasStandard) {
            System.out.println("[ensureRules] Injecting Standard rule");
            board.rules.add(new com.predixcode.core.rules.Standard());
        }
    }

    private Pane buildSquares() {
        Pane pane = new Pane();

        // Theme-based board background
        String bgPath = "/pieces/" + THEME + "/board.png";
        InputStream bgStream = getClass().getResourceAsStream(bgPath);
        ImageView bg = new ImageView(new Image(bgStream));
        bg.setFitWidth(SIZE * TILE);
        bg.setFitHeight(SIZE * TILE);
        bg.setPreserveRatio(false); // Fill the board area exactly
        bg.setSmooth(true);
        bg.setMouseTransparent(true);
        pane.getChildren().add(bg); // background at the very bottom

        // Invisible tile rectangles for input handling (and for highlights to layer above)
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final int fx = x;
                final int fy = y;

                Rectangle r = new Rectangle(x * TILE, y * TILE, TILE, TILE);
                r.setFill(javafx.scene.paint.Color.TRANSPARENT); // board.png provides visuals
                r.setPickOnBounds(true);
                r.setOnMouseClicked(evt -> onSquareClick(fx, fy));
                squares[y][x] = r;
                pane.getChildren().add(r);
            }
        }
        return pane;
    }

    private void repaintBoard() {
        // Keep tiles transparent so the themed board.png remains visible
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                squares[y][x].setFill(javafx.scene.paint.Color.TRANSPARENT);
            }
        }
    }

    private Pane buildCoordinates() {
        Pane coords = new Pane();
        coords.setMouseTransparent(true);
        coords.setPrefSize(PADDING + SIZE * TILE + PADDING, PADDING + SIZE * TILE + PADDING);

        // Files a-h (bottom)
        for (int x = 0; x < SIZE; x++) {
            char file = (char) ('a' + x);
            Text t = text(String.valueOf(file), 12);
            t.setX(PADDING + x * TILE + TILE - 14);
            t.setY(PADDING + SIZE * TILE + 16);
            t.setFill(Paint.valueOf("#333"));
            coords.getChildren().add(t);
        }
        // Ranks 8-1 (left)
        for (int y = 0; y < SIZE; y++) {
            int rank = SIZE - y;
            Text t = text(String.valueOf(rank), 12);
            t.setX(6);
            t.setY(PADDING + y * TILE + 16);
            t.setFill(Paint.valueOf("#333"));
            coords.getChildren().add(t);
        }
        return coords;
    }

    private Text text(String s, int size) {
        Text t = new Text(s);
        t.setFont(Font.font(size));
        return t;
    }

    private void loadPieces() {
        pieceLayer.getChildren().clear();
        pieceNodes.clear();

        for (Piece p : board.pieces) {
            ImageView iv = new ImageView(loadImageFor(p));
            iv.setFitWidth(TILE * 0.9);
            iv.setFitHeight(TILE * 0.9);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            DropShadow ds = new DropShadow();
            ds.setRadius(8);
            ds.setColor(javafx.scene.paint.Color.web("#00000040")); // JavaFX Color
            iv.setEffect(ds);

            int x = p.getX();
            int y = p.getY();
            placeNodeAt(iv, x, y);

            iv.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    onSquareClick(p.getX(), p.getY());
                }
            });
            pieceNodes.put(p, iv);
            pieceLayer.getChildren().add(iv);
        }
    }

    private void refreshPieces() {
        // Build an identity-based membership of current pieces
        java.util.IdentityHashMap<Piece, Boolean> currentMap = new java.util.IdentityHashMap<>();
        for (Piece p : board.pieces) currentMap.put(p, Boolean.TRUE);

        // Remove nodes for pieces that no longer exist (identity-based)
        java.util.List<Piece> toRemove = new java.util.ArrayList<>();
        for (Piece p : pieceNodes.keySet()) {
            if (!currentMap.containsKey(p)) toRemove.add(p);
        }
        for (Piece p : toRemove) {
            ImageView iv = pieceNodes.remove(p);
            if (iv != null) pieceLayer.getChildren().remove(iv);
        }

        // Add nodes for new pieces (identity-based)
        for (Piece p : board.pieces) {
            if (!pieceNodes.containsKey(p)) {
                ImageView iv = new ImageView(loadImageFor(p));
                iv.setFitWidth(TILE * 0.9);
                iv.setFitHeight(TILE * 0.9);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                DropShadow ds = new DropShadow();
                ds.setRadius(8);
                ds.setColor(javafx.scene.paint.Color.web("#00000040"));
                iv.setEffect(ds);
                iv.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY) onSquareClick(p.getX(), p.getY());
                });
                pieceNodes.put(p, iv);
                pieceLayer.getChildren().add(iv);
                placeNodeAt(iv, p.getX(), p.getY());
            }
        }

        // Sync positions (identity-based)
        for (Piece p : board.pieces) {
            ImageView iv = pieceNodes.get(p);
            if (iv != null) placeNodeAt(iv, p.getX(), p.getY());
        }
    }

    private Image loadImageFor(Piece p) {
        String path = p.getImagePath(THEME);
        InputStream resource = getClass().getResourceAsStream(path);
        if (resource == null) {
            System.err.println("Missing piece image: " + path);
            return missingPiecePlaceholder();
        }
        return new Image(resource);
    }

    private Image missingPiecePlaceholder() {
        WritableImage img;
        img = new WritableImage((int)(TILE * 0.9), (int)(TILE * 0.9));
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.getPixelWriter().setArgb(x, y, 0xFFCCCCCC); // light gray opaque
            }
        }
        return img;
    } 

    private void onSquareClick(int x, int y) {
        clearHighlights();

        if (selected == null) {
            Piece p = board.getPieceAt(x, y);
            if (p == null) return;
            selected = new int[] { x, y };
            highlightSelection(x, y);
            legalTargets = p.getLegalMoves(board);
            highlightTargets(legalTargets);
            return;
        }

        Piece selPiece = board.getPieceAt(selected[0], selected[1]);
        Piece clicked = board.getPieceAt(x, y);

        if (clicked != null && selPiece != null && sameColor(clicked, selPiece)) {
            selected = new int[] { x, y };
            highlightSelection(x, y);
            legalTargets = clicked.getLegalMoves(board);
            highlightTargets(legalTargets);
            return;
        }

        if (selPiece == null) { selected = null; return; }

        String fromAlg = board.toAlg(selected[0], selected[1]);
        String toAlg = board.toAlg(x, y);

        if (!legalTargets.contains(toAlg)) {
            selected = null;
            return;
        }

        // Pre-move: compute nodes we might animate/fade, but do NOT mutate the model
        int fromX = selected[0], fromY = selected[1];
        ImageView capturedNode = null;

        // Regular capture node (piece at destination prior to move)
        Piece preCaptured = board.getPieceAt(x, y);

        // En passant capture node (destination empty, pawn moves diagonally)
        boolean isPawn = selPiece.getClass().getSimpleName().equals("Pawn");
        boolean epAttempt = isPawn && fromX != x && (preCaptured == null);
        Piece epCaptured = epAttempt ? board.getPieceAt(x, fromY) : null;

        if (preCaptured != null) {
            capturedNode = pieceNodes.get(preCaptured);
        } else if (epCaptured != null) {
            capturedNode = pieceNodes.get(epCaptured);
        }

        try {
            board.move(fromAlg, toAlg);
            lastFrom = new int[]{fromX, fromY};
            lastTo = new int[]{x, y};

            // Animate the piece from its original square to the destination
            animateMove(selPiece, fromX, fromY, x, y, capturedNode);

            // If we faded a captured node, drop it from the map now
            if (preCaptured != null && !board.pieces.contains(preCaptured)) {
                pieceNodes.remove(preCaptured);
            } else if (epCaptured != null && !board.pieces.contains(epCaptured)) {
                pieceNodes.remove(epCaptured);
            }

            // Record move and refresh panel
            String ply = fromAlg + "-" + toAlg;
            moveHistory.add(ply);
            infoPanel.refresh(board, moveHistory);
        } catch (Exception ex) {
            // Move failed; do nothing to the model or nodes, and optionally log ex
            System.err.println("Move rejected: " + ex.getMessage());
        } finally {
            selected = null;
            legalTargets.clear();
            highlightLastMove(lastFrom, lastTo);
        }
    }

    private void animateMove(Piece p, int fromX, int fromY, int toX, int toY, ImageView capturedNode) {
        ImageView iv = pieceNodes.get(p);
        if (iv == null) return;

        double fromLayoutX = snapX(fromX);
        double fromLayoutY = snapY(fromY);
        double toLayoutX = snapX(toX);
        double toLayoutY = snapY(toY);

        Timeline tl = new Timeline();
        int time = 200;

        // Fade captured AFTER a successful move (we call this only post-move)
        if (capturedNode != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(120), capturedNode);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> pieceLayer.getChildren().remove(capturedNode));
            fade.play();
        }

        iv.toFront();
        tl.getKeyFrames().addAll(
            new KeyFrame(Duration.ZERO,
                new KeyValue(iv.layoutXProperty(), fromLayoutX),
                new KeyValue(iv.layoutYProperty(), fromLayoutY)
            ),
            new KeyFrame(Duration.millis(time),
                new KeyValue(iv.layoutXProperty(), toLayoutX),
                new KeyValue(iv.layoutYProperty(), toLayoutY)
            )
        );
        tl.setOnFinished(e -> {
            placeNodeAt(iv, toX, toY);
            refreshPieces(); // sync other state (e.g., rook after castling, EP removal)
        });
        tl.play();
    }

    private double snapX(int x) { return x * TILE + (TILE - TILE * 0.9) / 2.0; }
    private double snapY(int y) { return y * TILE + (TILE - TILE * 0.9) / 2.0; }

    private void placeNodeAt(ImageView iv, int x, int y) {
        iv.setLayoutX(snapX(x));
        iv.setLayoutY(snapY(y));
    }

    private void clearHighlights() {
        highlightLayer.getChildren().clear();
        // Repaint base squares (keep last move shading)
        repaintBoard();
        if (lastFrom != null && lastTo != null) {
            shadeSquare(lastFrom[0], lastFrom[1], lastMoveColor);
            shadeSquare(lastTo[0], lastTo[1], lastMoveColor);
        }
    }

    private void highlightSelection(int x, int y) {
        shadeSquare(x, y, selectColor);
    }

    private void highlightLastMove(int[] from, int[] to) {
        if (from == null || to == null) return;
        shadeSquare(from[0], from[1], lastMoveColor);
        shadeSquare(to[0], to[1], lastMoveColor);
    }

    private void shadeSquare(int x, int y, Paint color) {
        Rectangle r = new Rectangle(x * TILE, y * TILE, TILE, TILE);
        r.setFill(color);
        highlightLayer.getChildren().add(r);
    }

    private void highlightTargets(Set<String> targets) {
        for (String alg : targets) {
            int[] xy = board.fromAlg(alg);
            double cx = xy[0] * TILE + TILE / 2.0;
            double cy = xy[1] * TILE + TILE / 2.0;
            Circle dot = new Circle(cx, cy, TILE * 0.16);
            dot.setFill(targetDotColor);
            highlightLayer.getChildren().add(dot);
        }
    }

    private boolean sameColor(Piece a, Piece b) {
        return a.getColor() != null && b.getColor() != null && a.getColor().getCode() == b.getColor().getCode();
    }
}