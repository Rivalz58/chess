package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        TextView welcomeText = findViewById(R.id.welcomeText);
        TextView eloText = findViewById(R.id.eloText);
        welcomeText.setText("Bienvenue, " + session.getUsername());
        eloText.setText(session.getElo() + " Elo");

        Button playButton = findViewById(R.id.playButton);
        Button profileButton = findViewById(R.id.profileButton);
        Button playersButton = findViewById(R.id.playersButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        playButton.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra(ProfileActivity.EXTRA_PLAYER_ID, session.getPlayerId());
            startActivity(intent);
        });

        playersButton.setOnClickListener(v -> startActivity(new Intent(this, PlayersListActivity.class)));

        logoutButton.setOnClickListener(v -> {
            session.clear();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
