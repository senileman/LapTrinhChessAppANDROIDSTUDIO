package com.teamFive.chessapp.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.teamFive.chessapp.engine.BoardManager;
import com.teamFive.chessapp.engine.MoveValidator;
import com.teamFive.chessapp.model.Move;
import com.teamFive.chessapp.model.Piece;
import com.teamFive.chessapp.model.PlayerColor;
import com.teamFive.chessapp.model.Position;

import java.util.HashSet;
import java.util.Set;

public class ChessBoardAdapter extends BaseAdapter {

    // ----------------------------------------------------------------
    //  Colors
    // ----------------------------------------------------------------
    private static final int COLOR_LIGHT      = 0xFFF0D9B5;
    private static final int COLOR_DARK       = 0xFFB58863;
    private static final int COLOR_SELECTED   = 0xFFF6F669;
    private static final int COLOR_VALID_MOVE = 0x6000C853;
    private static final int COLOR_CAPTURE    = 0x60FF5252;
    private static final int COLOR_CHECK      = 0xAAFF1744;
    private static final int COLOR_HINT_FROM  = 0xCC7EC8F4;
    private static final int COLOR_HINT_TO    = 0xCC2196F3;

    // ----------------------------------------------------------------
    //  State
    // ----------------------------------------------------------------
    private final Context      context;
    private       BoardManager boardManager;
    private       PlayerColor  currentTurn;

    private int selectedIndex = -1;
    private final Set<Integer> validMoveIndices = new HashSet<>();
    private final Set<Integer> captureIndices   = new HashSet<>();
    private int checkKingIndex = -1;

    private int hintFromIndex = -1;
    private int hintToIndex   = -1;

    // Cached cell size so every getView call uses the same value
    private int cellSize = 0;

    // ----------------------------------------------------------------
    //  Callback
    // ----------------------------------------------------------------
    public interface OnMoveSelectedListener {
        void onMoveSelected(Position from, Position to);
    }
    private OnMoveSelectedListener moveListener;
    public void setOnMoveSelectedListener(OnMoveSelectedListener l) {
        this.moveListener = l;
    }

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public ChessBoardAdapter(Context context, BoardManager boardManager, PlayerColor currentTurn) {
        this.context      = context;
        this.boardManager = boardManager;
        this.currentTurn  = currentTurn;
    }

    // ----------------------------------------------------------------
    //  Public API
    // ----------------------------------------------------------------
    public void update(BoardManager boardManager, PlayerColor currentTurn, int checkKingIndex) {
        this.boardManager   = boardManager;
        this.currentTurn    = currentTurn;
        this.checkKingIndex = checkKingIndex;
        clearSelection();
    }

    public void clearSelection() {
        selectedIndex = -1;
        validMoveIndices.clear();
        captureIndices.clear();
        clearHint();
        notifyDataSetChanged();
    }

    public void showHint(Position from, Position to) {
        selectedIndex = -1;
        validMoveIndices.clear();
        captureIndices.clear();
        hintFromIndex = from.row * 8 + from.col;
        hintToIndex   = to.row   * 8 + to.col;
        notifyDataSetChanged();
    }

    private void clearHint() {
        hintFromIndex = -1;
        hintToIndex   = -1;
    }

    // ----------------------------------------------------------------
    //  BaseAdapter
    // ----------------------------------------------------------------
    @Override public int    getCount()         { return 64; }
    @Override public Object getItem(int pos)   { return pos; }
    @Override public long   getItemId(int pos) { return pos; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final CellView cell;
        if (convertView instanceof CellView) {
            cell = (CellView) convertView;
        } else {
            cell = new CellView(context);
        }

        // Compute cell size once from the parent GridView's width.
        // The GridView has no padding (fixed in layout), so parentWidth / 8
        // gives perfectly square cells that tile the full board.
        if (cellSize == 0 && parent.getWidth() > 0) {
            cellSize = parent.getWidth() / 8;
        }

        if (cellSize > 0) {
            ViewGroup.LayoutParams lp = cell.getLayoutParams();
            if (lp == null) {
                lp = new ViewGroup.LayoutParams(cellSize, cellSize);
            } else {
                lp.width  = cellSize;
                lp.height = cellSize;
            }
            cell.setLayoutParams(lp);
            cell.updateTextSize(cellSize);
        }

        final int row = position / 8;
        final int col = position % 8;

        // Background — priority: check > selected > hint > normal
        final boolean isLight = (row + col) % 2 == 0;
        final int bgColor;
        if (position == checkKingIndex) {
            bgColor = COLOR_CHECK;
        } else if (position == selectedIndex) {
            bgColor = COLOR_SELECTED;
        } else if (position == hintFromIndex) {
            bgColor = COLOR_HINT_FROM;
        } else if (position == hintToIndex) {
            bgColor = COLOR_HINT_TO;
        } else {
            bgColor = isLight ? COLOR_LIGHT : COLOR_DARK;
        }
        cell.setBackgroundColor(bgColor);

        // Piece
        final Piece piece = boardManager.board[row][col];
        if (piece != null) {
            cell.tvPiece.setText(getPieceUnicode(piece));
            cell.tvPiece.setTextColor(
                    piece.color == PlayerColor.WHITE ? Color.WHITE : Color.BLACK
            );
            cell.tvPiece.setVisibility(View.VISIBLE);
        } else {
            cell.tvPiece.setText("");
            cell.tvPiece.setVisibility(View.INVISIBLE);
        }

        // Valid-move overlay
        if (validMoveIndices.contains(position)) {
            cell.viewDot.setBackgroundColor(COLOR_VALID_MOVE);
            cell.viewDot.setVisibility(View.VISIBLE);
        } else if (captureIndices.contains(position)) {
            cell.viewDot.setBackgroundColor(COLOR_CAPTURE);
            cell.viewDot.setVisibility(View.VISIBLE);
        } else {
            cell.viewDot.setVisibility(View.GONE);
        }

        cell.setOnClickListener(v -> handleCellClick(position, row, col));

        return cell;
    }

