package com.teamFive.chessapp.engine;

import com.teamFive.chessapp.model.Move;
import com.teamFive.chessapp.model.Piece;
import com.teamFive.chessapp.model.PieceType;
import com.teamFive.chessapp.model.PlayerColor;
import com.teamFive.chessapp.model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class HintEngine {

    // ----------------------------------------------------------------
    //  Search parameters
    // ----------------------------------------------------------------
    private static final int SEARCH_DEPTH     = 4;   // ply
    private static final int QUIESCE_DEPTH    = 2;   // extra ply for captures only
    private static final int INF              = 1_000_000;
    private static final int CHECKMATE_SCORE  = 900_000;

    // ----------------------------------------------------------------
    //  Material values (centipawns)
    // ----------------------------------------------------------------
    private static int material(PieceType t) {
        switch (t) {
            case QUEEN:  return 900;
            case ROOK:   return 500;
            case BISHOP: return 330;
            case KNIGHT: return 320;
            case PAWN:   return 100;
            case KING:   return 20_000;
            default:     return 0;
        }
    }

    // ----------------------------------------------------------------
    //  Piece-Square Tables (from White's perspective, row 7 = rank 1)
    // ----------------------------------------------------------------
    private static final int[] PST_PAWN = {
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };
    private static final int[] PST_KNIGHT = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };
    private static final int[] PST_BISHOP = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };
    private static final int[] PST_ROOK = {
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         0,  0,  0,  5,  5,  0,  0,  0
    };
    private static final int[] PST_QUEEN = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };
    private static final int[] PST_KING_MID = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };

    private static int pst(PieceType type, int row, int col, PlayerColor color) {
        // Mirror row for BLACK (row 0 for black = rank 8, same as row 7 for white = rank 1)
        int r = (color == PlayerColor.WHITE) ? row : (7 - row);
        int idx = r * 8 + col;
        switch (type) {
            case PAWN:   return PST_PAWN[idx];
            case KNIGHT: return PST_KNIGHT[idx];
            case BISHOP: return PST_BISHOP[idx];
            case ROOK:   return PST_ROOK[idx];
            case QUEEN:  return PST_QUEEN[idx];
            case KING:   return PST_KING_MID[idx];
            default:     return 0;
        }
    }

    // ----------------------------------------------------------------
    //  Board evaluation (positive = good for WHITE)
    // ----------------------------------------------------------------
    private static int evaluate(BoardManager board) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.board[r][c];
                if (p == null) continue;
                int val = material(p.type) + pst(p.type, r, c, p.color);
                score += (p.color == PlayerColor.WHITE) ? val : -val;
            }
        }
        return score;
    }

    // ----------------------------------------------------------------
    //  Public entry point
    // ----------------------------------------------------------------
    /**
     * Returns the best move for the given color, or null if no moves exist.
     */
    public static Move getBestMove(BoardManager board, PlayerColor color) {
        List<Move> moves = getAllValidSafeMoves(board, color);
        if (moves.isEmpty()) return null;

        orderMoves(board, moves, color);

        int alpha = -INF, beta = INF;
        Move best = null;
        boolean maximizing = (color == PlayerColor.WHITE);

        for (Move move : moves) {
            Piece captured = applyMove(board, move);
            int score = minimax(board, SEARCH_DEPTH - 1, alpha, beta, !maximizing);
            undoMove(board, move, captured);

            if (maximizing) {
                if (score > alpha) { alpha = score; best = move; }
            } else {
                if (score < beta)  { beta  = score; best = move; }
            }
        }
        return best;
    }

    // ----------------------------------------------------------------
    //  Minimax with alpha-beta
    // ----------------------------------------------------------------
    private static int minimax(BoardManager board, int depth, int alpha, int beta, boolean maximizing) {
        PlayerColor color = maximizing ? PlayerColor.WHITE : PlayerColor.BLACK;

        if (depth == 0) {
            return quiesce(board, QUIESCE_DEPTH, alpha, beta, maximizing);
        }

        List<Move> moves = getAllValidSafeMoves(board, color);

        if (moves.isEmpty()) {
            if (CheckDetector.isKingInCheck(board, color)) {
                // Checkmate — penalise by depth so engine prefers faster mates
                return maximizing ? -(CHECKMATE_SCORE + depth) : (CHECKMATE_SCORE + depth);
            }
            return 0; // Stalemate
        }

        orderMoves(board, moves, color);

        if (maximizing) {
            int best = -INF;
            for (Move move : moves) {
                Piece cap = applyMove(board, move);
                best = Math.max(best, minimax(board, depth - 1, alpha, beta, false));
                undoMove(board, move, cap);
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break; // β cutoff
            }
            return best;
        } else {
            int best = INF;
            for (Move move : moves) {
                Piece cap = applyMove(board, move);
                best = Math.min(best, minimax(board, depth - 1, alpha, beta, true));
                undoMove(board, move, cap);
                beta = Math.min(beta, best);
                if (beta <= alpha) break; // α cutoff
            }
            return best;
        }
    }

    // ----------------------------------------------------------------
    //  Quiescence search (captures only, avoids horizon effect)
    // ----------------------------------------------------------------
    private static int quiesce(BoardManager board, int depth, int alpha, int beta, boolean maximizing) {
        int standPat = evaluate(board);

        if (maximizing) {
            if (standPat >= beta) return beta;
            alpha = Math.max(alpha, standPat);
        } else {
            if (standPat <= alpha) return alpha;
            beta = Math.min(beta, standPat);
        }

        if (depth == 0) return standPat;

        PlayerColor color = maximizing ? PlayerColor.WHITE : PlayerColor.BLACK;
        List<Move> captures = getCaptureMoves(board, color);

        for (Move move : captures) {
            Piece cap = applyMove(board, move);
            if (CheckDetector.isKingInCheck(board, color)) {
                undoMove(board, move, cap);
                continue;
            }
            int score = quiesce(board, depth - 1, alpha, beta, !maximizing);
            undoMove(board, move, cap);

            if (maximizing) {
                if (score >= beta) return beta;
                alpha = Math.max(alpha, score);
            } else {
                if (score <= alpha) return alpha;
                beta = Math.min(beta, score);
            }
        }
        return maximizing ? alpha : beta;
    }

    // ----------------------------------------------------------------
    //  Move helpers
    // ----------------------------------------------------------------
    private static Piece applyMove(BoardManager board, Move move) {
        Piece captured = board.board[move.to.row][move.to.col];
        board.board[move.to.row][move.to.col]   = board.board[move.from.row][move.from.col];
        board.board[move.from.row][move.from.col] = null;
        // Auto-promote pawn to Queen for evaluation
        Piece p = board.board[move.to.row][move.to.col];
        if (p != null && p.type == PieceType.PAWN) {
            if ((p.color == PlayerColor.WHITE && move.to.row == 0) ||
                (p.color == PlayerColor.BLACK && move.to.row == 7)) {
                board.board[move.to.row][move.to.col] = new Piece(PieceType.QUEEN, p.color);
            }
        }
        return captured;
    }

    private static void undoMove(BoardManager board, Move move, Piece captured) {
        board.board[move.from.row][move.from.col] = board.board[move.to.row][move.to.col];
        board.board[move.to.row][move.to.col]     = captured;
    }

    // ----------------------------------------------------------------
    //  Move generation
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
                        // Safety check: don't leave own king in check
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

    private static List<Move> getCaptureMoves(BoardManager board, PlayerColor color) {
        List<Move> result = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.board[r][c];
                if (p == null || p.color != color) continue;
                for (int tr = 0; tr < 8; tr++) {
                    for (int tc = 0; tc < 8; tc++) {
                        Piece target = board.board[tr][tc];
                        if (target == null || target.color == color) continue;
                        Move move = new Move(new Position(r, c), new Position(tr, tc));
                        if (MoveValidator.isValidMove(board, move, color)) {
                            result.add(move);
                        }
                    }
                }
            }
        }
        return result;
    }

    // ----------------------------------------------------------------
    //  Move ordering — captures by MVV-LVA, then PST delta
    // ----------------------------------------------------------------
    private static void orderMoves(BoardManager board, List<Move> moves, PlayerColor color) {
        moves.sort((a, b) -> {
            int sa = scoreMoveOrder(board, a, color);
            int sb = scoreMoveOrder(board, b, color);
            return sb - sa; // descending
        });
    }

    private static int scoreMoveOrder(BoardManager board, Move move, PlayerColor color) {
        int score = 0;
        Piece target = board.board[move.to.row][move.to.col];
        Piece mover  = board.board[move.from.row][move.from.col];
        if (target != null) {
            // MVV-LVA: Most Valuable Victim — Least Valuable Attacker
            score += 10 * material(target.type) - material(mover.type);
        }
        if (mover != null) {
            score += pst(mover.type, move.to.row, move.to.col, color)
                   - pst(mover.type, move.from.row, move.from.col, color);
        }
        return score;
    }
}
