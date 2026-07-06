package com.example.myapplication;

import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

// Interroge périodiquement /challenges/incoming pendant qu'un écran "salon"
// (Home, liste des joueurs) est au premier plan, et affiche une pop-up
// Accepter/Refuser dès qu'un défi arrive. Partagé entre les activités pour
// éviter de dupliquer la logique de sondage et de dialogue.
public class ChallengePoller {

    private static final long POLL_INTERVAL_MS = 3000;

    public interface OnAcceptedListener {
        void onAccepted(int gameId);
    }

    private final AppCompatActivity activity;
    private final SessionManager session;
    private final OnAcceptedListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = this::poll;

    private boolean running = false;
    private boolean dialogShowing = false;

    public ChallengePoller(AppCompatActivity activity, SessionManager session, OnAcceptedListener listener) {
        this.activity = activity;
        this.session = session;
        this.listener = listener;
    }

    public void start() {
        running = true;
        poll();
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(pollRunnable);
    }

    private void poll() {
        if (!running) return;

        ApiClient.get("/challenges/incoming", session.getToken(), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isUsable()) return;
                if (!dialogShowing) {
                    try {
                        JSONArray challenges = response.getJSONArray("data");
                        if (challenges.length() > 0) {
                            showChallengeDialog(challenges.getJSONObject(0));
                        }
                    } catch (Exception ignored) {
                    }
                }
                scheduleNext();
            }

            @Override
            public void onError(String message) {
                scheduleNext();
            }
        });
    }

    private void scheduleNext() {
        if (running) handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private boolean isUsable() {
        return running && !activity.isFinishing() && !activity.isDestroyed();
    }

    private void showChallengeDialog(JSONObject challenge) throws Exception {
        int challengeId = challenge.getInt("id");
        String username = challenge.getString("challenger_username");
        int elo = challenge.getInt("challenger_elo");

        dialogShowing = true;
        new AlertDialog.Builder(activity)
                .setTitle("Défi reçu")
                .setMessage(username + " (" + elo + " Elo) vous a défié !")
                .setCancelable(false)
                .setPositiveButton("Accepter", (d, w) -> respond(challengeId, true))
                .setNegativeButton("Refuser", (d, w) -> respond(challengeId, false))
                .show();
    }

    private void respond(int challengeId, boolean accept) {
        String path = "/challenges/" + challengeId + "/" + (accept ? "accept" : "decline");
        ApiClient.post(path, null, session.getToken(), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                dialogShowing = false;
                if (!isUsable()) return;
                if (accept) {
                    try {
                        int gameId = response.getJSONObject("data").getJSONObject("game").getInt("id");
                        listener.onAccepted(gameId);
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            public void onError(String message) {
                dialogShowing = false;
            }
        });
    }
}
