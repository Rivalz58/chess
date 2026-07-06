package com.example.myapplication;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    // 10.0.2.2 is how the Android emulator reaches the host machine's localhost.
    // Every emulator instance on this same PC can use this same alias independently.
    private static final String BASE_URL = "http://10.0.2.2/chess/backend/api";

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    public static void get(String path, String token, Callback cb) {
        request("GET", path, null, token, cb);
    }

    public static void post(String path, JSONObject body, String token, Callback cb) {
        request("POST", path, body, token, cb);
    }

    private static void request(String method, String path, JSONObject body, String token, Callback cb) {
        EXECUTOR.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + path);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Content-Type", "application/json");
                if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);

                if (body != null) {
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }

                int code = conn.getResponseCode();
                InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String text = readAll(stream);
                JSONObject json = new JSONObject(text);

                if (json.optBoolean("success", false)) {
                    deliverSuccess(cb, json);
                } else {
                    deliverError(cb, json.optString("error", "Erreur inconnue"));
                }
            } catch (Exception e) {
                deliverError(cb, "Erreur réseau : " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "{}";
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        return sb.toString();
    }

    private static void deliverSuccess(Callback cb, JSONObject response) {
        MAIN_HANDLER.post(() -> cb.onSuccess(response));
    }

    private static void deliverError(Callback cb, String message) {
        MAIN_HANDLER.post(() -> cb.onError(message));
    }
}
