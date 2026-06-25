package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_ID = "player_id";

    private TextView usernameText, eloText, statsText, emptyText;
    private android.widget.ProgressBar progressBar;
    private RecyclerView historyList;
    private int playerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        playerId = getIntent().getIntExtra(EXTRA_PLAYER_ID, -1);

        usernameText = findViewById(R.id.usernameText);
        eloText = findViewById(R.id.eloText);
        statsText = findViewById(R.id.statsText);
        emptyText = findViewById(R.id.emptyText);
        progressBar = findViewById(R.id.progressBar);
        historyList = findViewById(R.id.historyList);
        historyList.setLayoutManager(new LinearLayoutManager(this));

        if (playerId == -1) {
            finish();
            return;
        }

        loadProfile();
        loadHistory();
    }

    private void loadProfile() {
        ApiClient.get("/players/" + playerId + "/profile", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONObject p = response.getJSONObject("data");
                    JSONObject stats = p.getJSONObject("stats");
                    usernameText.setText(p.getString("username"));
                    eloText.setText(p.getInt("elo") + " Elo");
                    statsText.setText(stats.getInt("total") + " parties · "
                            + stats.getInt("wins") + "V / "
                            + stats.getInt("draws") + "N / "
                            + stats.getInt("losses") + "D");
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onError(String message) {
                usernameText.setText("Profil introuvable");
            }
        });
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.get("/players/" + playerId + "/history", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                List<GameHistoryItem> items = new ArrayList<>();
                try {
                    JSONArray games = response.getJSONArray("data");
                    for (int i = 0; i < games.length(); i++) {
                        items.add(toHistoryItem(games.getJSONObject(i)));
                    }
                } catch (Exception ignored) {
                }
                emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                historyList.setAdapter(new GameHistoryAdapter(items));
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }

    private GameHistoryItem toHistoryItem(JSONObject game) throws Exception {
        JSONObject white = game.getJSONObject("white_player");
        JSONObject black = game.isNull("black_player") ? null : game.getJSONObject("black_player");

        boolean isWhite = white.getInt("id") == playerId;
        String opponent = black == null ? "—" : (isWhite ? black.getString("username") : white.getString("username"));

        String result = game.optString("result", null);
        String label;
        int color;
        if ("draw".equals(result)) {
            label = "Match nul";
            color = Color.parseColor("#CCCCCC");
        } else if ((isWhite && "white".equals(result)) || (!isWhite && "black".equals(result))) {
            label = "Victoire";
            color = Color.parseColor("#6BCB77");
        } else {
            label = "Défaite";
            color = Color.parseColor("#FF6B6B");
        }

        return new GameHistoryItem(opponent, label, color, game.optString("finished_at", ""), game.optInt("move_count", 0));
    }
}
