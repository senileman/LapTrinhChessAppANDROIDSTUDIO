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
 *  - Mỗi bên có đúng 1 Vua + 15 quân ngẫu nhiên (Queen/Rook/Bishop/Knight/Pawn)
 *  - Quân được xếp ngẫu nhiên trong 2 hàng đầu của mỗi bên
 *  - Pawn không được đứng ở hàng cuối (hàng 0 hoặc 7)
 *  - King KHÔNG bao giờ nằm trong pool ngẫu nhiên — chỉ có đúng 1 Vua mỗi bên
 */
public class RandomBoardGenerator {

    private static final Random RNG = new Random();

    /**
     * Pool ngẫu nhiên — KHÔNG bao gồm KING.
     * Trọng số xuất hiện: Pawn nhiều hơn các quân mạnh.
     */
    private static final PieceType[] POOL = {
            PieceType.QUEEN,  PieceType.QUEEN,
            PieceType.ROOK,   PieceType.ROOK,   PieceType.ROOK,
            PieceType.BISHOP, PieceType.BISHOP,  PieceType.BISHOP,
            PieceType.KNIGHT, PieceType.KNIGHT,  PieceType.KNIGHT,
            PieceType.PAWN,   PieceType.PAWN,    PieceType.PAWN,
            PieceType.PAWN,   PieceType.PAWN,    PieceType.PAWN,
            PieceType.PAWN,   PieceType.PAWN,    PieceType.PAWN,
    };

    /**
     * Tạo bàn cờ 8x8 với quân ngẫu nhiên.
     * @return mảng Piece[8][8], null = ô trống
     */
    public static Piece[][] generate() {
        Piece[][] board = new Piece[8][8];

        placeRandomPieces(board, PlayerColor.BLACK, 0, 1);  // hàng 0-1
        placeRandomPieces(board, PlayerColor.WHITE, 6, 7);  // hàng 6-7

        return board;
    }

    private static void placeRandomPieces(Piece[][] board,
                                           PlayerColor color,
                                           int topRow, int bottomRow) {
        // 16 vị trí trong 2 hàng
        List<int[]> positions = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            positions.add(new int[]{topRow,    c});
            positions.add(new int[]{bottomRow, c});
        }
        Collections.shuffle(positions, RNG);

        // --- Vua luôn ở vị trí đầu tiên (ngẫu nhiên trong 16 ô) ---
        int[] kingPos = positions.get(0);
        board[kingPos[0]][kingPos[1]] = new Piece(PieceType.KING, color);

        // --- 15 quân còn lại: random từ POOL (không chứa KING) ---
        for (int i = 0; i < 15; i++) {
            int[] pos  = positions.get(i + 1);
            PieceType type = POOL[RNG.nextInt(POOL.length)]; // guaranteed non-KING

            // Pawn không được ở hàng cuối (hàng 0 hoặc 7 → sẽ phải phong cấp ngay)
            // Nếu rơi vào hàng cấm, đổi thành Knight
            if (type == PieceType.PAWN && (pos[0] == 0 || pos[0] == 7)) {
                type = PieceType.KNIGHT;
            }

            board[pos[0]][pos[1]] = new Piece(type, color);
        }
    }
}
