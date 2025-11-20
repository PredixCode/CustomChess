package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Bureaucrat;
import com.predixcode.core.board.pieces.Piece;

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