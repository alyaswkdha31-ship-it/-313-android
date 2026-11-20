package com.thirteenhub.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Opt-in "يا إيلاف" wake listener.
 * Uses Android's installed speech-recognition provider; availability and continuous
 * recognition depend on the device/provider and are not guaranteed by Android.
 */
public final class ElafWakeService extends Service {
    public static final String ACTION_STOP = "com.thirteenhub.app.action.STOP_ELAF_WAKE";
    private static final String CHANNEL = "elaf_wake_listener";
    private static final int NOTIFICATION_ID = 3131;
    private static volatile boolean running = false;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService requests = Executors.newSingleThreadExecutor();
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean awaitingCommand = false;
    private boolean listening = false;
    private boolean destroyed = false;

    public static boolean isRunning() { return running; }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        Notification notification = buildNotification("قل: يا إيلاف");
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        running = true;
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                tts.setLanguage(new Locale("ar", "IQ"));
            }
        });
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("خدمة التعرف على الكلام غير متاحة");
            speak("خدمة التعرف على الكلام غير متاحة على هذا الجهاز");
            stopSelf();
            return;
        }
        startRecognizer();
        scheduleListen(500);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) stopSelf();
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "استماع يا إيلاف", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("إشعار دائم أثناء تشغيل كلمة التنبيه يا إيلاف");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, 313,
                open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, ElafWakeService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 314,
                stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("مركز قيادة إيلاف")
                .setContentText(text)
                .setContentIntent(content)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(android.R.drawable.ic_media_pause, "إيقاف", stopPending);
        return builder.build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private void startRecognizer() {
        if (destroyed) return;
        if (recognizer != null) {
            try { recognizer.destroy(); } catch (Exception ignored) { }
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(android.os.Bundle params) { listening = true; }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { listening = false; }
            @Override public void onError(int error) {
                listening = false;
                scheduleListen(error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1400 : 700);
            }
            @Override public void onResults(android.os.Bundle results) {
                listening = false;
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                String heard = matches == null || matches.isEmpty() ? "" : matches.get(0);
                handleRecognized(heard);
            }
            @Override public void onPartialResults(android.os.Bundle partial) {
                ArrayList<String> matches = partial.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && !awaitingCommand) {
                    String heard = matches.get(0);
                    if (containsWakeWord(heard)) {
                        String command = removeWakeWord(heard);
                        if (command.trim().length() > 2) {
                            awaitingCommand = true;
                            handleRecognized(heard);
                        } else {
                            awaitingCommand = true;
                            updateNotification("سمعتك، قل الأمر الآن");
                            restartAfter(350);
                        }
                    }
                }
            }
            @Override public void onEvent(int eventType, android.os.Bundle params) { }
        });
    }

    private void handleRecognized(String heard) {
        String clean = heard == null ? "" : heard.trim();
        if (clean.isEmpty()) { scheduleListen(500); return; }
        if (!awaitingCommand) {
            if (!containsWakeWord(clean)) { scheduleListen(400); return; }
            clean = removeWakeWord(clean).trim();
            if (clean.isEmpty()) {
                awaitingCommand = true;
                updateNotification("سمعتك، قل الأمر الآن");
                scheduleListen(400);
                return;
            }
        } else {
            clean = removeWakeWord(clean).trim();
        }
        awaitingCommand = false;
        if (clean.length() < 2) {
            updateNotification("قل: يا إيلاف ثم الأمر");
            scheduleListen(500);
            return;
        }
        final String command = clean;
        updateNotification("وصل الأمر إلى 313: " + shortText(command));
        answerThrough313(command);
        scheduleListen(650);
    }

    private boolean containsWakeWord(String value) {
        String s = normalize(value);
        return s.contains("يا ايلاف") || s.contains("يا يلاف")
                || s.contains("يا الاف") || s.contains("يا اللاف");
    }

    private String removeWakeWord(String value) {
        String s = value == null ? "" : value;
        String[] forms = {"يا إيلاف", "يا ايلاف", "يا يلاف", "يا الاف", "يا اللاف"};
        for (String form : forms) {
            int i = normalize(s).indexOf(normalize(form));
            if (i >= 0) return s.substring(Math.min(s.length(), i + form.length())).trim();
        }
        return s;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
                .replace("ى", "ي").replaceAll("[ًٌٍَُِّْـ]", "")
                .replaceAll("\\s+", " ").trim();
    }

    private void answerThrough313(String command) {
        requests.execute(() -> {
            String answer;
            try {
                String token = TeamGatewayStore.read(this);
                if (token == null || token.trim().isEmpty()) {
                    answer = "اربط 313 مع Termux من التطبيق أولاً.";
                } else {
                    JSONArray messages = new JSONArray();
                    JSONObject user = new JSONObject();
                    user.put("role", "user");
                    user.put("content", "المستخدم نادى يا إيلاف وقال: " + command
                            + ". أجب بالعربية باختصار. لا تدّع تنفيذ فتح تطبيق أو اتصال أو تحكم بجهاز؛ "
                            + "هذه الخدمة حالياً للمحادثة الصوتية فقط ولا تملك منفذ تنفيذ أوامر الهاتف.");
                    messages.put(user);
                    answer = TermuxGatewayApi.chat(token, messages);
                }
            } catch (Exception e) {
                answer = "تعذر الاتصال ببوابة 313. تأكد أن خادم Termux يعمل.";
            }
            final String response = answer;
            main.post(() -> {
                if (destroyed) return;
                updateNotification(shortText(response));
                speak(response);
            });
        });
    }

    private String shortText(String value) {
        if (value == null) return "";
        String s = value.replace('\n', ' ').trim();
        return s.length() > 90 ? s.substring(0, 87) + "..." : s;
    }

    private void speak(String text) {
        if (tts != null && text != null && !text.trim().isEmpty()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "elafyas-response");
        }
    }

    private void scheduleListen(long delay) {
        main.removeCallbacks(listenTask);
        if (!destroyed) main.postDelayed(listenTask, delay);
    }

    private final Runnable listenTask = () -> {
        if (destroyed || recognizer == null || listening) return;
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-IQ");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            recognizer.startListening(intent);
        } catch (Exception e) {
            listening = false;
            scheduleListen(1500);
        }
    };

    private void restartAfter(long delay) {
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (Exception ignored) { }
        }
        listening = false;
        scheduleListen(delay);
    }

    @Override public void onDestroy() {
        destroyed = true;
        running = false;
        main.removeCallbacksAndMessages(null);
        if (recognizer != null) {
            try { recognizer.cancel(); recognizer.destroy(); } catch (Exception ignored) { }
            recognizer = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        requests.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
