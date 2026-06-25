package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "session";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(String token, int playerId, String username, String email, int elo) {
        prefs.edit()
                .putString("token", token)
                .putInt("player_id", playerId)
                .putString("username", username)
                .putString("email", email)
                .putInt("elo", elo)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public String getToken() {
        return prefs.getString("token", null);
    }

    public int getPlayerId() {
        return prefs.getInt("player_id", -1);
    }

    public String getUsername() {
        return prefs.getString("username", "");
    }

    public String getEmail() {
        return prefs.getString("email", "");
    }

    public int getElo() {
        return prefs.getInt("elo", 1200);
    }
}
