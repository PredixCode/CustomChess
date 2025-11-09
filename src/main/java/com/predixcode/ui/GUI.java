package com.predixcode.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;

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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GUI extends Application {

    private static final int SIZE = 8;
    private static final double TILE = 88; // tile size in px
    private static final double PADDING = 24; // space for coordinates
    private static final String THEME = "merida"; // folder under /pieces/

    private Board board;
    private final Rectangle[][] squares = new Rectangle[SIZE][SIZE];
    private final Group highlightLayer = new Group();
    private final Group pieceLayer = new Group();
    private final Group overlayLayer = new Group();
    private final Map<Piece, ImageView> pieceNodes = new HashMap<>();

    // Selection and last move tracking
    private int[] selected = null; // [x, y] board coords
    private Set<String> legalTargets = new HashSet<>();
    private int[] lastFrom = null, lastTo = null;

    private final Paint lightColor = Paint.valueOf("#F0D9B5"); // wood-light
    private final Paint darkColor = Paint.valueOf("#B58863");  // wood-dark
    private final Paint selectColor = Paint.valueOf("#F6F66980"); // translucent yellow
    private final Paint lastMoveColor = Paint.valueOf("#9FC76E80"); // translucent green
    private final Paint targetDotColor = Paint.valueOf("#3A7EFDCC");

    @Override
    public void start(Stage stage) throws Exception {
        board = Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        Pane root = new Pane();
        root.setPrefSize(PADDING + SIZE * TILE + PADDING, PADDING + SIZE * TILE + PADDING);

        Group boardGroup = new Group();
        boardGroup.setLayoutX(PADDING);
        boardGroup.setLayoutY(PADDING);

        // Stacking order: base squares, last move/selection highlights, move dots, pieces, top overlays
        boardGroup.getChildren().add(buildSquares());
        boardGroup.getChildren().add(highlightLayer);
        boardGroup.getChildren().add(pieceLayer);
        boardGroup.getChildren().add(overlayLayer);

        root.getChildren().add(boardGroup);
        root.getChildren().add(buildCoordinates());

        Scene scene = new Scene(root);
        stage.setTitle("Predix Chess • JavaFX");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        loadPieces();
        refreshPieces();
    }

    private Pane buildSquares() {
        Pane pane = new Pane();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final int fx = x;  // make final copies
                final int fy = y;

                Rectangle r = new Rectangle(x * TILE, y * TILE, TILE, TILE);
                r.setFill(((x + y) % 2 == 0) ? lightColor : darkColor);
                r.setOnMouseClicked(evt -> onSquareClick(fx, fy)); // use fx/fy here
                squares[y][x] = r;
                pane.getChildren().add(r);
            }
        }
        return pane;
    }

    private Pane buildCoordinates() {
        Pane coords = new Pane();
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

        for (Piece p : board.getPieces()) {
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
        // Sync positions and add/remove any nodes if needed
        // Remove nodes for pieces that no longer exist
        Set<Piece> current = new HashSet<>(board.getPieces());
        List<Piece> toRemove = pieceNodes.keySet().stream().filter(p -> !current.contains(p)).collect(Collectors.toList());
        for (Piece p : toRemove) {
            ImageView iv = pieceNodes.remove(p);
            if (iv != null) pieceLayer.getChildren().remove(iv);
        }
        // Add nodes for new pieces (should not happen often)
        for (Piece p : current) {
            if (!pieceNodes.containsKey(p)) {
                ImageView iv = new ImageView(loadImageFor(p));
                iv.setFitWidth(TILE * 0.9);
                iv.setFitHeight(TILE * 0.9);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                DropShadow ds = new DropShadow();
                ds.setRadius(8);
                ds.setColor(javafx.scene.paint.Color.web("#00000040")); // JavaFX Color
                iv.setEffect(ds);
                iv.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY) onSquareClick(p.getX(), p.getY());
                });
                pieceNodes.put(p, iv);
                pieceLayer.getChildren().add(iv);
                placeNodeAt(iv, p.getX(), p.getY());
            }
        }
        // Move nodes to the right coordinates without animation (used on initial load)
        for (Piece p : current) {
            ImageView iv = pieceNodes.get(p);
            if (iv != null) placeNodeAt(iv, p.getX(), p.getY());
        }
    }

    private Image loadImageFor(Piece p) {
        boolean isWhite = p.getColor() != null && p.getColor().getCode() == 1;
        char t = Character.toLowerCase(p.symbol().charAt(0));
        String name = switch (t) {
            case 'k' -> isWhite ? "wK" : "bK";
            case 'q' -> isWhite ? "wQ" : "bQ";
            case 'r' -> isWhite ? "wR" : "bR";
            case 'b' -> isWhite ? "wB" : "bB";
            case 'n' -> isWhite ? "wN" : "bN";
            case 'p' -> isWhite ? "wP" : "bP";
            default -> "wP";
        };
        String path = "/pieces/" + THEME + "/" + name + ".png";
        java.io.InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            // Missing image resource; return a small transparent placeholder so the app can still run.
            return new javafx.scene.image.WritableImage((int) (TILE * 0.9), (int) (TILE * 0.9));
        }
        return new Image(is);
    }

    private void onSquareClick(int x, int y) {
        clearHighlights();

        if (selected == null) {
            Piece p = pieceAt(x, y);
            if (p == null) return;
            selected = new int[] { x, y };
            highlightSelection(x, y);
            legalTargets = computePseudoLegalTargets(p);
            highlightTargets(legalTargets);
        } else {
            // If clicked same color piece, reselect
            Piece clicked = pieceAt(x, y);
            Piece selPiece = pieceAt(selected[0], selected[1]);
            if (clicked != null && selPiece != null &&
                sameColor(clicked, selPiece)) {
                selected = new int[] { x, y };
                highlightSelection(x, y);
                legalTargets = computePseudoLegalTargets(clicked);
                highlightTargets(legalTargets);
                return;
            }

            String fromAlg = toAlg(selected[0], selected[1]);
            String toAlg = toAlg(x, y);

            if (!legalTargets.contains(toAlg)) {
                // Deselect if clicking outside legal targets
                selected = null;
                return;
            }

            // Handle capture visually: remove target piece from model list before move
            Piece captured = pieceAt(x, y);
            ImageView capturedNode = null;
            if (captured != null) {
                capturedNode = pieceNodes.get(captured);
                board.getPieces().remove(captured); // ensure Board doesn't re-add it in refresh
            }

            // Perform engine move (no legality enforcement inside engine)
            try {
                lastFrom = new int[]{selected[0], selected[1]};
                lastTo = new int[]{x, y};
                animateMove(selPiece, x, y, capturedNode);
                board.move(fromAlg, toAlg);
            } catch (Exception ex) {
                // rollback on error: re-insert captured to model (if any)
                if (captured != null && !board.getPieces().contains(captured)) {
                    board.getPieces().add(captured);
                }
            } finally {
                selected = null;
                legalTargets.clear();
                highlightLastMove(lastFrom, lastTo);
            }
        }
    }

    private void animateMove(Piece p, int toX, int toY, ImageView capturedNode) {
        ImageView iv = pieceNodes.get(p);
        if (iv == null) return;

        double fromLayoutX = snapX(p.getX());
        double fromLayoutY = snapY(p.getY());
        double toLayoutX = snapX(toX);
        double toLayoutY = snapY(toY);

        // Capture fade-out first (under 120ms), then move
        Timeline tl = new Timeline();
        int time = 200;

        if (capturedNode != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(120), capturedNode);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> pieceLayer.getChildren().remove(capturedNode));
            fade.play();
        }

        // Animate piece to new square
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
            // Update model coordinates for the node
            placeNodeAt(iv, toX, toY);
            refreshPieces(); // sync in case of other state changes
        });
        tl.play();
    }

    private void placeNodeAt(ImageView iv, int x, int y) {
        iv.setLayoutX(snapX(x));
        iv.setLayoutY(snapY(y));
    }

    private double snapX(int x) { return x * TILE + (TILE - TILE * 0.9) / 2.0; }
    private double snapY(int y) { return y * TILE + (TILE - TILE * 0.9) / 2.0; }

    private void clearHighlights() {
        highlightLayer.getChildren().clear();
        // Repaint base squares (keep last move shading)
        repaintBoard();
        if (lastFrom != null && lastTo != null) {
            shadeSquare(lastFrom[0], lastFrom[1], lastMoveColor);
            shadeSquare(lastTo[0], lastTo[1], lastMoveColor);
        }
    }

    private void repaintBoard() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                squares[y][x].setFill(((x + y) % 2 == 0) ? lightColor : darkColor);
            }
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
            int[] xy = fromAlg(alg);
            double cx = xy[0] * TILE + TILE / 2.0;
            double cy = xy[1] * TILE + TILE / 2.0;
            Circle dot = new Circle(cx, cy, TILE * 0.16);
            dot.setFill(targetDotColor);
            highlightLayer.getChildren().add(dot);
        }
    }

    private Piece pieceAt(int x, int y) {
        for (Piece p : board.getPieces()) {
            if (p.getX() == x && p.getY() == y) return p;
        }
        return null;
    }

    private boolean sameColor(Piece a, Piece b) {
        return a.getColor() != null && b.getColor() != null && a.getColor().getCode() == b.getColor().getCode();
    }

    private String toAlg(int x, int y) {
        char file = (char) ('a' + x);
        int rank = SIZE - y;
        return "" + file + rank;
    }

    private int[] fromAlg(String alg) {
        int x = alg.charAt(0) - 'a';
        int rank = alg.charAt(1) - '0'; // 1..8
        int y = SIZE - rank;
        return new int[]{x, y};
    }

    // Pseudo-legal move generation on the GUI side
    private Set<String> computePseudoLegalTargets(Piece p) {
        Set<String> targets = new LinkedHashSet<>();
        boolean isWhite = p.getColor() != null && p.getColor().getCode() == 1;
        char t = Character.toLowerCase(p.symbol().charAt(0));
        int x = p.getX(), y = p.getY();

        switch (t) {
            case 'n' -> {
                int[][] offs = {{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}};
                for (int[] o : offs) addIfValidTarget(x + o[0], y + o[1], isWhite, targets);
            }
            case 'k' -> {
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    addIfValidTarget(x + dx, y + dy, isWhite, targets);
                }
                // Castling omitted here for brevity
            }
            case 'b' -> slideTargets(x, y, isWhite, targets, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
            case 'r' -> slideTargets(x, y, isWhite, targets, new int[][]{{1,0},{-1,0},{0,1},{0,-1}});
            case 'q' -> slideTargets(x, y, isWhite, targets, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}});
            case 'p' -> {
                int dir = isWhite ? -1 : 1;
                // Single push
                if (empty(x, y + dir)) targets.add(toAlg(x, y + dir));
                // Double push from start rank
                int startRank = isWhite ? 6 : 1;
                if (y == startRank && empty(x, y + dir) && empty(x, y + 2*dir)) {
                    targets.add(toAlg(x, y + 2*dir));
                }
                // Captures
                addIfCapture(x + 1, y + dir, isWhite, targets);
                addIfCapture(x - 1, y + dir, isWhite, targets);
                // En passant omitted for brevity
            }
        }
        return targets;
    }

    private void slideTargets(int x, int y, boolean isWhite, Set<String> out, int[][] dirs) {
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            while (inBounds(nx, ny)) {
                Piece at = pieceAt(nx, ny);
                if (at == null) {
                    out.add(toAlg(nx, ny));
                } else {
                    if (at.getColor() != null && (at.getColor().getCode() != (isWhite ? 1 : 0))) {
                        // Your Color codes: 1 for white, else black
                        out.add(toAlg(nx, ny));
                    } else if (at.getColor() == null) {
                        out.add(toAlg(nx, ny));
                    }
                    break;
                }
                nx += d[0];
                ny += d[1];
            }
        }
    }

    private boolean empty(int x, int y) {
        return inBounds(x, y) && pieceAt(x, y) == null;
    }

    private void addIfValidTarget(int x, int y, boolean isWhite, Set<String> out) {
        if (!inBounds(x, y)) return;
        Piece at = pieceAt(x, y);
        if (at == null) out.add(toAlg(x, y));
        else if (at.getColor() != null && at.getColor().getCode() != (isWhite ? 1 : 0)) out.add(toAlg(x, y));
    }

    private void addIfCapture(int x, int y, boolean isWhite, Set<String> out) {
        if (!inBounds(x, y)) return;
        Piece at = pieceAt(x, y);
        if (at != null && at.getColor() != null && at.getColor().getCode() != (isWhite ? 1 : 0)) {
            out.add(toAlg(x, y));
        }
    }

    private boolean inBounds(int x, int y) { return x >= 0 && x < SIZE && y >= 0 && y < SIZE; }

    public static void main(String[] args) { launch(args); }
}