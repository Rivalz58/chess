package com.example.myapplication;

// Pièces : positif = blanc, négatif = noir
// 1=pion, 2=cavalier, 3=fou, 4=tour, 5=dame, 6=roi
//
// Copie instanciable du moteur de règles de MainActivity, pour GameActivity
// (une partie en ligne a besoin de sa propre instance/état, contrairement à
// l'écran local qui reste inchangé).
public class ChessRules {

    public int[][] board;

    private int enPassantRow = -1, enPassantCol = -1;
    private boolean whiteKingMoved, blackKingMoved;
    private boolean whiteRookAMoved, whiteRookHMoved;
    private boolean blackRookAMoved, blackRookHMoved;

    public ChessRules() {
        board = new int[8][8];
        board[0] = new int[]{-4, -2, -3, -5, -6, -3, -2, -4};
        board[1] = new int[]{-1, -1, -1, -1, -1, -1, -1, -1};
        board[6] = new int[]{1, 1, 1, 1, 1, 1, 1, 1};
        board[7] = new int[]{4, 2, 3, 5, 6, 3, 2, 4};
    }

    public boolean isValidMove(int fr, int fc, int tr, int tc, int[][] b, boolean checkSafety) {
        if (tr < 0 || tr > 7 || tc < 0 || tc > 7) return false;
        int piece = b[fr][fc];
        int target = b[tr][tc];
        if (piece == 0) return false;
        if (piece > 0 && target > 0) return false;
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

        if (tc == fc && tr == fr + dir && b[tr][tc] == 0) return true;
        if (tc == fc && fr == startRow && tr == fr + 2 * dir
                && b[fr + dir][fc] == 0 && b[tr][tc] == 0) return true;
        if (Math.abs(tc - fc) == 1 && tr == fr + dir) {
            if (piece > 0 && b[tr][tc] < 0) return true;
            if (piece < 0 && b[tr][tc] > 0) return true;
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

        boolean white = piece > 0;
        if (dr == 0 && dc == 2) {
            if (tc > fc) {
                if (white && !whiteKingMoved && !whiteRookHMoved && fr == 7
                        && b[7][5] == 0 && b[7][6] == 0
                        && !attacked(7, 4, false, b) && !attacked(7, 5, false, b) && !attacked(7, 6, false, b))
                    return true;
                if (!white && !blackKingMoved && !blackRookHMoved && fr == 0
                        && b[0][5] == 0 && b[0][6] == 0
                        && !attacked(0, 4, true, b) && !attacked(0, 5, true, b) && !attacked(0, 6, true, b))
                    return true;
            } else {
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

    public boolean isInCheck(boolean white, int[][] b) {
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

    public boolean hasAnyMove(boolean white) {
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

    // Applique le coup sur le plateau réel de l'instance (met à jour roque/en-passant).
    public void makeMove(int fr, int fc, int tr, int tc) {
        int piece = board[fr][fc];

        if (Math.abs(piece) == 1 && tc != fc && board[tr][tc] == 0) {
            board[fr][tc] = 0;
        }

        enPassantRow = enPassantCol = -1;
        if (Math.abs(piece) == 1 && Math.abs(tr - fr) == 2) {
            enPassantRow = (fr + tr) / 2;
            enPassantCol = fc;
        }

        if (Math.abs(piece) == 6) {
            if (tc - fc == 2) { board[fr][5] = board[fr][7]; board[fr][7] = 0; }
            else if (fc - tc == 2) { board[fr][3] = board[fr][0]; board[fr][0] = 0; }
        }

        if (piece == 6) whiteKingMoved = true;
        if (piece == -6) blackKingMoved = true;
        if (fr == 7 && fc == 0) whiteRookAMoved = true;
        if (fr == 7 && fc == 7) whiteRookHMoved = true;
        if (fr == 0 && fc == 0) blackRookAMoved = true;
        if (fr == 0 && fc == 7) blackRookHMoved = true;

        board[tr][tc] = piece;
        board[fr][fc] = 0;

        if (piece == 1 && tr == 0) board[tr][tc] = 5;
        if (piece == -1 && tr == 7) board[tr][tc] = -5;
    }

    // Simule un coup sur une copie de plateau (pour la sécurité du roi), sans toucher
    // aux droits de roque/en-passant de l'instance.
    public void applyMove(int[][] b, int fr, int fc, int tr, int tc) {
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

    public int[][] copyBoard(int[][] b) {
        int[][] c = new int[8][8];
        for (int i = 0; i < 8; i++) c[i] = b[i].clone();
        return c;
    }

    public static String pieceSymbol(int piece) {
        switch (piece) {
            case  1: return "♙"; case  2: return "♘"; case  3: return "♗";
            case  4: return "♖"; case  5: return "♕"; case  6: return "♔";
            case -1: return "♟"; case -2: return "♞"; case -3: return "♝";
            case -4: return "♜"; case -5: return "♛"; case -6: return "♚";
            default: return "";
        }
    }
}
