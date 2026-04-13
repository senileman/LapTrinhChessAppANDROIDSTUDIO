package com.teamFive.chessapp.engine;

import com.teamFive.chessapp.model.Piece;
import com.teamFive.chessapp.model.PieceType;
import com.teamFive.chessapp.model.PlayerColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class RandomBoardGenerator {

    // ----------------------------------------------------------------
    //  Constants
    // ----------------------------------------------------------------
    public static final int DEFAULT_TARGET = 45;  // close to standard chess non-king material

    private static final Random RNG = new Random();

    // ----------------------------------------------------------------
    //  Public API
    // ----------------------------------------------------------------

    /** Balanced board using DEFAULT_TARGET for both sides. */
    public static Piece[][] generate() {
        return generate(DEFAULT_TARGET, DEFAULT_TARGET);
    }

    public static Piece[][] generate(int whiteTarget, int blackTarget) {
        Piece[][] board = new Piece[8][8];

        // Kings are always fixed
        board[0][4] = new Piece(PieceType.KING, PlayerColor.BLACK);
        board[7][4] = new Piece(PieceType.KING, PlayerColor.WHITE);

        placeSpecificPieces(board, PlayerColor.BLACK, 0, 1,
                generatePieceList(blackTarget, 15));
        placeSpecificPieces(board, PlayerColor.WHITE, 6, 7,
                generatePieceList(whiteTarget, 15));

        return board;
    }

    // ----------------------------------------------------------------
    //  Simplified piece value (used by UI for display)
    // ----------------------------------------------------------------
    public static int simplifiedValue(PieceType type) {
        switch (type) {
            case QUEEN:  return 9;
            case ROOK:   return 5;
            case BISHOP:
            case KNIGHT: return 3;
            default:     return 1;  // PAWN (KING excluded from totals)
        }
    }

    // ----------------------------------------------------------------
    //  Piece list generation
    // ----------------------------------------------------------------

    private static List<PieceType> generatePieceList(int target, int count) {
        // Clamp to achievable range
        target = Math.max(count, Math.min(count * 9, target));

        List<PieceType> result = new ArrayList<>(count);
        int remaining = target;

        for (int i = 0; i < count; i++) {
            int slotsLeft = count - i - 1;   // positions still to fill after this one

            // Budget constraints for this position:
            //   minPiece: so the rest (slotsLeft slots at max 9 each) can still reach remaining
            //   maxPiece: so the rest (slotsLeft slots at min 1 each) won't undershoot
            int minPiece = Math.max(1, remaining - 9 * slotsLeft);
            int maxPiece = Math.min(9, remaining - slotsLeft);

            // Collect valid piece values from the discrete set {1,3,5,9}
            List<Integer> valid = new ArrayList<>(4);
            for (int v : new int[]{1, 3, 5, 9}) {
                if (v >= minPiece && v <= maxPiece) valid.add(v);
            }

            int chosen;
            if (!valid.isEmpty()) {
                chosen = valid.get(RNG.nextInt(valid.size()));
            } else {
                // Unreachable target (e.g. 133 with 15 pieces):
                // pick the piece value closest to the ideal mid-point.
                int mid = (minPiece + maxPiece) / 2;
                chosen = 1;
                int bestDist = Integer.MAX_VALUE;
                for (int v : new int[]{1, 3, 5, 9}) {
                    int dist = Math.abs(v - mid);
                    if (dist < bestDist) {
                        bestDist = dist;
                        chosen = v;
                    }
                }
            }

            result.add(pieceOfValue(chosen));
            remaining -= chosen;
        }

        return result;
    }

    /** Maps a simplified value to a random concrete PieceType. */
    private static PieceType pieceOfValue(int value) {
        if (value >= 9) return PieceType.QUEEN;
        if (value >= 5) return PieceType.ROOK;
        if (value >= 3) return RNG.nextBoolean() ? PieceType.BISHOP : PieceType.KNIGHT;
        return PieceType.PAWN;
    }

    // ----------------------------------------------------------------
    //  Piece placement
    // ----------------------------------------------------------------

    private static void placeSpecificPieces(Piece[][] board,
                                             PlayerColor color,
                                             int topRow, int bottomRow,
                                             List<PieceType> pieces) {
        // King is always on the back row (row 7 for WHITE, row 0 for BLACK)
        int backRow  = (color == PlayerColor.WHITE) ? bottomRow : topRow;
        int frontRow = (color == PlayerColor.WHITE) ? topRow    : bottomRow;
        int kingCol  = 4;

        // Build cell lists (back: 7, front: 8)
        List<int[]> backCells  = new ArrayList<>(7);
        List<int[]> frontCells = new ArrayList<>(8);
        for (int c = 0; c < 8; c++) {
            if (c != kingCol) backCells.add(new int[]{backRow, c});
            frontCells.add(new int[]{frontRow, c});
        }

        // Partition pieces by pawn-eligibility
        List<PieceType> nonPawns = new ArrayList<>(15);
        List<PieceType> pawns    = new ArrayList<>(15);
        for (PieceType t : pieces) {
            if (t == PieceType.PAWN) pawns.add(t);
            else                     nonPawns.add(t);
        }

        // Ensure we have enough non-pawns for the 7 back-rank cells
        while (nonPawns.size() < backCells.size() && !pawns.isEmpty()) {
            pawns.remove(0);
            nonPawns.add(PieceType.KNIGHT);   // pawn promoted to knight (same color)
        }

        // Shuffle for randomness
        Collections.shuffle(nonPawns, RNG);
        Collections.shuffle(pawns,    RNG);
        Collections.shuffle(backCells,  RNG);
        Collections.shuffle(frontCells, RNG);

        // Fill back rank with non-pawns
        for (int i = 0; i < backCells.size(); i++) {
            int[] pos = backCells.get(i);
            board[pos[0]][pos[1]] = new Piece(nonPawns.get(i), color);
        }

        // Fill front rank with remaining non-pawns + all pawns
        List<PieceType> frontPieces = new ArrayList<>(
                nonPawns.subList(backCells.size(), nonPawns.size()));
        frontPieces.addAll(pawns);
        Collections.shuffle(frontPieces, RNG);

        for (int i = 0; i < frontCells.size() && i < frontPieces.size(); i++) {
            int[] pos = frontCells.get(i);
            board[pos[0]][pos[1]] = new Piece(frontPieces.get(i), color);
        }
    }
}
