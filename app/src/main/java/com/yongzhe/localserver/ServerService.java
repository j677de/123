package com.yongzhe.localserver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ServerService extends Service {
    public static volatile LocalHttpServer server;
    public static volatile LocalWebSocketServer wsServer;
    public static volatile String lastLog = "";
    private GameDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                "yongzhe_local_server",
                "勇者之路本地服务器",
                NotificationManager.IMPORTANCE_LOW
        );
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, "yongzhe_local_server")
                .setContentTitle("勇者之路本地服务器")
                .setContentText("HTTP 9055 / WebSocket 8058 正在运行")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build();
        startForeground(9055, notification);

        db = new GameDatabase(this);
        LocalHttpServer.Logger logger = line -> {
            String now = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
            lastLog = now + "  " + line + "\n" + lastLog;
            if (lastLog.length() > 12000) lastLog = lastLog.substring(0, 12000);
        };

        server = new LocalHttpServer(9055, db, logger);
        wsServer = new LocalWebSocketServer(8058, db, logger::log);
        try { server.start(); } catch (Exception e) { logger.log("HTTP 启动失败: " + e.getMessage()); }
        try { wsServer.start(); } catch (Exception e) { logger.log("WebSocket 启动失败: " + e.getMessage()); }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (server != null) server.stop();
        if (wsServer != null) wsServer.stop();
        server = null;
        wsServer = null;
        if (db != null) db.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
