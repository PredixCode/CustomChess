package com.predixcode.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.predixcode.board.pieces.King;
import com.predixcode.board.pieces.Pawn;
import com.predixcode.board.pieces.Piece;
import com.predixcode.board.pieces.Rook;
import com.predixcode.colors.Color;
import com.predixcode.rules.MoveContext;
import com.predixcode.rules.Rule;
import com.predixcode.rules.RuleBuilder;


public class Board {
    private int width;
    private int height;

    private int halfmove;
    private int fullmove;
    // enPassant as board coordinates [x, y]; -1 means none
    private int[] enPassant = new int[] { -1, -1 };

    private Color activeColor;

    private int[] selectedSquare = null;              // [x,y] selected square
    private Set<String> cachedLegalTargets = Set.of();// filtered UI highlight set
    private int[] lastFromXY = null, lastToXY = null; // last move shading
    private Piece lastCapturedPiece = null;           // who was captured last (incl. EP)

    private final List<Piece> pieces = new ArrayList<>();
    private final List<Rule> rules = new ArrayList<>();

    // Board initialization
    public static Board fromFen(String fen) {
        return FenAdapter.boardFromFen(fen);
    }

    // ---------------- Encapsulation ----------------
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public int getHalfmove() { return halfmove; }
    public void setHalfmove(int halfmove) { this.halfmove = halfmove; }
    public void increaseHalfmove() { this.halfmove++; }
    public void resetHalfmove() { this.halfmove = 0; }

    public int getFullmove() { return fullmove; }
    public void setFullmove(int fullmove) { this.fullmove = fullmove; }
    public void increaseFullmove() { this.fullmove++; }
    public void resetFullmove() { this.fullmove = 1; }

    public Color getActiveColor() { return activeColor; }
    public void setActiveColor(Color activeColor) { this.activeColor = activeColor; }

    public List<Piece> getPieces() { return pieces; }

    public void setPieces(List<Piece> newPieces) {
        pieces.clear();
        if (newPieces != null) pieces.addAll(newPieces);
    }

    public List<Rule> getRules() { return rules; }

    public void addRule(Rule rule) { if (rule != null) rules.add(rule); }

    public void setRules(List<Rule> newRules) {
        rules.clear();
        if (newRules != null) rules.addAll(newRules);
    }

    public void setEnPassant(int[] enPassant) {
        if (enPassant == null || enPassant.length < 2) {
            this.enPassant = new int[] { -1, -1 };
        } else {
            this.enPassant = new int[] { enPassant[0], enPassant[1] };
        }
    }

    public int[] getEnPassantXY() { return new int[] { enPassant[0], enPassant[1] }; }

    public void clearEnPassant() { this.enPassant[0] = -1; this.enPassant[1] = -1; }

    // ---------------- Core flow ----------------
    public void applyTurn(String from, String to) {
        int[] fromXY = fromAlg(from);
        int[] toXY   = fromAlg(to);
        if (fromXY == null || toXY == null)
            throw new IllegalArgumentException("Invalid (null) move coordinates");

        Piece movingPiece = getPieceAt(fromXY[0], fromXY[1]);
        if (movingPiece == null)
            throw new IllegalArgumentException("No piece at source square: " + from);

        ensureRules();

        MoveContext ctx = new MoveContext(movingPiece, fromXY, toXY);

        // 1) Validation first on a clean board
        for (Rule rule : rules) {
            rule.validateMove(this, ctx);
        }

        // 2) Now rules may mutate state in beforeMove (e.g. EP side-effects)
        for (Rule rule : rules) {
            rule.beforeMove(this, ctx);
        }

        // 3) Core move: actually move the piece
        performCoreMove(ctx);

        // 4) Post-move hooks (captures, castling, EP target, etc.)
        for (Rule rule : rules) {
            rule.afterMove(this, ctx);
        }

        // 5) End-of-turn hooks (turn switching, clocks, multi-move, etc.)
        for (Rule rule : rules) {
            rule.afterTurn(this, ctx);
        }
    }

    private void performCoreMove(MoveContext ctx) {
        // Minimal core: simply move the piece.
        // Most capture logic is handled in rules (StandardMoveRule, BureaucratCaptureRule, etc).
        ctx.piece.setPosition(ctx.toXY[0], ctx.toXY[1]);
    }

