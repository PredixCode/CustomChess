package com.predixcode.desktop.ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.MoveResult;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.ui.BoardController;
import com.predixcode.core.ui.BoardViewState;
import com.predixcode.core.ui.ClickOutcome;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX GUI for the chess board, using BoardController + BoardViewState.
 * All click / selection / move-history logic lives in BoardController.
 * This class is only responsible for visual rendering and animations.
 */
public class GameScreen extends Application {

    private static final double TILE = 88; // tile size in px
    private static final double PADDING = 24; // space for coordinates
    private static final String THEME = "neo/upscale"; // folder under /pieces/

    private Board board;
    private BoardController boardController;
    private BoardViewState viewState;

    private Rectangle[][] squares;
    private Scene gameScene;      // the active game scene
    private final Group highlightLayer = new Group();
    private final Group pieceLayer = new Group();
    private final Group overlayLayer = new Group();
    private final Map<Piece, ImageView> pieceNodes = new HashMap<>();

    private Runnable backToMenuHandler;

    private static final Paint BOARD_LIGHT = Paint.valueOf("#ececd0");
    private static final Paint BOARD_DARK  = Paint.valueOf("#749552");
    private final Paint selectColor = Paint.valueOf("#F6F66980"); // translucent yellow
    private final Paint lastMoveColor = Paint.valueOf("#f6f6693b");
    private final Paint targetDotColor = Paint.valueOf("#6161612a");

    private GameInfoPanel infoPanel;

    public GameScreen(BoardController controller) {
        this.boardController = controller;
        this.board = controller.getBoard();
    }

    @Override
    public void start(Stage stage) throws Exception {
        if (this.board == null) {
            throw new IllegalStateException("Board not initialized before GUI start");
        }
        this.viewState = boardController.getViewState();

        initGui(stage);
    }

