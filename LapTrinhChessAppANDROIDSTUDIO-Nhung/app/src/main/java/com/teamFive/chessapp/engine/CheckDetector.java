package com.teamFive.chessapp.engine;

import com.teamFive.chessapp.model.*;

public class CheckDetector {

    /**
     * KIỂM TRA XEM VUA CÓ ĐANG BỊ CHIẾU HAY KHÔNG
     * Bạn CẦN phương thức này để hàm isCheckmate bên dưới hoạt động
     */
    public static boolean isKingInCheck(BoardManager board, PlayerColor color) {
        // Tìm vị trí quân Vua của phe đang xét
        Position kingPos = null;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = board.board[i][j];
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    kingPos = new Position(i, j);
                    break;
                }
            }
        }

        if (kingPos == null) return false;

        // Kiểm tra xem có quân đối phương nào có thể ăn được Vua không
        PlayerColor opponentColor = (color == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = board.board[i][j];
                if (p != null && p.color == opponentColor) {
                    Move attackMove = new Move(new Position(i, j), kingPos);
                    // Giả sử bạn đã có class MoveValidator để kiểm tra nước đi hợp lệ
                    if (MoveValidator.isValidMove(board, attackMove, opponentColor)) {
                        return true; // Vua đang bị chiếu
                    }
                }
            }
        }
        return false;
    }

    /**
     * KIỂM TRA CHIẾU BÍ (CHECKMATE)
     */
    public static boolean isCheckmate(BoardManager board, PlayerColor color) {

        // 1. Nếu không bị check → không phải checkmate
        if (!isKingInCheck(board, color)) {
            return false;
        }

        // 2. Thử tất cả nước đi có thể để xem có thoát chiếu được không
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.board[i][j];

                if (piece != null && piece.color == color) {
                    for (int r = 0; r < 8; r++) {
                        for (int c = 0; c < 8; c++) {
                            Move move = new Move(new Position(i, j), new Position(r, c));

                            if (MoveValidator.isValidMove(board, move, color)) {
                                // Thực hiện đi thử (giả lập)
                                Piece originalTarget = board.board[r][c];
                                board.board[r][c] = board.board[i][j];
                                board.board[i][j] = null;

                                // Kiểm tra sau khi đi thử còn bị chiếu không
                                boolean stillInCheck = isKingInCheck(board, color);

                                // Hoàn tác (Undo) nước đi thử
                                board.board[i][j] = board.board[r][c];
                                board.board[r][c] = originalTarget;

                                if (!stillInCheck) {
                                    return false; // Tìm thấy ít nhất 1 nước thoát chiếu
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Không có nước nào cứu được → CHECKMATE
        return true;
    }
}