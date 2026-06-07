package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Pièces : positif = blanc, négatif = noir
    // 1=pion, 2=cavalier, 3=fou, 4=tour, 5=dame, 6=roi
    private int[][] board;
    private TextView[][] cells = new TextView[8][8];

    private boolean whiteTurn;
    private int selectedRow = -1, selectedCol = -1;
    private TextView statusText, messageText;

    // En passant : case cible
    private int enPassantRow, enPassantCol;

    // Droits de roque
    private boolean whiteKingMoved, blackKingMoved;
    private boolean whiteRookAMoved, whiteRookHMoved;
    private boolean blackRookAMoved, blackRookHMoved;

    private boolean gameOver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        messageText = findViewById(R.id.messageText);

        Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(v -> resetGame());

        buildBoardUI();
        resetGame();
    }

    private void buildBoardUI() {
        GridLayout grid = findViewById(R.id.boardGrid);
        grid.setColumnCount(8);
        grid.setRowCount(8);

        int cellSize = getResources().getDisplayMetrics().widthPixels / 8;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams p = new GridLayout.LayoutParams(
                        GridLayout.spec(row), GridLayout.spec(col));
                p.width = cellSize;
                p.height = cellSize;
                cell.setLayoutParams(p);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(TypedValue.COMPLEX_UNIT_PX, cellSize * 0.65f);
                cell.setTypeface(null, Typeface.BOLD);
                final int r = row, c = col;
                cell.setOnClickListener(v -> onCellClick(r, c));
                cells[row][col] = cell;
                grid.addView(cell);
            }
        }
    }

    private void resetGame() {
        board = new int[8][8];
        // Rangée noire
        board[0] = new int[]{-4, -2, -3, -5, -6, -3, -2, -4};
        board[1] = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
        // Rangée blanche
        board[6] = new int[]{1, 1, 1, 1, 1, 1, 1, 1};
        board[7] = new int[]{4, 2, 3, 5, 6, 3, 2, 4};

        whiteTurn = true;
        selectedRow = selectedCol = -1;
        enPassantRow = enPassantCol = -1;
        whiteKingMoved = blackKingMoved = false;
        whiteRookAMoved = whiteRookHMoved = false;
        blackRookAMoved = blackRookHMoved = false;
        gameOver = false;
        messageText.setText("");

        renderBoard();
        updateStatus();
    }

    // ─── Gestion des clics ────────────────────────────────────────────────────

    private void onCellClick(int row, int col) {
        if (gameOver) return;

        int piece = board[row][col];
        boolean isOwnPiece = whiteTurn ? piece > 0 : piece < 0;

        if (selectedRow == -1) {
            // Sélectionner une pièce
            if (isOwnPiece) {
                selectedRow = row;
                selectedCol = col;
                messageText.setText("");
            }
        } else if (row == selectedRow && col == selectedCol) {
            // Désélectionner
            selectedRow = selectedCol = -1;
        } else if (isOwnPiece) {
            // Changer de pièce sélectionnée
            selectedRow = row;
            selectedCol = col;
            messageText.setText("");
        } else {
            // Tenter un déplacement
            if (isValidMove(selectedRow, selectedCol, row, col, board, true)) {
                makeMove(selectedRow, selectedCol, row, col);
                selectedRow = selectedCol = -1;
                whiteTurn = !whiteTurn;
                checkGameState();
            } else {
                messageText.setText("Mouvement interdit !");
                selectedRow = selectedCol = -1;
            }
        }

        renderBoard();
        updateStatus();
    }

    // ─── Exécution d'un coup ──────────────────────────────────────────────────

    private void makeMove(int fr, int fc, int tr, int tc) {
        int piece = board[fr][fc];

        // Prise en passant
        if (Math.abs(piece) == 1 && tc != fc && board[tr][tc] == 0) {
            board[fr][tc] = 0;
        }

        // Mise à jour de la cible en passant
        enPassantRow = enPassantCol = -1;
        if (Math.abs(piece) == 1 && Math.abs(tr - fr) == 2) {
            enPassantRow = (fr + tr) / 2;
            enPassantCol = fc;
        }

        // Roque : déplacer la tour
        if (Math.abs(piece) == 6) {
            if (tc - fc == 2) { board[fr][5] = board[fr][7]; board[fr][7] = 0; }
            else if (fc - tc == 2) { board[fr][3] = board[fr][0]; board[fr][0] = 0; }
        }

        // Suivi des droits de roque
        if (piece == 6) whiteKingMoved = true;
        if (piece == -6) blackKingMoved = true;
        if (fr == 7 && fc == 0) whiteRookAMoved = true;
        if (fr == 7 && fc == 7) whiteRookHMoved = true;
        if (fr == 0 && fc == 0) blackRookAMoved = true;
        if (fr == 0 && fc == 7) blackRookHMoved = true;

        board[tr][tc] = piece;
        board[fr][fc] = 0;

        // Promotion automatique en dame
        if (piece == 1 && tr == 0) board[tr][tc] = 5;
        if (piece == -1 && tr == 7) board[tr][tc] = -5;
    }

    // ─── Validation des coups ─────────────────────────────────────────────────

    private boolean isValidMove(int fr, int fc, int tr, int tc, int[][] b, boolean checkSafety) {
        if (tr < 0 || tr > 7 || tc < 0 || tc > 7) return false;
        int piece = b[fr][fc];
        int target = b[tr][tc];
        if (piece == 0) return false;
        if (piece > 0 && target > 0) return false; // capture pièce alliée
        if (piece < 0 && target < 0) return false;

        boolean raw;
        switch (Math.abs(piece)) {
            case 1: raw = validPawn(fr, fc, tr, tc, piece, b); break;
            case 2: raw = validKnight(fr, fc, tr, tc); break;
            case 3: raw = validBishop(fr, fc, tr, tc, b); break;
            case 4: raw = validRook(fr, fc, tr, tc, b); break;
            case 5: raw = validBishop(fr, fc, tr, tc, b) || validRook(fr, fc, tr, tc, b); break;
            case 6: raw = validKing(fr, fc, tr, tc, piece, b); break;
            default: return false;
        }
        if (!raw) return false;

        // Vérifier que le coup ne laisse pas le roi en échec
        if (checkSafety) {
            int[][] copy = copyBoard(b);
            applyMove(copy, fr, fc, tr, tc);
            if (isInCheck(piece > 0, copy)) return false;
        }
        return true;
    }

    private boolean validPawn(int fr, int fc, int tr, int tc, int piece, int[][] b) {
        int dir = piece > 0 ? -1 : 1;
        int startRow = piece > 0 ? 6 : 1;

        // Avancer d'une case
        if (tc == fc && tr == fr + dir && b[tr][tc] == 0) return true;
        // Avancer de deux cases depuis la position initiale
        if (tc == fc && fr == startRow && tr == fr + 2 * dir
                && b[fr + dir][fc] == 0 && b[tr][tc] == 0) return true;
        // Capture en diagonale
        if (Math.abs(tc - fc) == 1 && tr == fr + dir) {
            if (piece > 0 && b[tr][tc] < 0) return true;
            if (piece < 0 && b[tr][tc] > 0) return true;
            // En passant
            if (tr == enPassantRow && tc == enPassantCol) return true;
        }
        return false;
    }

    private boolean validKnight(int fr, int fc, int tr, int tc) {
        int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    private boolean validBishop(int fr, int fc, int tr, int tc, int[][] b) {
        if (tr == fr || Math.abs(tr - fr) != Math.abs(tc - fc)) return false;
        return pathClear(fr, fc, tr, tc, b);
    }

    private boolean validRook(int fr, int fc, int tr, int tc, int[][] b) {
        if (fr == tr && fc == tc) return false;
        if (fr != tr && fc != tc) return false;
        return pathClear(fr, fc, tr, tc, b);
    }

    private boolean validKing(int fr, int fc, int tr, int tc, int piece, int[][] b) {
        int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
        if (dr <= 1 && dc <= 1 && dr + dc > 0) return true;

        // Roque
        boolean white = piece > 0;
        if (dr == 0 && dc == 2) {
            if (tc > fc) { // Côté roi
                if (white && !whiteKingMoved && !whiteRookHMoved && fr == 7
                        && b[7][5] == 0 && b[7][6] == 0
                        && !attacked(7, 4, false, b) && !attacked(7, 5, false, b) && !attacked(7, 6, false, b))
                    return true;
                if (!white && !blackKingMoved && !blackRookHMoved && fr == 0
                        && b[0][5] == 0 && b[0][6] == 0
                        && !attacked(0, 4, true, b) && !attacked(0, 5, true, b) && !attacked(0, 6, true, b))
                    return true;
            } else { // Côté dame
                if (white && !whiteKingMoved && !whiteRookAMoved && fr == 7
                        && b[7][1] == 0 && b[7][2] == 0 && b[7][3] == 0
                        && !attacked(7, 4, false, b) && !attacked(7, 3, false, b) && !attacked(7, 2, false, b))
                    return true;
                if (!white && !blackKingMoved && !blackRookAMoved && fr == 0
                        && b[0][1] == 0 && b[0][2] == 0 && b[0][3] == 0
                        && !attacked(0, 4, true, b) && !attacked(0, 3, true, b) && !attacked(0, 2, true, b))
                    return true;
            }
        }
        return false;
    }

    private boolean pathClear(int fr, int fc, int tr, int tc, int[][] b) {
        int dr = Integer.signum(tr - fr), dc = Integer.signum(tc - fc);
        int r = fr + dr, c = fc + dc;
        while (r != tr || c != tc) {
            if (b[r][c] != 0) return false;
            r += dr; c += dc;
        }
        return true;
    }

    // ─── Détection d'échec ────────────────────────────────────────────────────

    private boolean isInCheck(boolean white, int[][] b) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (b[r][c] == (white ? 6 : -6))
                    return attacked(r, c, !white, b);
        return false;
    }

    private boolean attacked(int row, int col, boolean byWhite, int[][] b) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                int p = b[r][c];
                if ((byWhite && p > 0) || (!byWhite && p < 0))
                    if (canAttack(r, c, row, col, b)) return true;
            }
        return false;
    }

    private boolean canAttack(int fr, int fc, int tr, int tc, int[][] b) {
        int p = b[fr][fc];
        switch (Math.abs(p)) {
            case 1: {
                int dir = p > 0 ? -1 : 1;
                return Math.abs(tc - fc) == 1 && tr == fr + dir;
            }
            case 2: return validKnight(fr, fc, tr, tc);
            case 3: return validBishop(fr, fc, tr, tc, b);
            case 4: return validRook(fr, fc, tr, tc, b);
            case 5: return validBishop(fr, fc, tr, tc, b) || validRook(fr, fc, tr, tc, b);
            case 6: {
                int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
                return dr <= 1 && dc <= 1 && dr + dc > 0;
            }
        }
        return false;
    }

    // ─── Fin de partie ────────────────────────────────────────────────────────

    private boolean hasAnyMove(boolean white) {
        for (int fr = 0; fr < 8; fr++)
            for (int fc = 0; fc < 8; fc++) {
                int p = board[fr][fc];
                if ((white && p > 0) || (!white && p < 0))
                    for (int tr = 0; tr < 8; tr++)
                        for (int tc = 0; tc < 8; tc++)
                            if (isValidMove(fr, fc, tr, tc, board, true)) return true;
            }
        return false;
    }

    private void checkGameState() {
        boolean inCheck = isInCheck(whiteTurn, board);
        boolean hasMove = hasAnyMove(whiteTurn);

        if (!hasMove) {
            gameOver = true;
            if (inCheck)
                messageText.setText((whiteTurn ? "Noir" : "Blanc") + " gagne ! Échec et mat ! 🏆");
            else
                messageText.setText("Pat ! Match nul !");
        } else if (inCheck) {
            messageText.setText("Échec !");
        }
    }

    // ─── Copie et simulation du plateau ──────────────────────────────────────

    private void applyMove(int[][] b, int fr, int fc, int tr, int tc) {
        int piece = b[fr][fc];
        if (Math.abs(piece) == 1 && tc != fc && b[tr][tc] == 0) b[fr][tc] = 0;
        if (Math.abs(piece) == 6) {
            if (tc - fc == 2) { b[fr][5] = b[fr][7]; b[fr][7] = 0; }
            else if (fc - tc == 2) { b[fr][3] = b[fr][0]; b[fr][0] = 0; }
        }
        b[tr][tc] = piece;
        b[fr][fc] = 0;
        if (piece == 1 && tr == 0) b[tr][tc] = 5;
        if (piece == -1 && tr == 7) b[tr][tc] = -5;
    }

    private int[][] copyBoard(int[][] b) {
        int[][] c = new int[8][8];
        for (int i = 0; i < 8; i++) c[i] = b[i].clone();
        return c;
    }

    // ─── Affichage ────────────────────────────────────────────────────────────

    private void renderBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean light = (row + col) % 2 == 0;
                int bg;
                if (row == selectedRow && col == selectedCol) {
                    bg = Color.parseColor("#F6F669");
                } else if (selectedRow != -1 && isValidMove(selectedRow, selectedCol, row, col, board, true)) {
                    bg = Color.parseColor("#CDD26A");
                } else if (light) {
                    bg = Color.parseColor("#F0D9B5");
                } else {
                    bg = Color.parseColor("#B58863");
                }
                cells[row][col].setBackgroundColor(bg);

                int piece = board[row][col];
                cells[row][col].setText(pieceSymbol(piece));

                if (piece > 0) {
                    cells[row][col].setTextColor(Color.WHITE);
                    cells[row][col].setShadowLayer(4f, 0f, 0f, Color.parseColor("#333333"));
                } else if (piece < 0) {
                    cells[row][col].setTextColor(Color.parseColor("#1A1A1A"));
                    cells[row][col].setShadowLayer(3f, 0f, 0f, Color.parseColor("#BBBBBB"));
                } else {
                    cells[row][col].setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
                }
            }
        }
    }

    private String pieceSymbol(int piece) {
        switch (piece) {
            case  1: return "♙"; case  2: return "♘"; case  3: return "♗";
            case  4: return "♖"; case  5: return "♕"; case  6: return "♔";
            case -1: return "♟"; case -2: return "♞"; case -3: return "♝";
            case -4: return "♜"; case -5: return "♛"; case -6: return "♚";
            default: return "";
        }
    }

    private void updateStatus() {
        if (!gameOver)
            statusText.setText(whiteTurn ? "Tour des Blancs  ♙" : "Tour des Noirs  ♟");
    }
}
