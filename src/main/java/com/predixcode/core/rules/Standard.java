package com.predixcode.core.rules;

import java.util.Set;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Pawn;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.board.pieces.Rook;
import com.predixcode.core.colors.Color;

public class Standard implements Rule {

    @Override
    public void apply(Board board, Piece movingPiece, String from, String to) {
        int[] fromXY = board.fromAlg(from);
        int[] toXY = board.fromAlg(to);

        turnCheck(board, movingPiece, to, from);

        boolean isCapture = (board.getPieceAt(toXY[0], toXY[1]) != null);
        boolean isPawn = movingPiece instanceof Pawn;
        boolean isKing = movingPiece instanceof King;

        // Handle en passant capture
        if (isPawn && fromXY[0] != toXY[0] && board.isEmpty(toXY[0], toXY[1])) {
            // Diagonal move into empty square => en passant
            int dir = movingPiece.getColor() == Color.WHITE ? -1 : 1;
            Piece epPawn = board.getPieceAt(toXY[0], toXY[1] - dir);
            if (epPawn == null || !(epPawn instanceof Pawn) || epPawn.getColor() == movingPiece.getColor()) {
                throw new IllegalStateException("Invalid en passant capture attempted");
            }
            board.pieces.remove(epPawn);
            isCapture = true;
        }

        // Handle castling (king moves two squares horizontally)
        if (isKing && Math.abs(toXY[0] - fromXY[0]) == 2) {
            King king = (King) movingPiece;
            int rankY = fromXY[1];
            if (toXY[0] > fromXY[0]) {
                // King-side castle: move rook from nearest right rook to f-file (x = fromX+1)
                Piece rook = board.findFirstRookOnRay(fromXY[0], rankY, +1, 0, movingPiece.getColor());
                if (!(rook instanceof Rook)) throw new IllegalStateException("No rook found for king-side castling");
                int rookToX = fromXY[0] + 1;
                board.pieces.remove(rook);
                rook.setPosition(rookToX, rankY);
                board.pieces.add(rook);
            } else {
                // Queen-side castle: move rook from nearest left rook to d-file (x = fromX-1)
                Piece rook = board.findFirstRookOnRay(fromXY[0], rankY, -1, 0, movingPiece.getColor());
                if (!(rook instanceof Rook)) throw new IllegalStateException("No rook found for queen-side castling");
                int rookToX = fromXY[0] - 1;
                board.pieces.remove(rook);
                rook.setPosition(rookToX, rankY);
                board.pieces.add(rook);
            }
            // King loses castling rights
            king.setCastleKingSide(false);
            king.setCastleQueenSide(false);
            board.clearEnPassant();
        } else {
            // Normal capture if any on destination
            Piece captured = board.getPieceAt(toXY[0], toXY[1]);
            if (captured != null) {
                board.pieces.remove(captured);
            }

            // Update en passant target for next move (only after a two-square pawn push)
            board.clearEnPassant();
            if (isPawn && Math.abs(toXY[1] - fromXY[1]) == 2) {
                int dir = movingPiece.getColor() == Color.WHITE ? -1 : 1;
                int epX = fromXY[0];
                int epY = fromXY[1] + dir; // mid square
                board.setEnPassant(new int[]{epX, epY});
            }
        }

        // Move the piece
        movingPiece.setPosition(toXY[0], toXY[1]);

        // Update halfmove clock
        if (isPawn || isCapture) {
            board.halfmove = 0;
        } else {
            board.halfmove++;
        }

        // Update castling rights after king/rook move or rook capture
        board.updateCastlingRightsAfterMove(movingPiece, fromXY[0], fromXY[1], toXY[0], toXY[1], isCapture);

        board.nextTurn();
    }

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
}
