package com.predixcode.core.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Pawn;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.board.pieces.Rook;
import com.predixcode.core.colors.Color;
import com.predixcode.core.fen.FenAdapter;
import com.predixcode.core.rules.MoveContext;
import com.predixcode.core.rules.Rule;
import com.predixcode.core.rules.RuleBuilder;

/**
 * Pure board model: no UI state, no click handling.
 * Responsibilities:
 *  - Board state (dimensions, pieces, active color, clocks, en passant target).
 *  - Applying moves via rules (validate, hooks, core move).
 *  - Core helpers for rules (captures, en passant, castling, castling rights).
 *  - Attack detection and check/checkmate helpers.
 */
public class Board {

    // ---- Core state ----

    private int width;
    private int height;

    private int halfmove;
    private int fullmove;

    /**
     * En passant target square as board coordinates [x, y]; [-1, -1] means "none".
     */
    private final int[] enPassant = new int[] { -1, -1 };

    private Color activeColor;

    private final List<Piece> pieces = new ArrayList<>();
    private final List<Rule> rules = new ArrayList<>();

    // ---- Construction ----

    /**
     * Convenience factory for standard FEN loading.
     */
    public static Board fromFen(String fen) {
        return FenAdapter.boardFromFen(fen);
    }

    // ---- Encapsulation: dimensions & clocks ----

    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    public void setWidth(int width)   { this.width = width; }
    public void setHeight(int height) { this.height = height; }

    public int getHalfmove() { return halfmove; }
    public void setHalfmove(int halfmove) { this.halfmove = halfmove; }
    public void increaseHalfmove() { this.halfmove++; }
    public void resetHalfmove()    { this.halfmove = 0; }

    public int getFullmove() { return fullmove; }
    public void setFullmove(int fullmove) { this.fullmove = fullmove; }
    public void increaseFullmove() { this.fullmove++; }

    // ---- Encapsulation: side to move ----

    public Color getActiveColor() { return activeColor; }
    public void setActiveColor(Color activeColor) { this.activeColor = activeColor; }

    // ---- Encapsulation: pieces ----

    /**
     * Returns the live list of pieces.
     * Rules and setup code may mutate this list directly.
     */
    public List<Piece> getPieces() { return pieces; }

    public void setPieces(List<Piece> newPieces) {
        pieces.clear();
        if (newPieces != null) pieces.addAll(newPieces);
    }

    // ---- Encapsulation: rules ----

    public List<Rule> getRules() { return rules; }

    public void setRules(List<Rule> newRules) {
        rules.clear();
        if (newRules != null) rules.addAll(newRules);
    }

    /**
     * Ensures a default rule set exists if none has been configured.
     * Mostly a convenience for quick setups.
     */
    public void ensureRules() {
        if (rules.isEmpty()) {
            rules.addAll(RuleBuilder.defaultRules());
        }
    }

    // ---- Encapsulation: en passant target ----

    public void setEnPassant(int[] xy) {
        if (xy == null || xy.length < 2) {
            enPassant[0] = -1;
            enPassant[1] = -1;
        } else {
            enPassant[0] = xy[0];
            enPassant[1] = xy[1];
        }
    }

    /**
     * Returns a copy of the current en passant target [x, y] or [-1, -1] if none.
     */
    public int[] getEnPassantXY() {
        return new int[] { enPassant[0], enPassant[1] };
    }

    public void clearEnPassant() {
        enPassant[0] = -1;
        enPassant[1] = -1;
    }