    // ----------------------------------------------------------------
    //  Click handling
    // ----------------------------------------------------------------
    private void handleCellClick(int index, int row, int col) {
        if (hintFromIndex != -1 || hintToIndex != -1) {
            clearHint();
            notifyDataSetChanged();
        }

        final Piece piece = boardManager.board[row][col];

        if (selectedIndex == -1) {
            if (piece != null && piece.color == currentTurn) {
                selectedIndex = index;
                computeValidMoves(row, col);
                notifyDataSetChanged();
            }
        } else {
            if (index == selectedIndex) {
                clearSelection();
                return;
            }
            if (piece != null && piece.color == currentTurn) {
                selectedIndex = index;
                computeValidMoves(row, col);
                notifyDataSetChanged();
                return;
            }
            if (validMoveIndices.contains(index) || captureIndices.contains(index)) {
                final int fromRow = selectedIndex / 8;
                final int fromCol = selectedIndex % 8;
                final Position from = new Position(fromRow, fromCol);
                final Position to   = new Position(row, col);
                clearSelection();
                if (moveListener != null) {
                    moveListener.onMoveSelected(from, to);
                }
            } else {
                clearSelection();
            }
        }
    }

    // ----------------------------------------------------------------
    //  Valid move computation
    // ----------------------------------------------------------------
    private void computeValidMoves(int fromRow, int fromCol) {
        validMoveIndices.clear();
        captureIndices.clear();
        final Position from = new Position(fromRow, fromCol);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                final Move move = new Move(from, new Position(r, c));
                if (MoveValidator.isValidMove(boardManager, move, currentTurn)) {
                    final int idx = r * 8 + c;
                    final Piece target = boardManager.board[r][c];
                    if (target != null && target.color != currentTurn) {
                        captureIndices.add(idx);
                    } else {
                        validMoveIndices.add(idx);
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------
    //  Unicode pieces
    // ----------------------------------------------------------------
    private String getPieceUnicode(Piece piece) {
        if (piece.color == PlayerColor.WHITE) {
            switch (piece.type) {
                case KING:   return "\u2654";
                case QUEEN:  return "\u2655";
                case ROOK:   return "\u2656";
                case BISHOP: return "\u2657";
                case KNIGHT: return "\u2658";
                case PAWN:   return "\u2659";
                default:     return "";
            }
        } else {
            switch (piece.type) {
                case KING:   return "\u265A";
                case QUEEN:  return "\u265B";
                case ROOK:   return "\u265C";
                case BISHOP: return "\u265D";
                case KNIGHT: return "\u265E";
                case PAWN:   return "\u265F";
                default:     return "";
            }
        }
    }

    // ================================================================
    //  CellView
    // ================================================================
    static class CellView extends FrameLayout {

        final TextView tvPiece;
        final View     viewDot;

        CellView(Context context) {
            super(context);

            tvPiece = new TextView(context);
            tvPiece.setGravity(Gravity.CENTER);
            tvPiece.setTypeface(Typeface.DEFAULT_BOLD);
            tvPiece.setShadowLayer(3f, 1f, 1f, 0xFF000000);
            addView(tvPiece, new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            viewDot = new View(context);
            addView(viewDot, new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
        }

        void updateTextSize(int cellSizePx) {
            if (cellSizePx > 0) {
                tvPiece.setTextSize(TypedValue.COMPLEX_UNIT_PX, cellSizePx * 0.72f);
            }
        }
    }
}
