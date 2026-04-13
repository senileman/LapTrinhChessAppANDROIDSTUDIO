package com.teamFive.chessapp.engine;

import com.teamFive.chessapp.model.Move;
import com.teamFive.chessapp.model.Piece;
import com.teamFive.chessapp.model.PlayerColor;
import com.teamFive.chessapp.model.Position;

/**
 * GameEngine với chế độ:
 *  - Bàn cờ ngẫu nhiên kiểu "Really Bad Chess"
 *  - Người (WHITE) đấu với AI (BLACK)
 */
public class GameEngine {

    private BoardManager board;
    private PlayerColor  turn       = PlayerColor.WHITE;
    private boolean      isGameOver = false;

    // Màu của người chơi thật (AI luôn là màu kia)
    private final PlayerColor humanColor = PlayerColor.WHITE;
    private final PlayerColor aiColor    = PlayerColor.BLACK;

    public GameEngine() {
        board = new BoardManager();
        // Dùng bàn ngẫu nhiên thay vì initialize() chuẩn
        board.board = RandomBoardGenerator.generate();
    }

    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------
    public BoardManager getBoard()    { return board; }
    public PlayerColor  getTurn()     { return turn; }
    public boolean      isGameOver()  { return isGameOver; }
    public PlayerColor  getHumanColor() { return humanColor; }
    public PlayerColor  getAiColor()    { return aiColor; }

    // ----------------------------------------------------------------
    //  Người chơi thực hiện nước đi
    // ----------------------------------------------------------------
    public boolean makeMove(Move move) {
        if (isGameOver) return false;
        if (turn != humanColor) return false; // Chặn nếu đang là lượt AI

        if (!MoveValidator.isValidMove(board, move, turn)) return false;

        Piece captured = board.getPiece(move.to);
        board.movePiece(move);

        // Kiểm tra Vua bị chiếu sau nước đi
        if (CheckDetector.isKingInCheck(board, turn)) {
            // Hoàn tác
            board.movePiece(new Move(move.to, move.from));
            board.board[move.to.row][move.to.col] = captured;
            return false;
        }

        // Xử lý phong cấp Pawn
        handlePromotion(move);

        // Chuyển lượt
        turn = aiColor;

        // Kiểm tra kết thúc
        if (CheckDetector.isCheckmate(board, turn)) {
            isGameOver = true;
        }

        return true;
    }

    /**
     * AI thực hiện nước đi — gọi từ GameActivity sau khi người chơi đi xong.
     * @return nước AI vừa đi, hoặc null nếu AI không có nước hợp lệ
     */
    public Move makeAiMove() {
        if (isGameOver) return null;
        if (turn != aiColor) return null;

        Move aiMove = AIPlayer.getBestMove(board, aiColor);
        if (aiMove == null) {
            // AI không có nước → stalemate hoặc thua
            isGameOver = true;
            return null;
        }

        Piece captured = board.getPiece(aiMove.to);
        board.movePiece(aiMove);

        // Kiểm tra an toàn (phòng trường hợp AI tự chiếu)
        if (CheckDetector.isKingInCheck(board, aiColor)) {
            board.movePiece(new Move(aiMove.to, aiMove.from));
            board.board[aiMove.to.row][aiMove.to.col] = captured;
            // Tìm nước khác (trường hợp hiếm, bỏ qua lượt)
            turn = humanColor;
            return null;
        }

        handlePromotion(aiMove);

        turn = humanColor;

        if (CheckDetector.isCheckmate(board, turn)) {
            isGameOver = true;
        }

        return aiMove;
    }

    // ----------------------------------------------------------------
    //  Phong cấp Pawn tự động thành Queen
    // ----------------------------------------------------------------
    private void handlePromotion(Move move) {
        Piece piece = board.board[move.to.row][move.to.col];
        if (piece == null) return;
        if (piece.type != com.teamFive.chessapp.model.PieceType.PAWN) return;

        boolean whitePromotes = (piece.color == PlayerColor.WHITE && move.to.row == 0);
        boolean blackPromotes = (piece.color == PlayerColor.BLACK && move.to.row == 7);

        if (whitePromotes || blackPromotes) {
            board.board[move.to.row][move.to.col] =
                    new Piece(com.teamFive.chessapp.model.PieceType.QUEEN, piece.color);
        }
    }

    // ----------------------------------------------------------------
    //  Reset ván mới (bàn ngẫu nhiên mới)
    // ----------------------------------------------------------------
    public void reset() {
        board      = new BoardManager();
        board.board = RandomBoardGenerator.generate();
        turn       = humanColor;
        isGameOver = false;
    }
}
