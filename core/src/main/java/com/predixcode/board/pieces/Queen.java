package com.predixcode.board.pieces;

import java.util.LinkedHashSet;
import java.util.Set;

import com.predixcode.board.Board;

public class Queen extends Piece {
    public Queen() { this.fenSymbol = "q"; }

    @Override
    public Set<String> getLegalMoves(Board board) {
        Set<String> out = new LinkedHashSet<>();
        addRay(board, out,  1,  0);
        addRay(board, out, -1,  0);
        addRay(board, out,  0,  1);
        addRay(board, out,  0, -1);
        addRay(board, out,  1,  1);
        addRay(board, out,  1, -1);
        addRay(board, out, -1,  1);
        addRay(board, out, -1, -1);
        return out;
    }

    @Override
    public Set<int[]> attackedSquares(Board board) {
        Set<int[]> out = new LinkedHashSet<>();
        addAttackRay(board, out,  1,  0);
        addAttackRay(board, out, -1,  0);
        addAttackRay(board, out,  0,  1);
        addAttackRay(board, out,  0, -1);
        addAttackRay(board, out,  1,  1);
        addAttackRay(board, out,  1, -1);
        addAttackRay(board, out, -1,  1);
        addAttackRay(board, out, -1, -1);
        return out;
    }

    private void addRay(Board board, Set<String> out, int dx, int dy) {
        int nx = this.posX + dx;
        int ny = this.posY + dy;
        while (board.inBounds(nx, ny)) {
            Piece at = board.getPieceAt(nx, ny);
            if (at == null) {
                out.add(board.toAlg(nx, ny));
            } else {
                if (!at.getColor().equals(this.color)) {
                    out.add(board.toAlg(nx, ny));
                }
                break;
            }
            nx += dx; ny += dy;
        }
    }

    private void addAttackRay(Board board, Set<int[]> out, int dx, int dy) {
        int nx = this.posX + dx;
        int ny = this.posY + dy;
        while (board.inBounds(nx, ny)) {
            out.add(new int[]{nx, ny});
            if (board.getPieceAt(nx, ny) != null) break; // stop at first piece
            nx += dx; ny += dy;
        }
    }
}