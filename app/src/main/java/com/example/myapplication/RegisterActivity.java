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

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameField, emailField, passwordField, confirmPasswordField;
    private TextView errorText;
    private ProgressBar progressBar;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        session = new SessionManager(this);

        usernameField = findViewById(R.id.usernameField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        errorText = findViewById(R.id.errorText);
        progressBar = findViewById(R.id.progressBar);

        Button registerButton = findViewById(R.id.registerButton);
        TextView loginLink = findViewById(R.id.loginLink);

        registerButton.setOnClickListener(v -> register());
        loginLink.setOnClickListener(v -> finish());
    }

    private void register() {
        String username = usernameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString();
        String confirmPassword = confirmPasswordField.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError("Tous les champs sont requis");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("email", email);
            body.put("password", password);

            setLoading(true);
            ApiClient.post("/auth/register", body, null, new ApiClient.Callback() {
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
                        startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                        finish();
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

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}
