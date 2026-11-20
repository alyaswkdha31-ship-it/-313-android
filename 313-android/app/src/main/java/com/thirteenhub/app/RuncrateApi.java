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

/**
 * Minimal OpenAI-compatible Runcrate chat client.
 *
 * Call from a worker thread, never the Android main thread. The API key must
 * come from a secure caller-managed store; this class never persists or logs it.
 */
public final class RuncrateApi {
    private static final String CHAT_URL =
            "https://api.runcrate.ai/v1/chat/completions";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 60000;

    private RuncrateApi() { }

    public static final class ChatResult {
        public final boolean success;
        public final String text;
        public final String errorCode;
        public final String errorMessage;
        public final int httpStatus;

        private ChatResult(boolean success, String text, String errorCode,
                           String errorMessage, int httpStatus) {
            this.success = success;
            this.text = text;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.httpStatus = httpStatus;
        }
    }

    /**
     * Sends a single non-streaming chat request.
     * Throws IllegalArgumentException before networking for any non-allowlisted model.
     */
    public static ChatResult chat(String apiKey, String modelId, String userMessage)
            throws Exception {
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", userMessage);
        messages.put(message);
        return chat(apiKey, modelId, messages);
    }

    /** Sends the complete conversation history to the selected free model. */
    public static ChatResult chat(String apiKey, String modelId, JSONArray conversation)
            throws Exception {
        if (!RuncrateFreeModels.isAllowed(modelId)) {
            throw new IllegalArgumentException("Model is not in the free-only allowlist.");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Runcrate API key is required.");
        }
        if (conversation == null || conversation.length() == 0) {
            throw new IllegalArgumentException("Conversation is empty.");
        }

        JSONObject body = new JSONObject();
        body.put("model", modelId);
        body.put("stream", false);
        body.put("messages", conversation);

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(CHAT_URL).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setFixedLengthStreamingMode(payload.length);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String responseText = readAll(stream);
            JSONObject response;
            try {
                response = responseText.isEmpty() ? new JSONObject() : new JSONObject(responseText);
            } catch (org.json.JSONException malformed) {
                String detail = responseText.length() > 240
                        ? responseText.substring(0, 240) : responseText;
                return new ChatResult(false, null, "invalid_response",
                        detail.isEmpty() ? "Runcrate returned a non-JSON response." : detail,
                        status);
            }

            if (status >= 200 && status < 300) {
                JSONArray choices = response.optJSONArray("choices");
                if (choices == null || choices.length() == 0) {
                    return new ChatResult(false, null, "empty_response",
                            "Runcrate returned HTTP " + status + " but no chat choices. " + responseText,
                            status);
                }
                JSONObject first = choices.optJSONObject(0);
                JSONObject reply = first == null ? null : first.optJSONObject("message");
                String answer = reply == null ? null : reply.optString("content", null);
                if (answer == null || answer.trim().isEmpty()) {
                    return new ChatResult(false, null, "empty_response",
                            "Runcrate returned an empty answer.", status);
                }
                return new ChatResult(true, answer, null, null, status);
            }

            JSONObject error = response.optJSONObject("error");
            String code = error == null ? null : error.optString("code", null);
            String detail = error == null ? null : error.optString("message", null);
            if (error == null && response.has("message")) detail = response.optString("message", null);
            if (RuncrateFreeModels.isFreeLimitError(code)) {
                return new ChatResult(false, null, code,
                        "Free Runcrate allowance unavailable. No paid fallback was attempted.",
                        status);
            }
            return new ChatResult(false, null,
                    code == null || code.isEmpty() ? "http_error" : code,
                    detail == null || detail.isEmpty()
                            ? "Runcrate request failed (HTTP " + status + ")." : detail,
                    status);
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }
}