    // =====================================================================
    //  Move application pipeline
    // =====================================================================
    /**
     * Applies a move and returns a model-only MoveResult
     * (from, to, captured piece).
     * The move pipeline is:
     *  1) validateMove on all rules
     *  2) beforeMove on all rules
     *  3) core move (piece position update)
     *  4) afterMove on all rules
     *  5) afterTurn on all rules
     * Rules and helpers must record captures via {@link MoveContext#setCapturedPiece(Piece)}.
     */
    public MoveResult applyTurnWithResult(String from, String to) {
        int[] fromXY = fromAlg(from);
        int[] toXY   = fromAlg(to);
        if (fromXY == null || toXY == null) {
            throw new IllegalArgumentException("Invalid (null) move coordinates");
        }

        Piece movingPiece = getPieceAt(fromXY[0], fromXY[1]);
        if (movingPiece == null) {
            throw new IllegalArgumentException("No piece at source square: " + from);
        }

        ensureRules();

        MoveContext ctx = new MoveContext(movingPiece, fromXY, toXY);

        // 1) Validation
        for (Rule rule : rules) {
            rule.validateMove(this, ctx);
        }

        // 2) Pre-move hooks
        for (Rule rule : rules) {
            rule.beforeMove(this, ctx);
        }

        // 3) Core move (pure position update)
        performCoreMove(ctx);

        // 4) Post-move hooks
        for (Rule rule : rules) {
            rule.afterMove(this, ctx);
        }

        // 5) End-of-turn hooks
        for (Rule rule : rules) {
            rule.afterTurn(this, ctx);
        }

        Piece captured = ctx.getCapturedPiece();

        return new MoveResult(
            new int[] { fromXY[0], fromXY[1] },
            new int[] { toXY[0], toXY[1] },
            captured
        );
    }

    /**
     * Core move: update the piece's board coordinates.
     * All side effects (captures, EP, castling, clocks) belong to rules.
     */
    private void performCoreMove(MoveContext ctx) {
        ctx.piece.setPosition(ctx.toXY[0], ctx.toXY[1]);
    }

    // =====================================================================
    //  Coordinate system & queries
    // =====================================================================

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isEmpty(int x, int y) {
        return inBounds(x, y) && getPieceAt(x, y) == null;
    }

    public Piece getPieceAt(int x, int y) {
        for (Piece p : pieces) {
            if (p.posX == x && p.posY == y) return p;
        }
        return null;
    }

    /**
     * Converts board coordinates [x,y] to algebraic notation (e.g. "e4").
     * Returns "-" if outside the board.
     */
    public String toAlg(int x, int y) {
        if (!inBounds(x, y)) return "-";
        char file = (char) ('a' + x);
        int rank = height - y;
        return "" + file + rank;
    }

    /**
     * Parses algebraic notation (e.g. "e4") into board coordinates [x,y].
     * "-" is mapped to [-1, -1].
     */
    public int[] fromAlg(String alg) {
        if (alg == null) throw new IllegalArgumentException("Square is null");
        String trimmed = alg.trim();
        if (trimmed.equals("-")) return new int[] { -1, -1 };
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException("Invalid square. Use file+rank like 'a1', 'h8': " + alg);
        }

        // Parse file
        char fileCh = Character.toLowerCase(trimmed.charAt(0));
        if (fileCh < 'a' || fileCh >= ('a' + width)) {
            throw new IllegalArgumentException("File out of range for board: " + alg);
        }
        int file = fileCh - 'a';

        // Parse rank
        String rankStr = trimmed.substring(1);
        int rank;
        try {
            rank = Integer.parseInt(rankStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Rank must be a number: " + alg, e);
        }
        if (rank < 1 || rank > height) {
            throw new IllegalArgumentException("Rank out of range for board: " + alg);
        }
        int y = height - rank;
        return new int[] { file, y };
    }

    /**
     * Returns movement direction for the given color:
     * WHITE moves "up" (-1), BLACK moves "down" (+1).
     */
    public int forwardDir(Color color) {
        return (color != null && color.equals(Color.WHITE)) ? -1 : 1;
    }

    public boolean isSquareOccupied(int[] xy) { return getPieceAt(xy[0], xy[1]) != null; }

    // =====================================================================
    //  Legal move computation (self-check filtering)
    // =====================================================================

