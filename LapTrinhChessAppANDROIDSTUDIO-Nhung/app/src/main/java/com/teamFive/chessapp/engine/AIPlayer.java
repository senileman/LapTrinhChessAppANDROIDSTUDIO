package com.teamFive.chessapp.engine;

import com.teamFive.chessapp.model.Move;
import com.teamFive.chessapp.model.Piece;
import com.teamFive.chessapp.model.PieceType;
import com.teamFive.chessapp.model.PlayerColor;
import com.teamFive.chessapp.model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * AI đơn giản theo chiến lược Greedy + nhìn trước 1 nước (minimax depth-1).
 *
 * Ưu tiên:
 *  1. Nước ăn quân có giá trị cao nhất
 *  2. Nước thoát chiếu (nếu đang bị chiếu)
 *  3. Nước ngẫu nhiên hợp lệ (thêm yếu tố ngẫu nhiên để game thú vị)
 */
public class AIPlayer {

    private static final Random RNG = new Random();

    // Điểm giá trị từng loại quân
    private static int pieceValue(PieceType type) {
        switch (type) {
            case QUEEN:  return 900;
            case ROOK:   return 500;
            case BISHOP: return 330;
            case KNIGHT: return 320;
            case PAWN:   return 100;
            case KING:   return 20000;
            default:     return 0;
        }
    }

    /**
     * Tính nước đi tốt nhất cho AI (màu BLACK).
     * @return Move hợp lệ, hoặc null nếu không có nước nào
     */
    public static Move getBestMove(BoardManager board, PlayerColor aiColor) {
        List<Move> allMoves = getAllValidMoves(board, aiColor);
        if (allMoves.isEmpty()) return null;

        // Tránh nước để Vua bị chiếu
        List<Move> safeMoves = filterSafeMoves(board, allMoves, aiColor);
        if (safeMoves.isEmpty()) safeMoves = allMoves; // không có nước an toàn → chấp nhận

        // Phân loại nước
        List<ScoredMove> scored = new ArrayList<>();
        for (Move move : safeMoves) {
            int score = evaluateMove(board, move, aiColor);
            scored.add(new ScoredMove(move, score));
        }

        // Sắp xếp giảm dần theo điểm
        Collections.sort(scored, (a, b) -> b.score - a.score);

        // Lấy nhóm nước tốt nhất (trong vòng 50 điểm so với tốt nhất)
        // → thêm ngẫu nhiên để AI không quá cứng nhắc
        int bestScore = scored.get(0).score;
        List<Move> topMoves = new ArrayList<>();
        for (ScoredMove sm : scored) {
            if (bestScore - sm.score <= 50) {
                topMoves.add(sm.move);
            } else {
                break;
            }
        }

        return topMoves.get(RNG.nextInt(topMoves.size()));
    }

    // ----------------------------------------------------------------
    //  Đánh giá điểm 1 nước đi
    // ----------------------------------------------------------------
    private static int evaluateMove(BoardManager board, Move move, PlayerColor aiColor) {
        int score = 0;

        // Ăn quân đối phương → cộng điểm
        Piece target = board.board[move.to.row][move.to.col];
        if (target != null && target.color != aiColor) {
            score += pieceValue(target.type);
        }

        // Thưởng cho việc đẩy quân vào trung tâm (cột 3-4, hàng 3-4)
        int centerBonus = (3 - Math.abs(move.to.col - 3)) + (3 - Math.abs(move.to.row - 3));
        score += centerBonus * 2;

        // Thưởng Pawn tiến về phía trước
        if (isPawn(board, move.from, aiColor)) {
            int direction = (aiColor == PlayerColor.BLACK) ? 1 : -1;
            score += (move.to.row - move.from.row) * direction * 5;
        }

        return score;
    }

    // ----------------------------------------------------------------
    //  Lọc nước an toàn (không để Vua bị chiếu sau khi đi)
    // ----------------------------------------------------------------
    private static List<Move> filterSafeMoves(BoardManager board,
                                               List<Move> moves,
                                               PlayerColor color) {
        List<Move> safe = new ArrayList<>();
        for (Move move : moves) {
            // Giả lập nước đi
            Piece captured = board.board[move.to.row][move.to.col];
            board.board[move.to.row][move.to.col] = board.board[move.from.row][move.from.col];
            board.board[move.from.row][move.from.col] = null;

            boolean inCheck = CheckDetector.isKingInCheck(board, color);

            // Hoàn tác
            board.board[move.from.row][move.from.col] = board.board[move.to.row][move.to.col];
            board.board[move.to.row][move.to.col] = captured;

            if (!inCheck) safe.add(move);
        }
        return safe;
    }

    // ----------------------------------------------------------------
    //  Lấy tất cả nước đi hợp lệ của một màu
    // ----------------------------------------------------------------
    private static List<Move> getAllValidMoves(BoardManager board, PlayerColor color) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.board[r][c];
                if (piece == null || piece.color != color) continue;
                for (int tr = 0; tr < 8; tr++) {
                    for (int tc = 0; tc < 8; tc++) {
                        Move move = new Move(new Position(r, c), new Position(tr, tc));
                        if (MoveValidator.isValidMove(board, move, color)) {
                            moves.add(move);
                        }
                    }
                }
            }
        }
        // Xáo trộn để AI không đi cùng 1 kiểu mãi
        Collections.shuffle(moves, RNG);
        return moves;
    }

    private static boolean isPawn(BoardManager board, Position pos, PlayerColor color) {
        Piece p = board.board[pos.row][pos.col];
        return p != null && p.type == PieceType.PAWN && p.color == color;
    }

    // ----------------------------------------------------------------
    //  Helper class
    // ----------------------------------------------------------------
    private static class ScoredMove {
        final Move move;
        final int  score;
        ScoredMove(Move move, int score) { this.move = move; this.score = score; }
    }
}
