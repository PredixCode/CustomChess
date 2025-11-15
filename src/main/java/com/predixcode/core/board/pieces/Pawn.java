package com.predixcode.core.board.pieces;

import java.util.LinkedHashSet;
import java.util.Set;

import com.predixcode.core.board.Board;
import com.predixcode.core.colors.Color;

public class Pawn extends Piece {

    public Pawn() {
        super();
        this.fenSymbol = "p";
    }

    @Override
    public Set<String> pseudoLegalTargets(Board board) {
        Set<String> out = new LinkedHashSet<>();
        int dir = (this.color == Color.WHITE) ? -1 : 1;
        int startRank = (this.color == Color.WHITE) ? 6 : 1;

        int oneY = this.y + dir;
        // Single push
        if (board.inBounds(this.x, oneY) && board.isEmpty(this.x, oneY)) {
            out.add(board.toAlg(this.x, oneY));

            // Double push from start rank
            int twoY = this.y + 2*dir;
            if (this.y == startRank && board.inBounds(this.x, twoY)
                    && board.isEmpty(this.x, twoY)) {
                out.add(board.toAlg(this.x, twoY));
            }
        }

        // Captures
        int[] dxs = new int[]{-1, +1};
        for (int dx : dxs) {
            int tx = this.x + dx;
            int ty = this.y + dir;
            if (!board.inBounds(tx, ty)) continue;

            var at = board.getPieceAt(tx, ty);
            if (at != null && at.getColor() != this.color) {
                out.add(board.toAlg(tx, ty));
            }
        }

        // En passant (use board's EP square)
        int[] ep = board.getEnPassantXY();
        if (ep[0] >= 0 && ep[1] >= 0) {
            for (int dx : dxs) {
                int tx = this.x + dx;
                int ty = this.y + dir;
                if (tx == ep[0] && ty == ep[1]) {
                    // Ensure a capturable enemy pawn exists on adjacent file at current rank
                    int capturedPawnY = this.y; // the enemy pawn is adjacent on same rank
                    var sidePawn = board.getPieceAt(tx, capturedPawnY);
                    if (sidePawn instanceof Pawn && sidePawn.getColor() != this.color) {
                        out.add(board.toAlg(tx, ty));
                    }
                }
            }
        }

        return out;
    }

    @Override
    public Set<int[]> attackedSquares(Board board) {
        Set<int[]> out = new LinkedHashSet<>();
        int dir = (this.color == Color.WHITE) ? -1 : 1;
        int[] dxs = new int[]{-1, +1};
        for (int dx : dxs) {
            int tx = this.x + dx;
            int ty = this.y + dir;
            if (board.inBounds(tx, ty)) out.add(new int[]{tx, ty});
        }
        return out;
    }
}