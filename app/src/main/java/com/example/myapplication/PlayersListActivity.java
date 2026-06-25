package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlayersListActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView emptyText;
    private RecyclerView playersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_players_list);

        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        playersList = findViewById(R.id.playersList);
        playersList.setLayoutManager(new LinearLayoutManager(this));

        loadPlayers();
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
                playersList.setAdapter(new PlayerAdapter(items, player -> {
                    Intent intent = new Intent(PlayersListActivity.this, ProfileActivity.class);
                    intent.putExtra(ProfileActivity.EXTRA_PLAYER_ID, player.id);
                    startActivity(intent);
                }));
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }
}
