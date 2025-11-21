package com.predixcode.rules;

import com.predixcode.board.Board;
import com.predixcode.board.pieces.Bureaucrat;
import com.predixcode.board.pieces.Piece;

public class BureaucratCaptureRule extends Rule {

    @Override
    public void afterMove(Board board, MoveContext ctx) {
        Piece captured = ctx.capturedPiece;
        if (!(captured instanceof Bureaucrat)) {
            return;
        }

        ((Bureaucrat) captured).actionOnCapture(board);
        // Mark capture as handled: standard capture rule should not remove it again.
        ctx.captureHandled = true;
    }
}