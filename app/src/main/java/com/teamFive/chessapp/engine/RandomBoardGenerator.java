package com.teamFive.chessapp.engine;

import com.teamFive.chessapp.model.Piece;
import com.teamFive.chessapp.model.PieceType;
import com.teamFive.chessapp.model.PlayerColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Sinh bàn cờ ngẫu nhiên theo phong cách "Really Bad Chess":
 *  - Mỗi bên có đúng 1 Vua cố định (e1 / e8) + 15 quân ngẫu nhiên
 *  - Quân được xếp ngẫu nhiên trong 2 hàng đầu của mỗi bên
 *  - Pawn không được đứng ở hàng cuối (hàng 0 hoặc 7)
 *  - King KHÔNG bao giờ nằm trong pool ngẫu nhiên
 */
public class RandomBoardGenerator {

    private static final Random RNG = new Random();

    private static final PieceType[] POOL = {
            PieceType.QUEEN,  PieceType.QUEEN,
            PieceType.ROOK,   PieceType.ROOK,   PieceType.ROOK,
            PieceType.BISHOP, PieceType.BISHOP,  PieceType.BISHOP,
            PieceType.KNIGHT, PieceType.KNIGHT,  PieceType.KNIGHT,
            PieceType.PAWN,   PieceType.PAWN,    PieceType.PAWN,
            PieceType.PAWN,   PieceType.PAWN,    PieceType.PAWN,
            PieceType.PAWN,   PieceType.PAWN,    PieceType.PAWN,
    };

    public static Piece[][] generate() {
        Piece[][] board = new Piece[8][8];

        // Kings are always fixed:
        //   Black King on e8  → row 0, col 4
        //   White King on e1  → row 7, col 4
        board[0][4] = new Piece(PieceType.KING, PlayerColor.BLACK);
        board[7][4] = new Piece(PieceType.KING, PlayerColor.WHITE);

        placeRandomPieces(board, PlayerColor.BLACK, 0, 1);  // hàng 0-1
        placeRandomPieces(board, PlayerColor.WHITE, 6, 7);  // hàng 6-7

        return board;
    }

    private static void placeRandomPieces(Piece[][] board,
                                           PlayerColor color,
                                           int topRow, int bottomRow) {
        // King's fixed square must be excluded from the random pool
        int kingRow = (color == PlayerColor.WHITE) ? 7 : 0;
        int kingCol = 4;

        List<int[]> positions = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            if (!(topRow    == kingRow && c == kingCol)) positions.add(new int[]{topRow,    c});
            if (!(bottomRow == kingRow && c == kingCol)) positions.add(new int[]{bottomRow, c});
        }
        // positions now has exactly 15 free squares
        Collections.shuffle(positions, RNG);

        for (int i = 0; i < 15; i++) {
            int[] pos = positions.get(i);
            PieceType type = POOL[RNG.nextInt(POOL.length)];

            // Pawn cannot stand on the last rank (would need immediate promotion)
            if (type == PieceType.PAWN && (pos[0] == 0 || pos[0] == 7)) {
                type = PieceType.KNIGHT;
            }

            board[pos[0]][pos[1]] = new Piece(type, color);
        }
    }
}
