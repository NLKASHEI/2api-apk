package com.vertexbridge.local;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import com.chaquo.python.*;
import com.chaquo.python.android.AndroidPlatform;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BridgeService extends Service {
    public static final String ACTION_STOP = "com.vertexbridge.local.STOP";
    public static volatile String status = "未启动";
    public static volatile boolean running = false;
    private static final String CHANNEL_ID = "vertex_bridge_service";
    private final AtomicBoolean launched = new AtomicBoolean(false);
    private Thread serverThread;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "本地 API 代理", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("让 SillyTavern 访问本机 Gemini 代理");
        nm.createNotificationChannel(channel);
    }

    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Vertex2OpenAI 本地代理")
            .setContentText("仅监听 127.0.0.1:8050 · SillyTavern 可连接")
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopServer();
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(2026, notification());
        if (launched.compareAndSet(false, true)) {
            status = "启动中（首次启动可能较慢）";
            serverThread = new Thread(() -> {
                try {
                    if (!Python.isStarted()) Python.start(new AndroidPlatform(getApplicationContext()));
                    String secret = MainActivity.getOrCreateKey(getApplicationContext());
                    String files = getFilesDir().getAbsolutePath();
                    copyModelConfigIfMissing();
                    status = "加载服务中";
                    Python.getInstance().getModule("android_launcher").callAttr(
                        "start", files, secret, "127.0.0.1", 8050);
                    status = "已停止";
                } catch (Throwable e) {
                    status = "启动失败: " + e.getMessage();
                    Log.e("VertexBridge", "Server failed", e);
                } finally {
                    running = false;
                    launched.set(false);
                    stopSelf();
                }
            }, "vertex-python-server");
            serverThread.start();
            new Thread(() -> {
                for (int i=0; i<100; i++) {
                    if (!launched.get()) return;
                    try (java.net.Socket sock = new java.net.Socket()) {
                        sock.connect(new java.net.InetSocketAddress("127.0.0.1", 8050), 250);
                        status = "运行中 · 127.0.0.1:8050";
                        running = true;
                        return;
                    } catch (IOException ignored) {}
                    try { Thread.sleep(350); } catch (InterruptedException e) { return; }
                }
            }, "vertex-port-check").start();
        }
        return START_NOT_STICKY;
    }

    private void copyModelConfigIfMissing() {
        File target = new File(getFilesDir(), "vertexModels.json");
        if (target.exists()) return;
        try (InputStream is = getAssets().open("vertexModels.json");
             OutputStream os = new FileOutputStream(target)) {
            byte[] b = new byte[8192]; int n;
            while ((n = is.read(b)) != -1) os.write(b, 0, n);
        } catch (IOException e) {
            Log.w("VertexBridge", "Local model fallback could not be copied", e);
        }
    }

    private void stopServer() {
        status = "停止中";
        running = false;
        try {
            if (Python.isStarted()) Python.getInstance().getModule("android_launcher").callAttr("stop");
        } catch (Throwable e) { Log.e("VertexBridge", "Stop failed", e); }
    }

    @Override public void onDestroy() {
        stopServer();
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
