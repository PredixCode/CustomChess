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
    public void apply(Board board, Piece movingPiece, String from, String to) {
        int[] fromXY = board.fromAlg(from);
        int[] toXY   = board.fromAlg(to);

        // 1) Turn and target validation
        turnCheck(board, movingPiece, to, from);

        // 2) Initial state/flags
        boolean isPawn = movingPiece instanceof Pawn;
        boolean isKing = movingPiece instanceof King;
        boolean isCapture = isSquareOccupied(board, toXY);

        // 3) Special move: En passant (if applicable)
        //    - If performed, it sets isCapture=true via removal of the captured pawn.
        if (isPawn) {
            boolean enPassantDone = performEnPassantIfApplicable(board, movingPiece, fromXY, toXY);
            if (enPassantDone) {
                isCapture = true;
            }
        }

        // 4) Special move: Castling (if applicable)
        //    - Handles rook move and clears en passant. Skip normal capture & EP-target changes if castling.
        if (isKing && isCastlingMove(fromXY, toXY)) {
            handleCastling(board, (King) movingPiece, fromXY, toXY);
        } else {
            // 5) Normal capture (if any)
            handleStandardCaptureIfAny(board, toXY);

            // 6) Update en passant target (only relevant for a 2-square pawn push)
            updateEnPassantTargetIfApplicable(board, movingPiece, fromXY, toXY);
        }

        // 7) Make the move
        movePieceToDestination(movingPiece, toXY);

        // 8) Halfmove clock update
        updateHalfmoveClock(board, movingPiece, isCapture);

        // 9) Castling rights update
        board.updateCastlingRightsAfterMove(movingPiece, fromXY[0], fromXY[1], toXY[0], toXY[1], isCapture);

        // 10) Next turn
        board.nextTurn();
    }

    // --------------------
    // Validation
    // --------------------

    protected void turnCheck(Board board, Piece movingPiece, String to, String from) {
        if (board.activeColor != null && !movingPiece.getColor().equals(board.activeColor)) {
            throw new IllegalStateException("It is not " + movingPiece.getColor() + "'s turn");
        }

        Set<String> targets = movingPiece.getLegalMoves(board);
        String toAlg = to.toLowerCase();
        if (!targets.contains(toAlg)) {
            throw new IllegalArgumentException("Destination " + to + " is not a pseudo-legal target for " + from);
        }
    }

    // --------------------
    // Special Moves
    // --------------------

    /**
     * Performs en passant if the move is a diagonal pawn move into an empty square.
     * Returns true if an en passant capture was performed.
     */
    private boolean performEnPassantIfApplicable(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
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
    private void handleCastling(Board board, King king, int[] fromXY, int[] toXY) {
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

    // --------------------
    // Standard Moves
    // --------------------

    private void handleStandardCaptureIfAny(Board board, int[] toXY) {
        Piece captured = board.getPieceAt(toXY[0], toXY[1]);
        if (captured != null) {
            board.pieces.remove(captured);
        }
    }

    /**
     * Clears EP by default. If the move is a two-square pawn push,
     * sets the en passant target square to the intermediate square.
     */
    private void updateEnPassantTargetIfApplicable(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        board.clearEnPassant();
        if (movingPiece instanceof Pawn && Math.abs(toXY[1] - fromXY[1]) == 2) {
            int dir = forwardDir(movingPiece.getColor());
            int epX = fromXY[0];
            int epY = fromXY[1] + dir; // mid square between start and end
            board.setEnPassant(new int[]{epX, epY});
        }
    }

    private void movePieceToDestination(Piece movingPiece, int[] toXY) {
        movingPiece.setPosition(toXY[0], toXY[1]);
    }

    // --------------------
    // Clocks & Rights
    // --------------------

    private void updateHalfmoveClock(Board board, Piece movingPiece, boolean isCapture) {
        if (movingPiece instanceof Pawn || isCapture) {
            board.halfmove = 0;
        } else {
            board.halfmove++;
        }
    }

    // --------------------
    // Utilities
    // --------------------

    private boolean isCastlingMove(int[] fromXY, int[] toXY) {
        return Math.abs(toXY[0] - fromXY[0]) == 2;
    }

    private boolean isSquareOccupied(Board board, int[] xy) {
        return board.getPieceAt(xy[0], xy[1]) != null;
    }

    /**
     * Returns movement direction for the given color:
     * WHITE moves "up" (-1), BLACK moves "down" (+1).
     */
    private int forwardDir(Color color) {
        return (color == Color.WHITE) ? -1 : 1;
    }
}