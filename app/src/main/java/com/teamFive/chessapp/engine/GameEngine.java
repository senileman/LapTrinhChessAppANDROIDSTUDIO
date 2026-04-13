package com.teamFive.chessapp.engine;

import com.teamFive.chessapp.model.Move;
import com.teamFive.chessapp.model.Piece;
import com.teamFive.chessapp.model.PlayerColor;
import com.teamFive.chessapp.model.Position;

/**
 * GameEngine with "Really Bad Chess" random boards.
 * Accepts optional piece-value targets for each side so the caller
 * (GameActivity / tests) can control board composition.
 */
public class GameEngine {

    private BoardManager board;
    private PlayerColor  turn       = PlayerColor.WHITE;
    private boolean      isGameOver = false;

    private final PlayerColor humanColor = PlayerColor.WHITE;
    private final PlayerColor aiColor    = PlayerColor.BLACK;

    /** Piece-value targets preserved across reset() calls. */
    private final int whiteTarget;
    private final int blackTarget;

    // ----------------------------------------------------------------
    //  Constructors
    // ----------------------------------------------------------------

    /** Balanced board using the default target value for both sides. */
    public GameEngine() {
        this(RandomBoardGenerator.DEFAULT_TARGET, RandomBoardGenerator.DEFAULT_TARGET);
    }


    public GameEngine(int whiteTarget, int blackTarget) {
        this.whiteTarget = whiteTarget;
        this.blackTarget = blackTarget;
        board = new BoardManager();
        board.board = RandomBoardGenerator.generate(whiteTarget, blackTarget);
    }

    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------
    public BoardManager getBoard()      { return board; }
    public PlayerColor  getTurn()       { return turn; }
    public boolean      isGameOver()    { return isGameOver; }
    public PlayerColor  getHumanColor() { return humanColor; }
    public PlayerColor  getAiColor()    { return aiColor; }

    // ----------------------------------------------------------------
    //  Human move
    // ----------------------------------------------------------------
    public boolean makeMove(Move move) {
        if (isGameOver)            return false;
        if (turn != humanColor)    return false;

        if (!MoveValidator.isValidMove(board, move, turn)) return false;

        Piece captured = board.getPiece(move.to);
        board.movePiece(move);

        if (CheckDetector.isKingInCheck(board, turn)) {
            board.movePiece(new Move(move.to, move.from));
            board.board[move.to.row][move.to.col] = captured;
            return false;
        }

        handlePromotion(move);
        turn = aiColor;

        if (CheckDetector.isCheckmate(board, turn)) {
            isGameOver = true;
        }
        return true;
    }

    // ----------------------------------------------------------------
    //  AI move
    // ----------------------------------------------------------------
    public Move makeAiMove() {
        if (isGameOver)        return null;
        if (turn != aiColor)   return null;

        Move aiMove = AIPlayer.getBestMove(board, aiColor);
        if (aiMove == null) {
            isGameOver = true;
            return null;
        }

        Piece captured = board.getPiece(aiMove.to);
        board.movePiece(aiMove);

        if (CheckDetector.isKingInCheck(board, aiColor)) {
            board.movePiece(new Move(aiMove.to, aiMove.from));
            board.board[aiMove.to.row][aiMove.to.col] = captured;
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
    //  Pawn promotion (auto-queen)
    // ----------------------------------------------------------------
    private void handlePromotion(Move move) {
        Piece piece = board.board[move.to.row][move.to.col];
        if (piece == null || piece.type != com.teamFive.chessapp.model.PieceType.PAWN) return;

        boolean whitePromotes = (piece.color == PlayerColor.WHITE && move.to.row == 0);
        boolean blackPromotes = (piece.color == PlayerColor.BLACK && move.to.row == 7);

        if (whitePromotes || blackPromotes) {
            board.board[move.to.row][move.to.col] =
                    new Piece(com.teamFive.chessapp.model.PieceType.QUEEN, piece.color);
        }
    }

    // ----------------------------------------------------------------
    //  Reset — reuses same targets for a rematch
    // ----------------------------------------------------------------
    public void reset() {
        board = new BoardManager();
        board.board = RandomBoardGenerator.generate(whiteTarget, blackTarget);
        turn       = humanColor;
        isGameOver = false;
    }
}
