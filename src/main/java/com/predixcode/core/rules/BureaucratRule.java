package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Bureaucrat;
import com.predixcode.core.board.pieces.Piece;

public class BureaucratRule extends StandardRule {

    @Override
    protected void handleCaptureIfAny(Board board, int[] toXY) {
        Piece captured = board.getPieceAt(toXY[0], toXY[1]);
        if (captured != null) {
            boolean isBureaucrat = captured instanceof Bureaucrat;
            if (isBureaucrat) {
                captured.actionOnCapture(board);
                return;
            }
        }
        super.handleCaptureIfAny(board, toXY);
    }
    
}
