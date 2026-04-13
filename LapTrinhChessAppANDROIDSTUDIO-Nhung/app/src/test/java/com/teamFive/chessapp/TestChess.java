package com.teamFive.chessapp;

import com.teamFive.chessapp.engine.*;
import com.teamFive.chessapp.model.*;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestChess {

    @Test
    public void runLogicTest() {

        System.out.println("--- ĐANG CHẠY TEST LOGIC CỜ VUA ---");

        // ================= TEST 1: Pawn (Quân Tốt) =================
        GameEngine engine = new GameEngine();

        // Tốt trắng đi 1 ô từ hàng 6 lên hàng 5
        boolean pawn1 = engine.makeMove(
                new Move(new Position(6, 0), new Position(5, 0))
        );
        System.out.println("Pawn đi 1 ô: " + pawn1);
        assertTrue("Lỗi: Tốt trắng phải đi được 1 ô", pawn1);

        // Tốt đen đi 2 ô từ hàng 1 xuống hàng 3 (lượt đen)
        boolean pawn2 = engine.makeMove(
                new Move(new Position(1, 0), new Position(3, 0))
        );
        System.out.println("Pawn đi 2 ô: " + pawn2);
        assertTrue("Lỗi: Tốt đen phải đi được 2 ô ở nước đầu tiên", pawn2);

        // ================= TEST 2: Blocked path (Bị chặn) =================
        GameEngine engine2 = new GameEngine();

        // Xe (Rook) ở (7,0) không thể nhảy qua Tốt ở (6,0)
        boolean rookBlocked = engine2.makeMove(
                new Move(new Position(7, 0), new Position(5, 0))
        );
        System.out.println("Rook bị chặn: " + rookBlocked);
        assertFalse("Lỗi: Xe không được nhảy qua quân khác", rookBlocked);

        // ================= TEST 3: Knight (Mã nhảy qua quân) =================
        GameEngine engine3 = new GameEngine();

        // Mã ở (7,1) nhảy lên (5,2) qua đầu hàng tốt
        boolean knightMove = engine3.makeMove(
                new Move(new Position(7, 1), new Position(5, 2))
        );
        System.out.println("Knight nhảy qua quân: " + knightMove);
        assertTrue("Lỗi: Mã phải nhảy qua được quân khác", knightMove);

        // ================= TEST 4: Check (Chiếu tướng) =================
        GameEngine engine4 = new GameEngine();
        BoardManager board = engine4.getBoard();

        // clear board
        board.board = new Piece[8][8];

        // đặt vua đen
        board.board[0][4] = new Piece(PieceType.KING, PlayerColor.BLACK);

        // đặt hậu trắng chiếu
        board.board[4][4] = new Piece(PieceType.QUEEN, PlayerColor.WHITE);

        boolean isCheck = CheckDetector.isKingInCheck(board, PlayerColor.BLACK);

        System.out.println("Vua đen bị chiếu: " + isCheck);
        assertTrue("Lỗi: Vua đen phải ở trạng thái bị chiếu", isCheck);

        // ================= TEST 5: Checkmate (Chiếu bí - Fool's Mate) =================
        GameEngine engine5 = new GameEngine();

        // Fool's Mate
        engine5.makeMove(new Move(new Position(6, 5), new Position(5, 5))); // Trắng: f3
        engine5.makeMove(new Move(new Position(1, 4), new Position(3, 4))); // Đen: e5
        engine5.makeMove(new Move(new Position(6, 6), new Position(4, 6))); // Trắng: g4
        engine5.makeMove(new Move(new Position(0, 3), new Position(4, 7))); // Đen: Qh4# (Chiếu bí)

        boolean isMate = CheckDetector.isCheckmate(
                engine5.getBoard(), PlayerColor.WHITE
        );

        System.out.println("Trắng bị chiếu bí: " + isMate);
        assertTrue("Lỗi: Đây phải là thế chiếu bí (Fool's Mate)", isMate);

        // ================= TEST 6: Pawn ăn chéo =================
        GameEngine engine6 = new GameEngine();
        BoardManager board6 = engine6.getBoard();

        board6.board = new Piece[8][8];
        board6.board[4][4] = new Piece(PieceType.PAWN, PlayerColor.WHITE);
        board6.board[3][5] = new Piece(PieceType.PAWN, PlayerColor.BLACK);

        Move pawnCapture = new Move(new Position(4, 4), new Position(3, 5));
        boolean canCapture = MoveValidator.isValidMove(board6, pawnCapture, PlayerColor.WHITE);

        System.out.println("Pawn ăn chéo: " + canCapture);
        assertTrue(canCapture);

        // ================= TEST 7: Pawn bị chặn =================
        GameEngine engine7 = new GameEngine();
        BoardManager board7 = engine7.getBoard();

        board7.board[5][0] = new Piece(PieceType.PAWN, PlayerColor.BLACK);

        boolean blockedPawn = engine7.makeMove(
                new Move(new Position(6, 0), new Position(5, 0))
        );

        System.out.println("Pawn bị chặn: " + blockedPawn);
        assertFalse(blockedPawn);

        // ================= TEST 8: Bishop bị chặn =================
        GameEngine engine8 = new GameEngine();

        boolean bishopBlocked = engine8.makeMove(
                new Move(new Position(7, 2), new Position(5, 4))
        );

        System.out.println("Bishop bị chặn: " + bishopBlocked);
        assertFalse(bishopBlocked);

        // ================= TEST 9: Queen bị chặn =================
        GameEngine engine9 = new GameEngine();

        boolean queenBlocked = engine9.makeMove(
                new Move(new Position(7, 3), new Position(3, 3))
        );

        System.out.println("Queen bị chặn: " + queenBlocked);
        assertFalse(queenBlocked);

        // ================= TEST 10: Self-check =================
        GameEngine engine10 = new GameEngine();
        BoardManager board10 = engine10.getBoard();

        board10.board = new Piece[8][8];
        board10.board[7][4] = new Piece(PieceType.KING, PlayerColor.WHITE);
        board10.board[0][4] = new Piece(PieceType.ROOK, PlayerColor.BLACK);
        board10.board[6][4] = new Piece(PieceType.ROOK, PlayerColor.WHITE);

        boolean illegalMove = engine10.makeMove(
                new Move(new Position(6, 4), new Position(6, 5))
        );

        System.out.println("Self-check bị chặn: " + illegalMove);
        assertFalse(illegalMove);

        // ================= TEST 11: King không được đi vào ô bị chiếu =================
        GameEngine engine11 = new GameEngine();
        BoardManager board11 = engine11.getBoard();

        board11.board = new Piece[8][8];
        board11.board[7][4] = new Piece(PieceType.KING, PlayerColor.WHITE);
        board11.board[5][4] = new Piece(PieceType.ROOK, PlayerColor.BLACK);

        Move moveIntoCheck = new Move(new Position(7, 4), new Position(6, 4));
        boolean result = engine11.makeMove(moveIntoCheck);

        System.out.println("King đi vào ô bị chiếu: " + result);
        assertFalse("Lỗi: Vua không được đi vào ô bị chiếu", result);

        // ================= TEST 12: Không phải checkmate =================
        GameEngine engine12 = new GameEngine();
        BoardManager board12 = engine12.getBoard();

        board12.board = new Piece[8][8];
        board12.board[0][4] = new Piece(PieceType.KING, PlayerColor.BLACK);
        board12.board[1][4] = new Piece(PieceType.ROOK, PlayerColor.WHITE);

        boolean isMate2 = CheckDetector.isCheckmate(board12, PlayerColor.BLACK);

        System.out.println("Không phải checkmate: " + isMate2);
        assertFalse(isMate2);

        System.out.println("--- TEST HOÀN TẤT ---");
    }
}
