package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_GAME_ID = "game_id";

    private static final long POLL_INTERVAL_MS = 2000;

    private SessionManager session;
    private int gameId;
    private ChessRules rules;

    private boolean myColorWhite;
    private boolean whiteTurn;
    private boolean gameOver;
    private boolean boardBuilt;
    private int lastMoveNumber = 0;
    private int selectedRow = -1, selectedCol = -1;

    private final TextView[][] cells = new TextView[8][8];
    private TextView statusText, messageText, opponentText;
    private Button resignButton, leaveButton;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private boolean pollScheduled = false;
    private final Runnable pollRunnable = this::refreshMoves;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        session = new SessionManager(this);
        gameId = getIntent().getIntExtra(EXTRA_GAME_ID, -1);
        if (gameId == -1 || !session.isLoggedIn()) {
            finish();
            return;
        }

        statusText = findViewById(R.id.statusText);
        messageText = findViewById(R.id.messageText);
        opponentText = findViewById(R.id.opponentText);
        resignButton = findViewById(R.id.resignButton);
        leaveButton = findViewById(R.id.leaveButton);

        resignButton.setOnClickListener(v -> resign());
        leaveButton.setOnClickListener(v -> finish());

        loadGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        schedulePoll();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
        pollScheduled = false;
    }

    private void schedulePoll() {
        if (pollScheduled || gameOver || rules == null) return;
        pollScheduled = true;
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    // Le tableau logique (cells[row][col], et toute la logique de jeu) reste indexé
    // par les coordonnées internes du plateau ; seule la position visuelle dans la
    // grille change pour les Noirs, afin que chacun voie ses propres pièces en bas.
    private void buildBoardUI() {
        boolean flip = !myColorWhite;

        GridLayout grid = findViewById(R.id.boardGrid);
        grid.setColumnCount(8);
        grid.setRowCount(8);

        int cellSize = getResources().getDisplayMetrics().widthPixels / 8;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                TextView cell = new TextView(this);
                int gridRow = flip ? 7 - row : row;
                int gridCol = flip ? 7 - col : col;
                GridLayout.LayoutParams p = new GridLayout.LayoutParams(
                        GridLayout.spec(gridRow), GridLayout.spec(gridCol));
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

    // ─── Chargement / synchronisation ────────────────────────────────────────

    private void loadGame() {
        ApiClient.get("/games/" + gameId, session.getToken(), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isFinishing() || isDestroyed()) return;
                try {
                    JSONObject data = response.getJSONObject("data");
                    applyGameState(data);
                } catch (Exception e) {
                    messageText.setText("Erreur de chargement de la partie");
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                messageText.setText(message);
            }
        });
    }

    private void applyGameState(JSONObject data) throws Exception {
        JSONObject white = data.getJSONObject("white_player");
        JSONObject black = data.isNull("black_player") ? null : data.getJSONObject("black_player");

        int myId = session.getPlayerId();
        myColorWhite = white.getInt("id") == myId;
        String opponentName = myColorWhite
                ? (black != null ? black.getString("username") : "?")
                : white.getString("username");
        opponentText.setText("Vs " + opponentName);

        if (!boardBuilt) {
            buildBoardUI();
            boardBuilt = true;
        }

        rules = new ChessRules();
        JSONArray moves = data.getJSONArray("moves");
        for (int i = 0; i < moves.length(); i++) {
            JSONObject m = moves.getJSONObject(i);
            rules.makeMove(m.getInt("from_row"), m.getInt("from_col"), m.getInt("to_row"), m.getInt("to_col"));
        }
        lastMoveNumber = moves.length();
        whiteTurn = (lastMoveNumber % 2 == 0);
        selectedRow = selectedCol = -1;

        String status = data.getString("status");
        gameOver = "finished".equals(status);

        renderBoard();
        if (gameOver) {
            showResult(data.optString("result", null));
        } else {
            updateStatus();
            schedulePoll();
        }
    }

    private void refreshMoves() {
        pollScheduled = false;
        if (gameOver) return;

        ApiClient.get("/games/" + gameId + "?since=" + lastMoveNumber, session.getToken(), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isFinishing() || isDestroyed()) return;
                try {
                    JSONObject data = response.getJSONObject("data");
                    JSONArray moves = data.getJSONArray("moves");
                    for (int i = 0; i < moves.length(); i++) {
                        JSONObject m = moves.getJSONObject(i);
                        rules.makeMove(m.getInt("from_row"), m.getInt("from_col"), m.getInt("to_row"), m.getInt("to_col"));
                        lastMoveNumber++;
                        whiteTurn = !whiteTurn;
                    }
                    if (moves.length() > 0) {
                        selectedRow = selectedCol = -1;
                        renderBoard();
                    }

                    String status = data.getString("status");
                    if ("finished".equals(status) && !gameOver) {
                        gameOver = true;
                        showResult(data.optString("result", null));
                        return;
                    }
                    updateStatus();
                    schedulePoll();
                } catch (Exception e) {
                    schedulePoll();
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                schedulePoll();
            }
        });
    }

    // ─── Interaction ──────────────────────────────────────────────────────────

    private void onCellClick(int row, int col) {
        if (gameOver || rules == null) return;

        boolean myTurn = (whiteTurn == myColorWhite);
        if (!myTurn) {
            messageText.setText("Ce n'est pas votre tour");
            return;
        }

        int piece = rules.board[row][col];
        boolean isOwnPiece = myColorWhite ? piece > 0 : piece < 0;

        if (selectedRow == -1) {
            if (isOwnPiece) {
                selectedRow = row;
                selectedCol = col;
                messageText.setText("");
            }
        } else if (row == selectedRow && col == selectedCol) {
            selectedRow = selectedCol = -1;
        } else if (isOwnPiece) {
            selectedRow = row;
            selectedCol = col;
            messageText.setText("");
        } else {
            if (rules.isValidMove(selectedRow, selectedCol, row, col, rules.board, true)) {
                int fr = selectedRow, fc = selectedCol;
                selectedRow = selectedCol = -1;
                submitMove(fr, fc, row, col);
            } else {
                messageText.setText("Mouvement interdit !");
                selectedRow = selectedCol = -1;
            }
        }

        renderBoard();
    }

    private void submitMove(int fr, int fc, int tr, int tc) {
        int movingPiece = rules.board[fr][fc];
        boolean isPromotion = Math.abs(movingPiece) == 1 && (tr == 0 || tr == 7);

        rules.makeMove(fr, fc, tr, tc);

        boolean nextIsWhite = !myColorWhite;
        boolean inCheck = rules.isInCheck(nextIsWhite, rules.board);
        boolean hasMove = rules.hasAnyMove(nextIsWhite);
        String result = null;
        if (!hasMove) {
            result = inCheck ? (myColorWhite ? "white" : "black") : "draw";
        }

        whiteTurn = nextIsWhite;
        renderBoard();
        updateStatus();

        try {
            JSONObject body = new JSONObject();
            body.put("from_row", fr);
            body.put("from_col", fc);
            body.put("to_row", tr);
            body.put("to_col", tc);
            if (isPromotion) body.put("promotion", 5);
            if (result != null) body.put("result", result);

            final String finalResult = result;
            ApiClient.post("/games/" + gameId + "/moves", body, session.getToken(), new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (isFinishing() || isDestroyed()) return;
                    lastMoveNumber++;
                    if (finalResult != null) {
                        gameOver = true;
                        showResult(finalResult);
                    }
                }

                @Override
                public void onError(String message) {
                    if (isFinishing() || isDestroyed()) return;
                    messageText.setText("Synchronisation...");
                    loadGame();
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void resign() {
        if (gameOver || rules == null) return;
        ApiClient.post("/games/" + gameId + "/resign", null, session.getToken(), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isFinishing() || isDestroyed()) return;
                gameOver = true;
                showResult(myColorWhite ? "black" : "white");
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                messageText.setText(message);
            }
        });
    }

    // ─── Affichage ────────────────────────────────────────────────────────────

    private void showResult(String result) {
        pollHandler.removeCallbacks(pollRunnable);
        pollScheduled = false;
        statusText.setText("Partie terminée");
        if ("draw".equals(result) || result == null) {
            messageText.setText("Match nul !");
        } else if ((myColorWhite && "white".equals(result)) || (!myColorWhite && "black".equals(result))) {
            messageText.setText("Vous avez gagné ! 🏆");
        } else {
            messageText.setText("Vous avez perdu.");
        }
        resignButton.setVisibility(View.GONE);
        leaveButton.setVisibility(View.VISIBLE);
    }

    private void updateStatus() {
        boolean myTurn = (whiteTurn == myColorWhite);
        statusText.setText(myTurn ? "Votre tour (" + (myColorWhite ? "Blancs" : "Noirs") + ")" : "Tour de l'adversaire");
    }

    private void renderBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean light = (row + col) % 2 == 0;
                int bg;
                if (row == selectedRow && col == selectedCol) {
                    bg = Color.parseColor("#F6F669");
                } else if (selectedRow != -1 && rules.isValidMove(selectedRow, selectedCol, row, col, rules.board, true)) {
                    bg = Color.parseColor("#CDD26A");
                } else if (light) {
                    bg = Color.parseColor("#F0D9B5");
                } else {
                    bg = Color.parseColor("#B58863");
                }
                cells[row][col].setBackgroundColor(bg);

                int piece = rules.board[row][col];
                cells[row][col].setText(ChessRules.pieceSymbol(piece));

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
}
