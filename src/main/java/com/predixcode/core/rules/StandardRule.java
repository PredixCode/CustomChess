package com.predixcode.core.rules;

import java.util.Set;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Pawn;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.board.pieces.Rook;
import com.predixcode.core.colors.Color;

public class StandardRule implements Rule {

    @Override
    public void applyOnStart(Board board) {}

    @Override
    public void applyOnTurn(Board board, Piece movingPiece, String from, String to) {
        throwIfIllegal(board, movingPiece, to, from);

        // Prepare move
        int[] fromXY = board.fromAlg(from);
        int[] toXY   = board.fromAlg(to);
        boolean isCapture = isSquareOccupied(board, toXY);

        // Handle special moves
        isCapture = enPassante(isCapture, board, movingPiece, fromXY, toXY);
        castling(board, movingPiece, fromXY, toXY, isCapture);

        // Handle standard move and capture
        handleCaptureIfAny(board, toXY);

        movePieceToDestination(movingPiece, toXY);
        nextTurn(board, movingPiece, isCapture);
    }

    public void throwIfIllegal(Board board, Piece movingPiece, String to, String from) {
        if (board.activeColor != null && !movingPiece.getColor().equals(board.activeColor)) {
            throw new IllegalStateException("It is not " + movingPiece.getColor() + "'s turn");
        }

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

    // Utilities
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