    /**
     * Compute legal targets for a given piece, filtering out moves
     * that would leave its own king in check.
     * This is pure model logic: no UI state is stored.
     */
    public Set<String> computeLegalTargets(Piece p) {
        Set<String> raw = p.getLegalMoves(this);
        if (raw == null || raw.isEmpty()) return Set.of();

        Set<String> filtered = new LinkedHashSet<>();
        int[] from = p.getXY();
        for (String alg : raw) {
            int[] to = fromAlg(alg);
            if (!wouldLeaveOwnKingInCheck(p, from, to)) {
                filtered.add(alg.toLowerCase());
            }
        }
        return filtered;
    }

    /**
     * Simulates the move (including captures and castling movement) and
     * reports whether the mover's own king would be in check after it.
     */
    public boolean wouldLeaveOwnKingInCheck(Piece movingPiece, int[] fromXY, int[] toXY) {
        // Save state needed to revert
        int oldX = movingPiece.posX;
        int oldY = movingPiece.posY;

        Piece captured = null;
        int dir = forwardDir(movingPiece.getColor());

        // Handle en passant capture for simulation
        if (movingPiece instanceof Pawn
                && fromXY[0] != toXY[0]
                && isEmpty(toXY[0], toXY[1])) {
            Piece epPawn = getPieceAt(toXY[0], toXY[1] - dir);
            if (epPawn instanceof Pawn && !epPawn.getColor().equals(movingPiece.getColor())) {
                captured = epPawn;
                pieces.remove(captured);
            }
        } else {
            // Normal capture at destination square
            captured = getPieceAt(toXY[0], toXY[1]);
            if (captured != null) pieces.remove(captured);
        }

        // Handle castling rook move (simulate rook motion)
        Piece rookMoved = null;
        int rookOldX = 0, rookOldY = 0;
        boolean castling = (movingPiece instanceof King) && Math.abs(toXY[0] - fromXY[0]) == 2;
        if (castling) {
            int rankY = fromXY[1];
            if (toXY[0] > fromXY[0]) {
                // King-side
                Piece rook = findFirstRookOnRay(fromXY[0], rankY, +1, 0, movingPiece.getColor());
                if (rook instanceof Rook) {
                    rookMoved = rook;
                    rookOldX = rook.posX; rookOldY = rook.posY;
                    rook.setPosition(fromXY[0] + 1, rankY);
                }
            } else {
                // Queen-side
                Piece rook = findFirstRookOnRay(fromXY[0], rankY, -1, 0, movingPiece.getColor());
                if (rook instanceof Rook) {
                    rookMoved = rook;
                    rookOldX = rook.posX; rookOldY = rook.posY;
                    rook.setPosition(fromXY[0] - 1, rankY);
                }
            }
        }

        // Make the move
        movingPiece.setPosition(toXY[0], toXY[1]);

        // Evaluate check on own king
        King myKing = getKing(movingPiece.getColor());
        int[] kingXY = (myKing != null) ? myKing.getXY() : null;
        boolean inCheck = (kingXY != null) &&
                isSquareAttacked(movingPiece.getColor().opposite(), kingXY[0], kingXY[1]);

        // Revert move
        movingPiece.setPosition(oldX, oldY);
        if (captured != null) {
            pieces.add(captured);
        }
        if (rookMoved != null) {
            rookMoved.setPosition(rookOldX, rookOldY);
        }

        return inCheck;
    }

    // =====================================================================
    //  Attack detection & king helpers
    // =====================================================================

    /**
     * Returns true if any piece of the given color attacks square (x,y).
     */
    public boolean isSquareAttacked(Color byColor, int x, int y) {
        for (Piece p : pieces) {
            if (p.getColor().equals(byColor)) {
                for (int[] sq : p.attackedSquares(this)) {
                    if (sq[0] == x && sq[1] == y) return true;
                }
            }
        }
        return false;
    }

    /**
     * Walks a ray (sx,sy) + n*(dx,dy) and returns the first rook of the given color if any.
     */
    public Piece findFirstRookOnRay(int sx, int sy, int dx, int dy, Color color) {
        int x = sx + dx, y = sy + dy;
        while (inBounds(x, y)) {
            Piece at = getPieceAt(x, y);
            if (at != null) {
                if (at instanceof Rook && at.getColor().equals(color)) return at;
                return null;
            }
            x += dx; y += dy;
        }
        return null;
    }

