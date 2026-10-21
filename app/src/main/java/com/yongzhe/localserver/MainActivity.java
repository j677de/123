package com.yongzhe.localserver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private LinearLayout root;
    private TextView status;
    private TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(248, 250, 252));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        startForegroundService(new Intent(this, ServerService.class));
        showHome();
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private void showHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(248, 250, 252));
        root.setPadding(dp(20), statusBarHeight() + dp(14), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("勇者之路 · 本地服务器管理中心");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(15, 23, 42));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(12));
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setTextColor(Color.rgb(30, 41, 59));
        status.setPadding(dp(14), dp(12), dp(14), dp(12));
        status.setBackgroundColor(Color.WHITE);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        Button gm = new Button(this);
        gm.setText("进入 GM 管理后台");
        root.addView(gm);

        Button restart = new Button(this);
        restart.setText("重启本地服务器");
        root.addView(restart);

        Button stop = new Button(this);
        stop.setText("停止本地服务器");
        root.addView(stop);

        TextView logTitle = new TextView(this);
        logTitle.setText("服务器运行日志");
        logTitle.setTextSize(17);
        logTitle.setTextColor(Color.rgb(15, 23, 42));
        logTitle.setPadding(0, dp(14), 0, dp(6));
        root.addView(logTitle);

        log = new TextView(this);
        log.setTextSize(13);
        log.setTextColor(Color.rgb(51, 65, 85));
        log.setTextIsSelectable(true);
        log.setPadding(dp(12), dp(10), dp(12), dp(10));
        log.setBackgroundColor(Color.WHITE);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(log);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);

        gm.setOnClickListener(v -> showGm());
        restart.setOnClickListener(v -> {
            stopService(new Intent(this, ServerService.class));
            root.postDelayed(() -> startForegroundService(new Intent(this, ServerService.class)), 500);
            Toast.makeText(this, "本地服务器正在重启", Toast.LENGTH_SHORT).show();
        });
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, ServerService.class));
            Toast.makeText(this, "本地服务器已停止", Toast.LENGTH_SHORT).show();
        });
        refreshLoop();
    }

    private void showGm() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.rgb(248, 250, 252));
        page.setPadding(0, statusBarHeight(), 0, 0);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));
        bar.setBackgroundColor(Color.WHITE);

        Button back = new Button(this);
        back.setText("返回");
        bar.addView(back, new LinearLayout.LayoutParams(-2, -2));

        TextView title = new TextView(this);
        title.setText("GM 管理后台");
        title.setTextSize(19);
        title.setTextColor(Color.rgb(15, 23, 42));
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        View holder = new View(this);
        bar.addView(holder, new LinearLayout.LayoutParams(dp(72), dp(1)));
        page.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        WebView web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient());
        page.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);

        back.setOnClickListener(v -> showHome());
        web.postDelayed(() -> web.loadUrl("http://127.0.0.1:9055/gm"), 400);
    }

    private void refreshLoop() {
        if (root == null || status == null || log == null) return;
        boolean http = ServerService.server != null && ServerService.server.isRunning();
        boolean ws = ServerService.wsServer != null && ServerService.wsServer.isRunning();
        status.setText(
                "HTTP 服务：" + (http ? "运行中" : "未运行") + "  ·  127.0.0.1:9055\n" +
                "WebSocket：" + (ws ? "运行中" : "未运行") + "  ·  127.0.0.1:8058\n" +
                "数据存储：本机 SQLite"
        );
        log.setText(ServerService.lastLog == null || ServerService.lastLog.isEmpty() ? "暂无运行日志" : ServerService.lastLog);
        root.postDelayed(this::refreshLoop, 1000);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        showHome();
    }
}
