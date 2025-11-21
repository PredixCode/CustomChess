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

        // --- Detect capture ---
        boolean isCapture = board.isSquareOccupied(toXY);

        // --- En passant: pawn moves diagonally into empty square ---
        if (movingPiece instanceof Pawn
                && fromXY[0] != toXY[0]
                && board.isEmpty(toXY[0], toXY[1])) {

            boolean epDone = board.performEnPassantIfApplicable(movingPiece, fromXY, toXY);
            if (epDone) {
                ctx.isEnPassant = true;
                ctx.isCapture = true;
                ctx.capturedPiece = getLastCapturedPiece(board);
                ctx.captureHandled = true; // captured pawn already removed inside performEnPassant
                return;
            }
        }

        // --- Castling rook move (rook motion only; king move is in core) ---
        if (movingPiece instanceof King && board.isCastlingMove(fromXY, toXY)) {
            ctx.isCastling = true;
            // Rook is moved inside handleCastling, which we call AFTER king move:
            // We'll call handleCastling in afterMove.
        }

        // Regular capture (non-EP, non-special)
        if (isCapture) {
            ctx.isCapture = true;
            ctx.capturedPiece = board.getPieceAt(toXY[0], toXY[1]);
            // Do NOT remove yet; we let afterMove handle that so other rules (e.g. Bureaucrat)
            // can inspect ctx.capturedPiece first.
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

        // --- Handle standard capture if not handled by another rule ---
        if (ctx.isCapture && !ctx.captureHandled) {
            board.handleCaptureIfAny(toXY);
            ctx.capturedPiece = getLastCapturedPiece(board);
            ctx.captureHandled = true;
        }

        // --- Update special move states: EP target & castling rights ---
        board.updateEnPassantTargetIfApplicable(movingPiece, fromXY, toXY);
        board.updateCastlingRights(movingPiece, fromXY[0], fromXY[1], toXY[0], toXY[1], ctx.isCapture);
    }

    private static Piece getLastCapturedPiece(Board board) {
        // You can either expose a getter on Board or keep using lastCapturedPiece internally
        // For now we don't strictly need it if only ClickOutcome cares about last capture.
        return null; // optional: fill in if you expose it
    }
}