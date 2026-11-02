package com.tvguard.tutor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/**
 * 前台上报服务：只在屏幕亮着时累计时间，到点拉起做题界面。
 * 4.4 上后台 startActivity 不受限（该限制是 Android 10 才有）。
 */
public class MonitorService extends Service {

    private static final String TAG = "TvGuard";
    private static final int TICK_MS = 3000;

    static MonitorService instance;

    private Handler handler;
    private PowerManager powerManager;
    private long accumulatedMs = 0L;
    private long lastTickMs = 0L;
    private long lastGuardMs = 0L;
    private boolean quizVisible = false;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            onTick();
            if (handler != null) {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        handler = new Handler();
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        lastTickMs = System.currentTimeMillis();
        startForeground(1001, buildNotification());
        handler.post(ticker);
        Log.i(TAG, "service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void onTick() {
        long now = System.currentTimeMillis();
        boolean screenOn = powerManager != null && powerManager.isScreenOn();

        Prefs p = new Prefs(this);
        if (!p.isRunning()) {
            lastTickMs = now;
            return;
        }

        // 守护模式：题目已弹出，但可能被视频 App 抢回前台
        if (quizVisible) {
            lastTickMs = now;
            if (screenOn && !QuizActivity.isForeground && now - lastGuardMs > 2500) {
                lastGuardMs = now;
                Log.i(TAG, "quiz lost focus, bring back");
                bringQuizToFront();
            }
            return;
        }

        if (screenOn) {
            accumulatedMs += (now - lastTickMs);
        }
        lastTickMs = now;

        long limit = p.getIntervalMin() * 60L * 1000L;
        if (accumulatedMs >= limit) {
            accumulatedMs = 0L;
            showQuiz();
        }
    }

    private void showQuiz() {
        quizVisible = true;
        try {
            Intent i = new Intent(this, QuizActivity.class);
            // REORDER_TO_FRONT：界面已存在时复用它，保留答题进度，
            // 避免按 Home 后被重建导致计数清零。
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            Log.i(TAG, "quiz shown");
        } catch (Exception e) {
            quizVisible = false;
            Log.e(TAG, "show quiz failed: " + e);
        }
    }

    private void bringQuizToFront() {
        try {
            Intent i = new Intent(this, QuizActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "bring back failed: " + e);
        }
    }

    /** 做题界面结束时回调，重置计时 */
    static void notifyQuizDone() {
        MonitorService s = instance;
        if (s != null) {
            s.quizVisible = false;
            s.accumulatedMs = 0L;
            s.lastTickMs = System.currentTimeMillis();
        }
    }

    private Notification buildNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("monitoring")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pi)
                .build();
    }

    @Override
    public void onDestroy() {
        if (handler != null) {
            handler.removeCallbacks(ticker);
        }
        handler = null;
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