    /**
     * Finds the king of the given color if present.
     */
    public King getKing(Color color) {
        for (Piece p : pieces) {
            if (p instanceof King && p.getColor() != null && p.getColor().equals(color)) {
                return (King) p;
            }
        }
        return null;
    }

    // =====================================================================
    //  Game state evaluation (check, mate, any-legal-moves)
    // =====================================================================

    public boolean isInCheck(Color color) {
        King k = getKing(color);
        int[] kingXY = (k != null) ? k.getXY() : null;
        if (kingXY == null) return false; // No king found; treat as not in check.
        return isSquareAttacked(color.opposite(), kingXY[0], kingXY[1]);
    }

    public boolean hasNoLegalMoves(Color color) {
        for (Piece p : pieces) {
            if (p.getColor() == null || !p.getColor().equals(color)) continue;
            Set<String> moves = p.getLegalMoves(this);
            if (moves == null || moves.isEmpty()) continue;

            for (String alg : moves) {
                int[] toXY = fromAlg(alg);
                if (!wouldLeaveOwnKingInCheck(p, p.getXY(), toXY)) {
                    return false;
                }
            }
        }
        return true;
    }

    // =====================================================================
    //  Helpers for rules (captures, EP, castling, rights)
    // =====================================================================

    /**
     * Returns true if the move from->to is a king castling move (two-file horizontal jump).
     */
    public boolean isCastlingMove(int[] fromXY, int[] toXY) {
        return Math.abs(toXY[0] - fromXY[0]) == 2;
    }

    /**
     * Performs en passant if the move is a diagonal pawn move into an empty square.
     * Returns true if an en passant capture was performed.
     * Rules should call this from their afterMove/beforeMove hooks.
     */
    public boolean performEnPassantIfApplicable(Piece movingPiece, int[] fromXY, int[] toXY, MoveContext ctx) {
        boolean isPawn = movingPiece instanceof Pawn;
        if (!isPawn) return false;

        // Diagonal into empty square => possible EP
        if (fromXY[0] != toXY[0] && isEmpty(toXY[0], toXY[1])) {
            int dir = forwardDir(movingPiece.getColor());
            Piece epPawn = getPieceAt(toXY[0], toXY[1] - dir);
            if (!(epPawn instanceof Pawn) || epPawn.getColor().equals(movingPiece.getColor())) {
                throw new IllegalStateException("Invalid en passant capture attempted");
            }
            pieces.remove(epPawn);
            ctx.setCapturedPiece(epPawn);
            return true;
        }
        return false;
    }

    /**
     * Handles castling (king moves two squares horizontally), moves the rook,
     * disables king castling rights and clears en passant target.
     * Rules should call this if they detect a castling move.
     */
    public void handleCastling(King king, int[] fromXY, int[] toXY) {
        int rankY = fromXY[1];
        if (toXY[0] > fromXY[0]) {
            // King-side castle: move rook from nearest right rook to f-file (x = fromX + 1)
            Piece rook = findFirstRookOnRay(fromXY[0], rankY, +1, 0, king.getColor());
            if (!(rook instanceof Rook)) {
                throw new IllegalStateException("No rook found for king-side castling");
            }
            int rookToX = fromXY[0] + 1;
            rook.setPosition(rookToX, rankY);
        } else {
            // Queen-side castle: move rook from nearest left rook to d-file (x = fromX - 1)
            Piece rook = findFirstRookOnRay(fromXY[0], rankY, -1, 0, king.getColor());
            if (!(rook instanceof Rook)) {
                throw new IllegalStateException("No rook found for queen-side castling");
            }
            int rookToX = fromXY[0] - 1;
            rook.setPosition(rookToX, rankY);
        }

        // King loses castling rights and EP is cleared on a castle
        king.setCastleKingSide(false);
        king.setCastleQueenSide(false);
        clearEnPassant();
    }

