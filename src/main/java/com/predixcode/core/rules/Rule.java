package com.predixcode.core.rules;


import java.util.Set;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Pawn;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.board.pieces.Rook;
import com.predixcode.core.colors.Color;

public abstract class Rule {

    public abstract void applyOnStart(Board board);

    public abstract void applyOnTurn(Board board, Piece movingPiece, int[] fromXY, int[] toXY);

    public void applyOnTurn(Board board, Piece movingPiece, String from, String to) {
        int[] fromXY = board.fromAlg(from);
        int[] toXY   = board.fromAlg(to);
        applyOnTurn(board, movingPiece, fromXY, toXY);
    }

    public void throwIfIllegal(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        if (board.activeColor != null && !movingPiece.getColor().equals(board.activeColor)) {
            throw new IllegalStateException("It is not " + movingPiece.getColor() + "'s turn");
        }

        if (wouldLeaveOwnKingInCheck(board, movingPiece, fromXY, toXY)) {
            throw new IllegalStateException("Illegal move: would leave own king in check");
        }

        String from = board.toAlg(fromXY[0], fromXY[1]); 
        String to = board.toAlg(toXY[0], toXY[1]);
        Set<String> targets = movingPiece.getLegalMoves(board);
        String toAlg = to.toLowerCase();
        if (!targets.contains(toAlg)) {
            throw new IllegalArgumentException("Destination " + to + " is not a legal target for " + from);
        }
    }

    public boolean enPassante(boolean isCapture, Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        boolean enPassanteDone = performEnPassantIfApplicable(board, movingPiece, fromXY, toXY);
        if (enPassanteDone) {
            isCapture = true;
        }
        // Update en passant target (only relevant for a 2-square pawn push)
        updateEnPassantTargetIfApplicable(board, movingPiece, fromXY, toXY);
        return isCapture;
    }

    public void castling(Board board, Piece movingPiece, int[] fromXY, int[] toXY, boolean isCapture) {
        boolean isKing = movingPiece instanceof King;
        if (isKing && isCastlingMove(fromXY, toXY)) {
            handleCastling(board, (King) movingPiece, fromXY, toXY);
            board.updateCastlingRights(movingPiece, fromXY[0], fromXY[1], toXY[0], toXY[1], isCapture);
        } 
    }

    public void nextTurn(Board board, Piece movingPiece, boolean isCapture) {
        updateHalfmove(board, movingPiece, isCapture);
        updateFullMove(board);
        switchPlayer(board);
    }

    // ================== Check / Checkmate helpers ===================

    /**
     * Returns true if the given color's king is currently in check.
     */
    public boolean isInCheck(Board board, Color color) {
        int[] kingXY = board.getKing(color).getXY();
        if (kingXY == null) return false; // No king found; treat as not in check.
        return board.isSquareAttacked(color.opposite(), kingXY[0], kingXY[1]);
    }

    /**
     * Returns true if executing the move (fromXY -> toXY) for movingPiece
     * would leave its own king in check. Simulates the move (including EP and castling)
     * then reverts.
     */
    protected boolean wouldLeaveOwnKingInCheck(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        // Save state needed to revert
        int oldX = movingPiece.x;
        int oldY = movingPiece.y;

        Piece captured = null;
        boolean epCapture = false;
        int dir = forwardDir(movingPiece.getColor());

        // Handle en passant capture for simulation
        if (movingPiece instanceof Pawn
                && fromXY[0] != toXY[0]
                && board.isEmpty(toXY[0], toXY[1])) {
            Piece epPawn = board.getPieceAt(toXY[0], toXY[1] - dir);
            if (epPawn != null && epPawn instanceof Pawn && epPawn.getColor() != movingPiece.getColor()) {
                captured = epPawn;
                epCapture = true;
                board.pieces.remove(captured);
            }
        } else {
            // Normal capture at destination square
            captured = board.getPieceAt(toXY[0], toXY[1]);
            if (captured != null) board.pieces.remove(captured);
        }

        // Handle castling rook move (simulate rook motion)
        Piece rookMoved = null;
        int rookOldX = 0, rookOldY = 0;
        boolean castling = (movingPiece instanceof King) && Math.abs(toXY[0] - fromXY[0]) == 2;
        if (castling) {
            int rankY = fromXY[1];
            if (toXY[0] > fromXY[0]) {
                // King-side
                Piece rook = board.findFirstRookOnRay(fromXY[0], rankY, +1, 0, movingPiece.getColor());
                if (rook instanceof Rook) {
                    rookMoved = rook;
                    rookOldX = rook.x; rookOldY = rook.y;
                    rook.setPosition(fromXY[0] + 1, rankY);
                }
            } else {
                // Queen-side
                Piece rook = board.findFirstRookOnRay(fromXY[0], rankY, -1, 0, movingPiece.getColor());
                if (rook instanceof Rook) {
                    rookMoved = rook;
                    rookOldX = rook.x; rookOldY = rook.y;
                    rook.setPosition(fromXY[0] - 1, rankY);
                }
            }
        }

        // Make the move
        movingPiece.setPosition(toXY[0], toXY[1]);

        // Evaluate check on own king
        int[] kingXY = board.getKing(movingPiece.getColor()).getXY();
        boolean inCheck = (kingXY != null) && board.isSquareAttacked(movingPiece.getColor().opposite(), kingXY[0], kingXY[1]);

        // Revert move
        movingPiece.setPosition(oldX, oldY);
        if (captured != null) {
            // If EP capture, piece removed was not at destination; just add it back
            if (epCapture) {
                board.pieces.add(captured);
            } else {
                board.pieces.add(captured);
            }
        }
        if (rookMoved != null) {
            rookMoved.setPosition(rookOldX, rookOldY);
        }

        return inCheck;
    }