    protected void initGui(Stage stage) {
        this.squares = new Rectangle[board.getHeight()][board.getWidth()];
        final double baseW = PADDING + board.getWidth() * TILE + PADDING;
        final double baseH = PADDING + board.getHeight() * TILE + PADDING;

        // Center area: scalable board content
        StackPane center = new StackPane();
        Group content = new Group();

        Group boardGroup = new Group();
        boardGroup.setLayoutX(PADDING);
        boardGroup.setLayoutY(PADDING);

        // Add layers
        boardGroup.getChildren().add(buildSquares());
        boardGroup.getChildren().add(highlightLayer);
        boardGroup.getChildren().add(pieceLayer);
        boardGroup.getChildren().add(overlayLayer);
        highlightLayer.setMouseTransparent(true);
        overlayLayer.setMouseTransparent(true);

        content.getChildren().add(boardGroup);
        content.getChildren().add(buildCoordinates());
        center.getChildren().add(content);

        // Right info panel
        double panelWidth = 400;
        infoPanel = new GameInfoPanel(panelWidth);
        BorderPane root = new BorderPane();
        root.setCenter(center);
        root.setRight(infoPanel);

        // Top bar with "Go Back" button
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8));
        Button backBtn = new Button("← Menu");
        backBtn.setOnAction(e -> goBackToMenu());
        backBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #ececec; -fx-border-color: #c7c7c7;");
        topBar.getChildren().add(backBtn);
        root.setTop(topBar);

        // Build scene
        Scene scene = new Scene(root, baseW + panelWidth, baseH);
        this.gameScene = scene;
        stage.setTitle("Custom Chess");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

        // Scale the board content to fit center area
        DoubleBinding scale = Bindings.createDoubleBinding(
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
        infoPanel.refresh(board, viewState.getMoveHistory());
        redrawHighlights();
    }

    protected void onSquareClick(int x, int y) {
        // 1) Let controller update its internal state and return event for animation
        ClickOutcome event = boardController.handleClick(x, y);
        // 2) Refresh viewState snapshot
        this.viewState = boardController.getViewState();

        // 3) Redraw highlights based on viewState (selection, targets, last move)
        redrawHighlights();

        // 4) Handle move animation & info panel updates on MOVE_APPLIED
        if (event.type == ClickOutcome.Type.MOVE_APPLIED && event.moveResult != null) {
            MoveResult mr = event.moveResult;
            int[] from = mr.getFrom();
            int[] to   = mr.getTo();

            if (from != null && to != null) {
                Piece moved = board.getPieceAt(to[0], to[1]);
                Piece captured = mr.getCaptured();

                boolean capturedStillExists = (captured != null) && board.getPieces().contains(captured);
                ImageView capturedNode = (!capturedStillExists && captured != null)
                        ? pieceNodes.get(captured)
                        : null;

                animateMove(moved, from[0], from[1], to[0], to[1], capturedNode);

                if (captured != null && !capturedStillExists) {
                    ImageView removed = pieceNodes.remove(captured);
                    if (removed != null) pieceLayer.getChildren().remove(removed);
                }
            }

            infoPanel.refresh(board, viewState.getMoveHistory());
        }

        // You could also use viewState.getLastError() for a toast / dialog if MOVE_REJECTED.
    }

    private void goBackToMenu() {
        if (backToMenuHandler != null) {
            backToMenuHandler.run();
        }
    }

    private Pane buildSquares() {
        Pane pane = new Pane();

        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                final int fx = x;
                final int fy = y;

                Rectangle r = new Rectangle(x * TILE, y * TILE, TILE, TILE);
                r.setFill(getSquareBaseFill(x, y));
                r.setPickOnBounds(true);
                r.setOnMouseClicked(evt -> onSquareClick(fx, fy));

                squares[y][x] = r;
                pane.getChildren().add(r);
            }
        }
        return pane;
    }

    private void repaintBoard() {
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                squares[y][x].setFill(getSquareBaseFill(x, y));
            }
        }
    }

    private Paint getSquareBaseFill(int x, int y) {
        // Standard orientation: a1 (x=0,y=height-1) is dark => (x+y) odd => dark
        return ((x + y) % 2 == 0) ? BOARD_LIGHT : BOARD_DARK;
    }

    private Pane buildCoordinates() {
        Pane coords = new Pane();
        coords.setMouseTransparent(true);
        coords.setPrefSize(PADDING + board.getWidth() * TILE + PADDING, PADDING + board.getHeight() * TILE + PADDING);

        // Files a-h (bottom)
        for (int x = 0; x < board.getWidth(); x++) {
            char file = (char) ('a' + x);
            Text t = text(String.valueOf(file), 12);
            t.setX(PADDING + x * TILE + TILE - 14);
            t.setY(PADDING + board.getHeight() * TILE + 16);
            t.setFill(Paint.valueOf("#333"));
            coords.getChildren().add(t);
        }
        // Ranks 8-1 (left)
        for (int y = 0; y < board.getHeight(); y++) {
            int rank = board.getHeight() - y;
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
            ds.setColor(Color.web("#00000040"));
            iv.setEffect(ds);

            int x = p.posX;
            int y = p.posY;
            placeNodeAt(iv, x, y);

            iv.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    onSquareClick(p.posX, p.posY);
                }
            });
            pieceNodes.put(p, iv);
            pieceLayer.getChildren().add(iv);
        }
    }

    private void refreshPieces() {
        // Build an identity-based membership of current pieces
        IdentityHashMap<Piece, Boolean> currentMap = new IdentityHashMap<>();
        for (Piece p : board.getPieces()) currentMap.put(p, Boolean.TRUE);

        // Remove nodes for pieces that no longer exist (identity-based)
        List<Piece> toRemove = new ArrayList<>();
        for (Piece p : pieceNodes.keySet()) {
            if (!currentMap.containsKey(p)) toRemove.add(p);
        }
        for (Piece p : toRemove) {
            ImageView iv = pieceNodes.remove(p);
            if (iv != null) pieceLayer.getChildren().remove(iv);
        }
        // Add nodes for new pieces (identity-based)
        for (Piece p : board.getPieces()) {
            if (!pieceNodes.containsKey(p)) {
                ImageView iv = new ImageView(loadImageFor(p));
                iv.getProperties().put("imgKey", p.getImagePath(THEME));
                iv.setFitWidth(TILE * 0.9);
                iv.setFitHeight(TILE * 0.9);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                DropShadow ds = new DropShadow();
                ds.setRadius(8);
                ds.setColor(Color.web("#00000040"));
                iv.setEffect(ds);
                iv.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY) onSquareClick(p.posX, p.posY);
                });
                pieceNodes.put(p, iv);
                pieceLayer.getChildren().add(iv);
                placeNodeAt(iv, p.posX, p.posY);
            }
        }
        // Sync positions (identity-based)
        for (Piece p : board.getPieces()) {
            ImageView iv = pieceNodes.get(p);
            if (iv != null) {
                ensureSpriteUpToDate(p, iv);
                placeNodeAt(iv, p.posX, p.posY);
                if (!pieceLayer.getChildren().contains(iv)) {
                    pieceLayer.getChildren().add(iv);
                }
            }
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
        WritableImage img = new WritableImage((int)(TILE * 0.9), (int)(TILE * 0.9));
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.getPixelWriter().setArgb(x, y, 0xFFCCCCCC); // light gray opaque
            }
        }
        return img;
    }

    private void ensureSpriteUpToDate(Piece p, ImageView iv) {
        String wanted = p.getImagePath(THEME);
        Object cur = iv.getProperties().get("imgKey");
        if (!wanted.equals(cur)) {
            iv.setImage(loadImageFor(p));
            iv.getProperties().put("imgKey", wanted);
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

        // Fade captured AFTER a successful move
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

    public Scene getGameScene() {
        return gameScene;
    }

    public void setBackToMenuHandler(Runnable handler) {
        this.backToMenuHandler = handler;
    }

    private double snapX(int x) { return x * TILE + (TILE - TILE * 0.9) / 2.0; }
    private double snapY(int y) { return y * TILE + (TILE - TILE * 0.9) / 2.0; }

    private void placeNodeAt(ImageView iv, int x, int y) {
        iv.setLayoutX(snapX(x));
        iv.setLayoutY(snapY(y));
    }

    /**
     * Clears and redraws highlights based on the current viewState.
     */
    private void redrawHighlights() {
        highlightLayer.getChildren().clear();
        repaintBoard();

        // Last move shading
        int[] from = viewState.getLastFrom();
        int[] to   = viewState.getLastTo();
        if (from != null && to != null) {
            shadeSquare(from[0], from[1], lastMoveColor);
            shadeSquare(to[0],   to[1],   lastMoveColor);
        }

        // Selection
        int[] sel = viewState.getSelectedSquare();
        if (sel != null) {
            shadeSquare(sel[0], sel[1], selectColor);
        }

        // Legal targets
        for (String alg : viewState.getLegalTargets()) {
            int[] xy = board.fromAlg(alg);
            double cx = xy[0] * TILE + TILE / 2.0;
            double cy = xy[1] * TILE + TILE / 2.0;
            Circle dot = new Circle(cx, cy, TILE * 0.16);
            dot.setFill(targetDotColor);
            highlightLayer.getChildren().add(dot);
        }
    }

    private void shadeSquare(int x, int y, Paint color) {
        Rectangle r = new Rectangle(x * TILE, y * TILE, TILE, TILE);
        r.setFill(color);
        highlightLayer.getChildren().add(r);
    }
}