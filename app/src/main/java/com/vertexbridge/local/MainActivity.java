package com.vertexbridge.local;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.security.SecureRandom;

public final class MainActivity extends Activity {
    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String BASE_URL = "http://127.0.0.1:8050";
    private static final int ID = 7001;

    public static synchronized String getOrCreateKey(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("local_secret", Context.MODE_PRIVATE);
        String key = prefs.getString("api_key", null);
        if (key == null || key.isEmpty()) {
            byte[] bytes = new byte[24]; new SecureRandom().nextBytes(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format(java.util.Locale.US, "%02x", b & 0xff));
            key = sb.toString();
            prefs.edit().putString("api_key", key).apply();
        }
        return key;
    }

    private void copy(String value) {
        ClipboardManager mgr = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        mgr.setPrimaryClip(ClipData.newPlainText("Vertex2OpenAI", value));
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }
    private TextView label(String text, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextSize(size); v.setTextColor(Color.rgb(30, 39, 54));
        v.setPadding(0, 10, 0, 10);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private void button(LinearLayout root, String text, Runnable callback) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false);
        root.addView(b, new LinearLayout.LayoutParams(-1, -2));
        b.setOnClickListener(v -> callback.run());
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 38, 32, 36);
        root.setBackgroundColor(Color.rgb(249, 250, 252));
        scroll.addView(root); setContentView(scroll);
        root.addView(label("Vertex2OpenAI · 本地桥", 24, true));
        root.addView(label("Gemini → OpenAI API · 专供本机 SillyTavern", 14, false));
        status = label("正在检测状态…", 17, true); root.addView(status);
        button(root, "▶ 启动代理", () -> {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, ID);
            }
            Intent i = new Intent(this, BridgeService.class);
            startForegroundService(i);
        });
        button(root, "■ 停止代理", () -> {
            Intent i = new Intent(this, BridgeService.class); i.setAction(BridgeService.ACTION_STOP);
            startService(i);
        });
        button(root, "⚙ 打开代理管理后台", () -> {
            if (!BridgeService.running) {
                Toast.makeText(this, "先启动代理并等待显示运行中", Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(new Intent(this, PanelActivity.class));
        });
        root.addView(label("SillyTavern 连接信息", 19, true));
        root.addView(label("API Base URL：" + BASE_URL + "/v1", 14, false));
        button(root, "复制 API Base URL", () -> copy(BASE_URL + "/v1"));
        root.addView(label("API Key：安装时随机生成，应用内持久保存", 14, false));
        button(root, "复制 API Key", () -> copy(getOrCreateKey(this)));
        root.addView(label("配置步骤：启动 → 管理后台配置 Google Express Key 或 Cookie/Project ID → SillyTavern 选择 Chat Completion / OpenAI 兼容接口 → 粘贴 URL、API Key → 选择模型。", 14, false));
        root.addView(label("安全：监听本机 127.0.0.1，默认不对局域网开放。Android 可能清理后台，长时间使用请允许后台活动。", 13, false));
        handler.post(new Runnable() {
            @Override public void run() {
                status.setText("状态：" + BridgeService.status);
                handler.postDelayed(this, 1000);
            }
        });
    }
}