    /**
     * Returns true if the given color has at least one legal move
     * that does not leave its own king in check.
     */
    public boolean hasAnyLegalMove(Board board, Color color) {
        for (Piece p : board.pieces) {
            if (p.getColor() != color) continue;
            Set<String> moves = p.getLegalMoves(board);
            if (moves == null || moves.isEmpty()) continue;

            for (String alg : moves) {
                int[] toXY = board.fromAlg(alg);
                if (!wouldLeaveOwnKingInCheck(board, p, p.getXY(), toXY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the given color is currently checkmated.
     * i.e., in check and has no legal moves that resolve it.
     */
    public boolean isCheckmate(Board board, Color color) {
        return isInCheck(board, color) && !hasAnyLegalMove(board, color);
    }

    /**
     * Performs en passant if the move is a diagonal pawn move into an empty square.
     * Returns true if an en passant capture was performed.
     */
    protected boolean performEnPassantIfApplicable(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        boolean isPawn = movingPiece instanceof Pawn;
        if (!isPawn) return false;
        
        // Diagonal move into an empty square => potential en passant
        if (fromXY[0] != toXY[0] && board.isEmpty(toXY[0], toXY[1])) {
            int dir = forwardDir(movingPiece.getColor());
            Piece epPawn = board.getPieceAt(toXY[0], toXY[1] - dir);
            if (epPawn == null || !(epPawn instanceof Pawn) || epPawn.getColor() == movingPiece.getColor()) {
                throw new IllegalStateException("Invalid en passant capture attempted");
            }
            board.pieces.remove(epPawn);
            return true;
        }
        return false;
    }

    /**
     * Handles castling (king moves two squares horizontally), moves the rook,
     * disables king castling rights and clears en passant target.
     */
    protected void handleCastling(Board board, King king, int[] fromXY, int[] toXY) {
        int rankY = fromXY[1];
        if (toXY[0] > fromXY[0]) {
            // King-side castle: move rook from nearest right rook to f-file (x = fromX + 1)
            Piece rook = board.findFirstRookOnRay(fromXY[0], rankY, +1, 0, king.getColor());
            if (!(rook instanceof Rook)) {
                throw new IllegalStateException("No rook found for king-side castling");
            }
            int rookToX = fromXY[0] + 1;
            board.pieces.remove(rook);
            rook.setPosition(rookToX, rankY);
            board.pieces.add(rook);
        } else {
            // Queen-side castle: move rook from nearest left rook to d-file (x = fromX - 1)
            Piece rook = board.findFirstRookOnRay(fromXY[0], rankY, -1, 0, king.getColor());
            if (!(rook instanceof Rook)) {
                throw new IllegalStateException("No rook found for queen-side castling");
            }
            int rookToX = fromXY[0] - 1;
            board.pieces.remove(rook);
            rook.setPosition(rookToX, rankY);
            board.pieces.add(rook);
        }

        // King loses castling rights and EP is cleared on a castle
        king.setCastleKingSide(false);
        king.setCastleQueenSide(false);
        board.clearEnPassant();
    }

    /**
     * Handles standard capture if there is a piece at the destination square.
     */
    protected void handleCaptureIfAny(Board board, int[] toXY) {
        Piece captured = board.getPieceAt(toXY[0], toXY[1]);
        if (captured != null) {
            board.pieces.remove(captured);
        }
    }

    protected void movePieceToDestination(Piece movingPiece, int[] toXY) {
        movingPiece.setPosition(toXY[0], toXY[1]);
    }

    /**
     * Updates the en passant target square if the moving piece is a pawn
     */
    protected void updateEnPassantTargetIfApplicable(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        board.clearEnPassant();
        if (movingPiece instanceof Pawn && Math.abs(toXY[1] - fromXY[1]) == 2) {
            int dir = forwardDir(movingPiece.getColor());
            int epX = fromXY[0];
            int epY = fromXY[1] + dir; // mid square between start and end
            board.setEnPassant(new int[]{epX, epY});
        }
    }

    protected void updateHalfmove(Board board, Piece movingPiece, boolean isCapture) {
        if (movingPiece instanceof Pawn || isCapture) {
            board.halfmove = 0;
        } else {
            board.halfmove++;
        }
    }

    protected void updateFullMove(Board board) {
        if (board.activeColor == Color.BLACK) {
            board.fullmove++;
        }
    }

    protected void switchPlayer(Board board) {
        if (board.activeColor != null) {
            board.activeColor = board.activeColor.opposite();
        }
    }

    // ================== Utilities ===================

    protected boolean isCastlingMove(int[] fromXY, int[] toXY) {
        return Math.abs(toXY[0] - fromXY[0]) == 2;
    }

    protected boolean isSquareOccupied(Board board, int[] xy) {
        return board.getPieceAt(xy[0], xy[1]) != null;
    }

    /**
     * Returns movement direction for the given color:
     * WHITE moves "up" (-1), BLACK moves "down" (+1).
     */
    protected int forwardDir(Color color) {
        return (color == Color.WHITE) ? -1 : 1;
    }
}
