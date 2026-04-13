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


public class AIPlayer {

    private static final int   INF = 1_000_000;
    private static final Random RNG = new Random();

    // Basic material values only — no positional bonuses
    private static int pieceValue(PieceType type) {
        switch (type) {
            case QUEEN:  return 900;
            case ROOK:   return 500;
            case BISHOP: return 330;
            case KNIGHT: return 320;
            case PAWN:   return 100;
            case KING:   return 20_000;
            default:     return 0;
        }
    }

    // Simple material count (positive = good for WHITE)
    private static int evaluate(BoardManager board) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.board[r][c];
                if (p == null) continue;
                int val = pieceValue(p.type);
                score += (p.color == PlayerColor.WHITE) ? val : -val;
            }
        }
        // Small random noise to break ties differently each game
        score += RNG.nextInt(15) - 7;
        return score;
    }

    public static Move getBestMove(BoardManager board, PlayerColor aiColor) {
        List<Move> moves = getAllValidSafeMoves(board, aiColor);
        if (moves.isEmpty()) return null;

        // Shuffle so equal-scored moves vary game to game
        Collections.shuffle(moves, RNG);

        boolean maximizing = (aiColor == PlayerColor.WHITE);
        int alpha = -INF, beta = INF;
        Move best = null;
        int bestScore = maximizing ? -INF : INF;

        for (Move move : moves) {
            Piece captured = applyMove(board, move);
            int score = minimax(board, 1, alpha, beta, !maximizing);
            undoMove(board, move, captured);

            if (maximizing ? score > bestScore : score < bestScore) {
                bestScore = score;
                best = move;
            }
            if (maximizing) alpha = Math.max(alpha, bestScore);
            else            beta  = Math.min(beta,  bestScore);
        }
        return best;
    }

    // ----------------------------------------------------------------
    //  Minimax — depth 2 total (1 remaining after root)
    // ----------------------------------------------------------------
    private static int minimax(BoardManager board, int depth, int alpha, int beta, boolean maximizing) {
        PlayerColor color = maximizing ? PlayerColor.WHITE : PlayerColor.BLACK;

        if (depth == 0) return evaluate(board);

        List<Move> moves = getAllValidSafeMoves(board, color);
        if (moves.isEmpty()) {
            if (CheckDetector.isKingInCheck(board, color)) {
                return maximizing ? -(INF - depth) : (INF - depth);
            }
            return 0; // stalemate
        }

        if (maximizing) {
            int best = -INF;
            for (Move move : moves) {
                Piece cap = applyMove(board, move);
                best = Math.max(best, minimax(board, depth - 1, alpha, beta, false));
                undoMove(board, move, cap);
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break;
            }
            return best;
        } else {
            int best = INF;
            for (Move move : moves) {
                Piece cap = applyMove(board, move);
                best = Math.min(best, minimax(board, depth - 1, alpha, beta, true));
                undoMove(board, move, cap);
                beta = Math.min(beta, best);
                if (beta <= alpha) break;
            }
            return best;
        }
    }

    // ----------------------------------------------------------------
    //  Move helpers
    // ----------------------------------------------------------------
    private static Piece applyMove(BoardManager board, Move move) {
        Piece captured = board.board[move.to.row][move.to.col];
        board.board[move.to.row][move.to.col]     = board.board[move.from.row][move.from.col];
        board.board[move.from.row][move.from.col] = null;
        return captured;
    }

    private static void undoMove(BoardManager board, Move move, Piece captured) {
        board.board[move.from.row][move.from.col] = board.board[move.to.row][move.to.col];
        board.board[move.to.row][move.to.col]     = captured;
    }

    // ----------------------------------------------------------------
    //  Move generation — only legal moves (no self-check)
    // ----------------------------------------------------------------
    private static List<Move> getAllValidSafeMoves(BoardManager board, PlayerColor color) {
        List<Move> result = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.board[r][c];
                if (p == null || p.color != color) continue;
                for (int tr = 0; tr < 8; tr++) {
                    for (int tc = 0; tc < 8; tc++) {
                        Move move = new Move(new Position(r, c), new Position(tr, tc));
                        if (!MoveValidator.isValidMove(board, move, color)) continue;
                        Piece cap = applyMove(board, move);
                        boolean safe = !CheckDetector.isKingInCheck(board, color);
                        undoMove(board, move, cap);
                        if (safe) result.add(move);
                    }
                }
            }
        }
        return result;
    }
}
