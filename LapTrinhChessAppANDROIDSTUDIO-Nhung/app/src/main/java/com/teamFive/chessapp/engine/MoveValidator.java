package com.teamFive.chessapp.engine;

import com.teamFive.chessapp.model.*;

public class MoveValidator {

    public static boolean isValidMove(BoardManager board, Move move, PlayerColor turn) {

        Piece piece = board.getPiece(move.from);
        if (piece == null || piece.color != turn) return false;

        Piece target = board.getPiece(move.to);
        if (target != null && target.color == turn) return false;

        int dr = move.to.row - move.from.row;
        int dc = move.to.col - move.from.col;

        switch (piece.type) {

            case PAWN:
                return validatePawn(board, move, piece);

            case ROOK:
                return validateRook(board, move) && isPathClear(board, move);

            case BISHOP:
                return validateBishop(move) && isPathClear(board, move);

            case QUEEN:
                return validateQueen(move) && isPathClear(board, move);

            case KNIGHT:
                return validateKnight(move);

            case KING:
                return validateKing(move);
        }

        return false;
    }

    // ================= PAWN =================
    private static boolean validatePawn(BoardManager board, Move move, Piece piece) {

        int dir = (piece.color == PlayerColor.WHITE) ? -1 : 1;

        int dr = move.to.row - move.from.row;
        int dc = move.to.col - move.from.col;

        Piece target = board.getPiece(move.to);

        // Đi thẳng 1 ô
        if (dc == 0 && dr == dir && target == null) {
            return true;
        }

        // Đi 2 ô lần đầu
        if (dc == 0 && dr == 2 * dir) {
            if ((piece.color == PlayerColor.WHITE && move.from.row == 6) ||
                    (piece.color == PlayerColor.BLACK && move.from.row == 1)) {

                // phải không bị chặn giữa đường
                int midRow = move.from.row + dir;
                if (board.board[midRow][move.from.col] == null && target == null) {
                    return true;
                }
            }
        }

        // Ăn chéo
        if (Math.abs(dc) == 1 && dr == dir && target != null && target.color != piece.color) {
            return true;
        }

        return false;
    }

    // ================= ROOK =================
    private static boolean validateRook(BoardManager board, Move move) {
        return move.from.row == move.to.row || move.from.col == move.to.col;
    }

    // ================= BISHOP =================
    private static boolean validateBishop(Move move) {
        return Math.abs(move.to.row - move.from.row) ==
                Math.abs(move.to.col - move.from.col);
    }

    // ================= QUEEN =================
    private static boolean validateQueen(Move move) {
        int dr = move.to.row - move.from.row;
        int dc = move.to.col - move.from.col;

        return dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc);
    }

    // ================= KNIGHT =================
    private static boolean validateKnight(Move move) {
        int dr = Math.abs(move.to.row - move.from.row);
        int dc = Math.abs(move.to.col - move.from.col);

        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    // ================= KING =================
    private static boolean validateKing(Move move) {
        int dr = Math.abs(move.to.row - move.from.row);
        int dc = Math.abs(move.to.col - move.from.col);

        return dr <= 1 && dc <= 1;
    }

    // ================= PATH BLOCKING =================
    private static boolean isPathClear(BoardManager board, Move move) {

        int dr = Integer.compare(move.to.row, move.from.row);
        int dc = Integer.compare(move.to.col, move.from.col);

        int r = move.from.row + dr;
        int c = move.from.col + dc;

        while (r != move.to.row || c != move.to.col) {

            if (board.board[r][c] != null) {
                return false;
            }

            r += dr;
            c += dc;
        }

        return true;
    }
}