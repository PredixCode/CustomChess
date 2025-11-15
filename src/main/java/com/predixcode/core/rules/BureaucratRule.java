package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Bureaucrat;
import com.predixcode.core.board.pieces.Piece;

public class BureaucratRule extends StandardRule {

    @Override
    protected void handleCaptureIfAny(Board board, int[] toXY) {
        Piece captured = board.getPieceAt(toXY[0], toXY[1]);
        boolean isBureaucrat = captured != null && captured instanceof Bureaucrat;

        if (captured != null) {
            if (isBureaucrat) {
                captured.actionOnCapture(board);
            } else {
                board.pieces.remove(captured);
            }
        }
    }
    
}