    public void updateCastlingRights(Piece mover,
                                    int fromX, int fromY,
                                    int toX,   int toY,
                                    boolean isCapture) {
        // If king moved, clear its rights
        if (mover instanceof King king) {
            king.setCastleKingSide(false);
            king.setCastleQueenSide(false);
        }

        // If rook moved from initial squares, clear that side for that color
        if (mover instanceof Rook) {
            if (mover.getColor().equals(Color.WHITE)) {
                King wk = getKing(Color.WHITE);
                if (wk != null) {
                    if (fromX == 0 && fromY == 7) wk.setCastleQueenSide(false); // a1 rook moved
                    if (fromX == 7 && fromY == 7) wk.setCastleKingSide(false);  // h1 rook moved
                }
            } else if (mover.getColor().equals(Color.BLACK)) {
                King bk = getKing(Color.BLACK);
                if (bk != null) {
                    if (fromX == 0 && fromY == 0) bk.setCastleQueenSide(false); // a8 rook moved
                    if (fromX == 7 && fromY == 0) bk.setCastleKingSide(false);  // h8 rook moved
                }
            }
        }

        // If a rook was captured on its original square, clear that side for that color
        if (isCapture) {
            King wk = getKing(Color.WHITE);
            King bk = getKing(Color.BLACK);
            if (toX == 0 && toY == 7 && wk != null) wk.setCastleQueenSide(false);
            if (toX == 7 && toY == 7 && wk != null) wk.setCastleKingSide(false);
            if (toX == 0 && toY == 0 && bk != null) bk.setCastleQueenSide(false);
            if (toX == 7 && toY == 0 && bk != null) bk.setCastleKingSide(false);
        }
    }

    /**
     * Handles standard capture if there is a piece at the destination square.
     * Rules should call this from afterMove or similar.
     */
    public void handleCaptureIfAny(int[] toXY, MoveContext ctx) {
        Piece captured = null;

        // Prefer a piece of the opposite color to the mover (activeColor)
        for (Piece p : pieces) {
            if (p.posX == toXY[0] && p.posY == toXY[1]) {
                if (!p.getColor().equals(activeColor)) {
                    captured = p;
                    break;
                }
            }
        }

        if (captured != null) {
            pieces.remove(captured);
            ctx.setCapturedPiece(captured);
        }
    }

    /**
     * Updates the en passant target square if the moving piece is a pawn
     * that has just moved two squares.
     */
    public void updateEnPassantTargetIfApplicable(Piece movingPiece, int[] fromXY, int[] toXY) {
        clearEnPassant();
        if (movingPiece instanceof Pawn && Math.abs(toXY[1] - fromXY[1]) == 2) {
            int dir = forwardDir(movingPiece.getColor());
            int epX = fromXY[0];
            int epY = fromXY[1] + dir; // mid square between start and end
            setEnPassant(new int[] { epX, epY });
        }
    }

    // =====================================================================
    //  Debug / textual representation
    // =====================================================================

    @Override
    public String toString() {
        // Use configured dimensions
        char[][] grid = new char[height][width];
        for (int r = 0; r < height; r++) Arrays.fill(grid[r], '.');

        for (Piece p : pieces) {
            int x = p.posX;
            int y = p.posY;
            if (x >= 0 && x < width && y >= 0 && y < height) {
                grid[y][x] = p.getSymbol().charAt(0);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("  +-----------------+\n");
        for (int row = 0; row < height; row++) {
            int rank = height - row;
            sb.append(rank).append(" | ");
            for (int col = 0; col < width; col++) {
                sb.append(grid[row][col]).append(' ');
            }
            sb.append("|\n");
        }
        sb.append("  +-----------------+\n");
        sb.append("    a b c d e f g h\n\n");

        sb.append("Active: ").append(activeColor != null ? activeColor : "unknown").append('\n');
        sb.append("Castling: ").append(FenAdapter.getCastlingString(this)).append('\n');
        sb.append("En Passant: ").append(FenAdapter.getEnPassantString(this)).append('\n');
        sb.append("Halfmove: ").append(halfmove).append('\n');
        sb.append("Fullmove: ").append(fullmove).append('\n');

        return sb.toString();
    }
}