package com.tvguard.tutor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int[] INTERVALS = {1, 5, 10, 15, 30};

    private Prefs prefs;
    private TextView tvStatus;
    private TextView tvHint;
    private Button btnToggle;
    private Button btnInterval;
    private Button btnGrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new Prefs(this);

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvHint = (TextView) findViewById(R.id.tvHint);
        btnToggle = (Button) findViewById(R.id.btnToggle);
        btnInterval = (Button) findViewById(R.id.btnInterval);
        btnGrade = (Button) findViewById(R.id.btnGrade);

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean running = !prefs.isRunning();
                prefs.setRunning(running);
                if (running) {
                    startService(new Intent(MainActivity.this, MonitorService.class));
                    Toast.makeText(MainActivity.this, "已开始监控", Toast.LENGTH_SHORT).show();
                    // 关键：立刻退出设置界面。
                    // 否则做题界面 finish 后会回到这里，而不是回到孩子原来看的画面。
                    finish();
                } else {
                    stopService(new Intent(MainActivity.this, MonitorService.class));
                    refresh();
                }
            }
        });

        btnInterval.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cur = prefs.getIntervalMin();
                int next = INTERVALS[0];
                for (int i = 0; i < INTERVALS.length; i++) {
                    if (INTERVALS[i] == cur) {
                        next = INTERVALS[(i + 1) % INTERVALS.length];
                        break;
                    }
                }
                prefs.setIntervalMin(next);
                refresh();
            }
        });

        btnGrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int g = prefs.getGrade();
                g = (g % 6) + 1;
                prefs.setGrade(g);
                refresh();
            }
        });

        refresh();
        applyAutomationIntent(getIntent());
    }

    /**
     * 自动化/测试入口：通过 am start 传参直接设定状态，避免依赖遥控器操作。
     * 例：am start -n com.tvguard.tutor/.MainActivity \
     *       --ei tvguard.interval 1 --ez tvguard.autostart true
     */
    private void applyAutomationIntent(Intent it) {
        if (it == null) {
            return;
        }
        boolean touched = false;
        int iv = it.getIntExtra("tvguard.interval", -1);
        if (iv > 0) {
            prefs.setIntervalMin(iv);
            touched = true;
        }
        int gv = it.getIntExtra("tvguard.grade", -1);
        if (gv > 0) {
            prefs.setGrade(gv);
            touched = true;
        }
        if (touched) {
            refresh();
        }
        if (it.getBooleanExtra("tvguard.autostart", false)) {
            prefs.setRunning(true);
            startService(new Intent(this, MonitorService.class));
            finish();
        } else if (it.hasExtra("tvguard.running")) {
            prefs.setRunning(it.getBooleanExtra("tvguard.running", false));
            refresh();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean running = prefs.isRunning();
        tvStatus.setText(running
                ? "监控中 · 每 " + prefs.getIntervalMin() + " 分钟弹一次题"
                : "已停止");
        btnToggle.setText(running ? "停止监控" : "开始监控");
        btnInterval.setText("间隔：" + prefs.getIntervalMin() + " 分钟");
        btnGrade.setText("年级：" + prefs.getGrade() + " 年级");
        tvHint.setText("连对 " + prefs.getNeedCount() + " 题才放行 · 科目：" + prefs.subjectsMask());
    }
}
