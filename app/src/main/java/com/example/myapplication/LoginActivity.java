package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private TextView errorText;
    private ProgressBar progressBar;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);
        if (session.isLoggedIn()) {
            goHome();
            return;
        }

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        errorText = findViewById(R.id.errorText);
        progressBar = findViewById(R.id.progressBar);

        Button loginButton = findViewById(R.id.loginButton);
        TextView registerLink = findViewById(R.id.registerLink);

        loginButton.setOnClickListener(v -> login());
        registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError("Email et mot de passe requis");
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            setLoading(true);
            ApiClient.post("/auth/login", body, null, new ApiClient.Callback() {
                @Override
                public void onSuccess(JSONObject response) {
                    setLoading(false);
                    try {
                        JSONObject data = response.getJSONObject("data");
                        JSONObject player = data.getJSONObject("player");
                        session.save(
                                data.getString("token"),
                                player.getInt("id"),
                                player.getString("username"),
                                player.getString("email"),
                                player.getInt("elo"));
                        goHome();
                    } catch (Exception e) {
                        showError("Réponse du serveur invalide");
                    }
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    showError(message);
                }
            });
        } catch (Exception e) {
            setLoading(false);
            showError("Erreur interne");
        }
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}
