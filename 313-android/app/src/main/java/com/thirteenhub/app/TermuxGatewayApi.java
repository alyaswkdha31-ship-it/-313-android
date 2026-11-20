package com.thirteenhub.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Routes the app's main chat through the local free-only OpenRouter gateway in Termux. */
public final class TermuxGatewayApi {
    private static final String BASE = "http://127.0.0.1:3131";
    private TermuxGatewayApi() { }

    public static String chat(String token, JSONArray messages) throws Exception {
        JSONObject body = new JSONObject();
        body.put("messages", messages);
        JSONObject response = request("/api/chat", token, body);
        String answer = response.optString("answer", "");
        if (answer.trim().isEmpty()) throw new IllegalStateException("وصل رد فارغ من بوابة 313.");
        return answer;
    }

    public static JSONObject diagnostics(String token) throws Exception {
        return request("/api/diagnostics", token, null);
    }

    private static JSONObject request(String path, String token, JSONObject body) throws Exception {
        if (token == null || token.trim().isEmpty())
            throw new IllegalArgumentException("رمز ربط Termux غير محفوظ.");
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + path).openConnection();
        try {
            c.setConnectTimeout(4000);
            c.setReadTimeout(body == null ? 20000 : 130000);
            c.setRequestProperty("X-313-Token", token);
            c.setRequestProperty("Accept", "application/json");
            if (body != null) {
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                c.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            }
            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            StringBuilder raw = new StringBuilder();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) raw.append(line);
                }
            }
            JSONObject result = raw.length() == 0 ? new JSONObject() : new JSONObject(raw.toString());
            if (code < 200 || code >= 300) {
                String error = result.optString("error", "HTTP " + code);
                throw new IllegalStateException("بوابة 313 (HTTP " + code + "): " + error);
            }
            return result;
        } finally {
            c.disconnect();
        }
    }
}
