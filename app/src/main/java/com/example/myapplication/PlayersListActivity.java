package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlayersListActivity extends AppCompatActivity {

    private static final long WAIT_POLL_INTERVAL_MS = 2000;

    private ProgressBar progressBar;
    private TextView emptyText;
    private RecyclerView playersList;
    private SessionManager session;
    private ChallengePoller challengePoller;

    private final Handler waitHandler = new Handler(Looper.getMainLooper());
    private Runnable waitRunnable;
    private AlertDialog waitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_players_list);

        session = new SessionManager(this);

        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        playersList = findViewById(R.id.playersList);
        playersList.setLayoutManager(new LinearLayoutManager(this));

        challengePoller = new ChallengePoller(this, session, gameId -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_GAME_ID, gameId);
            startActivity(intent);
        });

        loadPlayers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        challengePoller.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        challengePoller.stop();
        waitHandler.removeCallbacksAndMessages(null);
    }

    private void loadPlayers() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.get("/players", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                List<PlayerItem> items = new ArrayList<>();
                try {
                    JSONArray players = response.getJSONArray("data");
                    for (int i = 0; i < players.length(); i++) {
                        JSONObject p = players.getJSONObject(i);
                        items.add(new PlayerItem(p.getInt("id"), p.getString("username"), p.getInt("elo")));
                    }
                } catch (Exception ignored) {
                }
                emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                playersList.setAdapter(new PlayerAdapter(items, session.getPlayerId(),
                        player -> {
                            Intent intent = new Intent(PlayersListActivity.this, ProfileActivity.class);
                            intent.putExtra(ProfileActivity.EXTRA_PLAYER_ID, player.id);
                            startActivity(intent);
                        },
                        PlayersListActivity.this::sendChallenge));
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void sendChallenge(PlayerItem player) {
        try {
            JSONObject body = new JSONObject();
            body.put("challenged_id", player.id);
            ApiClient.post("/challenges", body, session.getToken(), new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        int challengeId = response.getJSONObject("data").getInt("id");
                        showWaitingDialog(challengeId, player.username);
                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(PlayersListActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void showWaitingDialog(int challengeId, String opponentName) {
        waitDialog = new AlertDialog.Builder(this)
                .setTitle("En attente")
                .setMessage("En attente de la réponse de " + opponentName + "...")
                .setCancelable(true)
                .setNegativeButton("Fermer", (d, w) -> {})
                .show();
        waitDialog.setOnDismissListener(d -> waitHandler.removeCallbacks(waitRunnable));

        waitRunnable = () -> pollChallengeStatus(challengeId, opponentName);
        waitHandler.postDelayed(waitRunnable, WAIT_POLL_INTERVAL_MS);
    }

    private void pollChallengeStatus(int challengeId, String opponentName) {
        if (isFinishing() || isDestroyed()) return;

        ApiClient.get("/challenges/" + challengeId, session.getToken(), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isFinishing() || isDestroyed()) return;
                try {
                    JSONObject data = response.getJSONObject("data");
                    String status = data.getString("status");
                    if ("accepted".equals(status)) {
                        if (waitDialog != null) waitDialog.dismiss();
                        int gameId = data.getInt("game_id");
                        Intent intent = new Intent(PlayersListActivity.this, GameActivity.class);
                        intent.putExtra(GameActivity.EXTRA_GAME_ID, gameId);
                        startActivity(intent);
                    } else if ("declined".equals(status)) {
                        if (waitDialog != null) waitDialog.dismiss();
                        Toast.makeText(PlayersListActivity.this, opponentName + " a refusé le défi.", Toast.LENGTH_SHORT).show();
                    } else {
                        waitHandler.postDelayed(waitRunnable, WAIT_POLL_INTERVAL_MS);
                    }
                } catch (Exception e) {
                    waitHandler.postDelayed(waitRunnable, WAIT_POLL_INTERVAL_MS);
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                waitHandler.postDelayed(waitRunnable, WAIT_POLL_INTERVAL_MS);
            }
        });
    }
}
