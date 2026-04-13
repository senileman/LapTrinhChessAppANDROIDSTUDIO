package com.teamFive.chessapp.engine;
import com.teamFive.chessapp.model.*;

public class BoardManager {
    public Piece[][] board = new Piece[8][8];
    public void initialize() {
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Piece(PieceType.PAWN, PlayerColor.BLACK);
            board[6][i] = new Piece(PieceType.PAWN, PlayerColor.WHITE);
        }
        board[0][0] = board[0][7] = new Piece(PieceType.ROOK, PlayerColor.BLACK);
        board[7][0] = board[7][7] = new Piece(PieceType.ROOK, PlayerColor.WHITE);
        board[0][1] = board[0][6] = new Piece(PieceType.KNIGHT, PlayerColor.BLACK);
        board[7][1] = board[7][6] = new Piece(PieceType.KNIGHT, PlayerColor.WHITE);
        board[0][2] = board[0][5] = new Piece(PieceType.BISHOP, PlayerColor.BLACK);
        board[7][2] = board[7][5] = new Piece(PieceType.BISHOP, PlayerColor.WHITE);
        board[0][3] = new Piece(PieceType.QUEEN, PlayerColor.BLACK);
        board[7][3] = new Piece(PieceType.QUEEN, PlayerColor.WHITE);
        board[0][4] = new Piece(PieceType.KING, PlayerColor.BLACK);
        board[7][4] = new Piece(PieceType.KING, PlayerColor.WHITE);
    }
    public Piece getPiece(Position pos) {
        return board[pos.row][pos.col];
    }

    public void movePiece(Move move) {
        // Đảm bảo class Move của bạn cũng đang sử dụng Position mới này
        board[move.to.row][move.to.col] = board[move.from.row][move.from.col];
        board[move.from.row][move.from.col] = null;
    }
}