    public Piece getPieceAt(int x, int y) {
        for (Piece p : pieces) {
            if (p.posX == x && p.posY == y) return p;
        }
        return null;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isEmpty(int x, int y) {
        return inBounds(x, y) && getPieceAt(x, y) == null;
    }

    public void ensureRules() {
        if (rules.isEmpty()) {
            rules.addAll(RuleBuilder.defaultRules());
        }
    }

    public String toAlg(int x, int y) {
        if (!inBounds(x, y)) return "-";
        char file = (char) ('a' + x);
        int rank = height - y;
        return "" + file + rank;
    }

    public int[] fromAlg(String alg) {
        if (alg == null) throw new IllegalArgumentException("Square is null");
        String trimmed = alg.trim();
        if (trimmed.equals("-")) return new int[] { -1, -1 };
        if (trimmed.length() < 2) throw new IllegalArgumentException("Invalid square. Use file+rank like 'a1', 'h8': " + alg);

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

    private Set<String> computeUiLegalTargets(Piece p) {
        Set<String> raw = p.getLegalMoves(this);
        if (raw == null || raw.isEmpty()) return Set.of();

        Set<String> filtered = new LinkedHashSet<>();
        int[] from = p.getXY();
        for (String alg : raw) {
            int[] to = fromAlg(alg);
            // UI only: filter out moves that would leave own king in check
            if (!wouldLeaveOwnKingInCheck(p, from, to)) {
                filtered.add(alg.toLowerCase());
            }
        }
        return filtered;
    }

    // UI related
    public ClickOutcome handleSquareClick(int x, int y) {
        // First click = selection
        if (selectedSquare == null) {
            Piece p = getPieceAt(x, y);
            if (p == null) return ClickOutcome.noop();

            // only allow selecting active side's pieces (keeps UX consistent)
            if (getActiveColor() != null && !getActiveColor().equals(p.getColor())) {
                return ClickOutcome.noop();
            }

            selectedSquare = new int[] { x, y };
            cachedLegalTargets = computeUiLegalTargets(p);
            return ClickOutcome.selection(new int[] { x, y }, cachedLegalTargets);
        }

        // Second click = reselect same-color, or attempt move
        Piece selPiece = getPieceAt(selectedSquare[0], selectedSquare[1]);
        Piece clicked = getPieceAt(x, y);

        if (selPiece == null) {
            selectedSquare = null;
            cachedLegalTargets = Set.of();
            return ClickOutcome.noop();
        }

        // Reselect if clicking same-color piece
        if (clicked != null && clicked.getColor().equals(selPiece.getColor())) {
            selectedSquare = new int[] { x, y };
            cachedLegalTargets = computeUiLegalTargets(clicked);
            return ClickOutcome.selection(new int[] { x, y }, cachedLegalTargets);
        }

        // Attempt to apply the move via rules.
        // IMPORTANT: We do NOT pre-reject based on cachedLegalTargets to avoid interfering with custom rules.
        String fromAlg = toAlg(selectedSquare[0], selectedSquare[1]);
        String toAlg   = toAlg(x, y);
        lastCapturedPiece = null;

        try {
            applyTurn(fromAlg, toAlg); // rules validate and perform the move

            lastFromXY = new int[] { selectedSquare[0], selectedSquare[1] };
            lastToXY   = new int[] { x, y };
            Piece cap  = lastCapturedPiece;

            selectedSquare = null;
            cachedLegalTargets = Set.of();

            return ClickOutcome.moveApplied(lastFromXY, lastToXY, cap);
        } catch (Exception ex) {
            selectedSquare = null;
            cachedLegalTargets = Set.of();
            return ClickOutcome.moveRejected(ex.getMessage());
        }
    }

    // ========== Attack detection and helpers ==========

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

    public void updateCastlingRights(Piece mover, int fromX, int fromY, int toX, int toY, boolean isCapture) {
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
                    if (fromX == 7 && fromY == 7) wk.setCastleKingSide(false);  // h1 rook moved (FIXED)
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

    public King getKing(Color color) {
        for (Piece p : pieces) {
            if (p instanceof King && p.getColor() != null && p.getColor().equals(color)) {
                return (King) p;
            }
        }
        return null;
    }

    // Backwards-compatibility signature kept (delegates to this instance)
    public boolean isCheckmate(Board ignored, Color color) {
        return isCheckmate(color);
    }

    public boolean isCheckmate(Color color) {
        return isInCheck(color) && !hasAnyLegalMove(color);
    }

    public boolean isInCheck(Color color) {
        King k = getKing(color);
        int[] kingXY = (k != null) ? k.getXY() : null;
        if (kingXY == null) return false; // No king found; treat as not in check.
        return isSquareAttacked(color.opposite(), kingXY[0], kingXY[1]);
    }

    public boolean hasAnyLegalMove(Color color) {
        for (Piece p : pieces) {
            if (p.getColor() == null || color == null || !p.getColor().equals(color)) continue;
            Set<String> moves = p.getLegalMoves(this);
            if (moves == null || moves.isEmpty()) continue;

            for (String alg : moves) {
                int[] toXY = fromAlg(alg);
                if (!wouldLeaveOwnKingInCheck(p, p.getXY(), toXY)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean wouldLeaveOwnKingInCheck(Piece movingPiece, int[] fromXY, int[] toXY) {
        // Save state needed to revert
        int oldX = movingPiece.posX;
        int oldY = movingPiece.posY;

        Piece captured = null;
        boolean epCapture = false;
        int dir = forwardDir(movingPiece.getColor());

        // Handle en passant capture for simulation
        if (movingPiece instanceof Pawn
                && fromXY[0] != toXY[0]
                && isEmpty(toXY[0], toXY[1])) {
            Piece epPawn = getPieceAt(toXY[0], toXY[1] - dir);
            if (epPawn != null && epPawn instanceof Pawn && !epPawn.getColor().equals(movingPiece.getColor())) {
                captured = epPawn;
                epCapture = true;
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
        boolean inCheck = (kingXY != null) && isSquareAttacked(movingPiece.getColor().opposite(), kingXY[0], kingXY[1]);

        // Revert move
        movingPiece.setPosition(oldX, oldY);
        if (captured != null) {
            // If EP capture, piece removed was not at destination; just add it back
            pieces.add(captured);
        }
        if (rookMoved != null) {
            rookMoved.setPosition(rookOldX, rookOldY);
        }

        return inCheck;
    }

    /**
     * Performs en passant if the move is a diagonal pawn move into an empty square.
     * Returns true if an en passant capture was performed.
     */
    public boolean performEnPassantIfApplicable(Piece movingPiece, int[] fromXY, int[] toXY) {
        boolean isPawn = movingPiece instanceof Pawn;
        if (!isPawn) return false;

        // Diagonal into empty square => possible EP
        if (fromXY[0] != toXY[0] && isEmpty(toXY[0], toXY[1])) {
            int dir = forwardDir(movingPiece.getColor());
            Piece epPawn = getPieceAt(toXY[0], toXY[1] - dir);
            if (epPawn == null || !(epPawn instanceof Pawn) || epPawn.getColor().equals(movingPiece.getColor())) {
                throw new IllegalStateException("Invalid en passant capture attempted");
            }
            lastCapturedPiece = epPawn;
            pieces.remove(epPawn);
            return true;
        }
        return false;
    }

    /**
     * Handles castling (king moves two squares horizontally), moves the rook,
     * disables king castling rights and clears en passant target.
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
            ((Rook) rook).setPosition(rookToX, rankY);
        } else {
            // Queen-side castle: move rook from nearest left rook to d-file (x = fromX - 1)
            Piece rook = findFirstRookOnRay(fromXY[0], rankY, -1, 0, king.getColor());
            if (!(rook instanceof Rook)) {
                throw new IllegalStateException("No rook found for queen-side castling");
            }
            int rookToX = fromXY[0] - 1;
            ((Rook) rook).setPosition(rookToX, rankY);
        }

        // King loses castling rights and EP is cleared on a castle
        king.setCastleKingSide(false);
        king.setCastleQueenSide(false);
        clearEnPassant();
    }

    /**
     * Handles standard capture if there is a piece at the destination square.
     */
    public void handleCaptureIfAny(int[] toXY) {
        Piece captured = null;

        // Prefer a piece of the opposite color to the mover (activeColor)
        for (Piece p : pieces) {
            if (p.posX == toXY[0] && p.posY == toXY[1]) {
                if (activeColor == null || !p.getColor().equals(activeColor)) {
                    captured = p;
                    break;
                }
            }
        }

        if (captured != null) {
            lastCapturedPiece = captured;
            pieces.remove(captured);
        }
    }

    /**
     * Updates the en passant target square if the moving piece is a pawn
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

    // ================== Utilities ===================

    public boolean isCastlingMove(int[] fromXY, int[] toXY) {
        return Math.abs(toXY[0] - fromXY[0]) == 2;
    }

    public boolean isSquareOccupied(int[] xy) { return getPieceAt(xy[0], xy[1]) != null; }

    public boolean isSquareOccupied(int x, int y) { return getPieceAt(x, y) != null; }

    /**
     * Returns movement direction for the given color:
     * WHITE moves "up" (-1), BLACK moves "down" (+1).
     */
    public int forwardDir(Color color) {
        return (color != null && color.equals(Color.WHITE)) ? -1 : 1;
    }

    @Override
    public String toString() {
        // Use configured dimensions (defaults 8x8)
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