package com.predixcode.rules;

import com.predixcode.board.Board;
import com.predixcode.board.pieces.King;
import com.predixcode.board.pieces.Pawn;
import com.predixcode.board.pieces.Piece;

public class StandardMovementRule extends Rule {

    @Override
    public void beforeMove(Board board, MoveContext ctx) {
        Piece movingPiece = ctx.piece;
        int[] fromXY = ctx.fromXY;
        int[] toXY   = ctx.toXY;

        // Detect if there is a piece on the target square (regular capture case).
        boolean isCapture = board.isSquareOccupied(toXY);

        // --- En passant: pawn moves diagonally into empty square ---
        if (movingPiece instanceof Pawn
                && fromXY[0] != toXY[0]
                && board.isEmpty(toXY[0], toXY[1])) {

            boolean epDone = board.performEnPassantIfApplicable(movingPiece, fromXY, toXY, ctx);
            if (epDone) {
                ctx.isEnPassant = true;
                ctx.isCapture = true;
                ctx.captureHandled = true; // EP pawn already removed inside performEnPassant
                return;
            }
        }

        // --- Castling detection (king moves two files horizontally) ---
        if (movingPiece instanceof King && board.isCastlingMove(fromXY, toXY)) {
            ctx.isCastling = true;
        }

        // --- Regular capture (non-EP, non-special) ---
        if (isCapture) {
            ctx.isCapture = true;
            ctx.capturedPiece = board.getPieceAt(toXY[0], toXY[1]);
            // Actual removal happens in afterMove via handleCaptureIfAny(...)
        }
    }

    @Override
    public void afterMove(Board board, MoveContext ctx) {
        Piece movingPiece = ctx.piece;
        int[] fromXY = ctx.fromXY;
        int[] toXY   = ctx.toXY;

        // --- Handle castling rook move AFTER king is moved ---
        if (ctx.isCastling && movingPiece instanceof King king) {
            board.handleCastling(king, fromXY, toXY);
        }

        // --- Handle standard capture if not handled by another rule (non-EP) ---
        if (ctx.isCapture && !ctx.captureHandled) {
            board.handleCaptureIfAny(toXY, ctx);
            ctx.captureHandled = true;
        }

        // --- Update EP target & castling rights ---
        board.updateEnPassantTargetIfApplicable(movingPiece, fromXY, toXY);
        board.updateCastlingRights(
            movingPiece,
            fromXY[0], fromXY[1],
            toXY[0],   toXY[1],
            ctx.isCapture
        );
    }
}