package com.tvguard.tutor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * 全屏做题界面。答对 needCount 题才放行。
 * 按 Home 离开会在 1.2 秒后被拉回，这是本方案唯一能做的一点"反抗"。
 */
public class QuizActivity extends Activity {

    private final QuestionBank bank = new QuestionBank();
    private final Handler handler = new Handler();

    /** 跨实例节流，防止重建风暴 */
    private static long sLastRebuildMs = 0L;

    private Question current;
    private int needCount = 2;
    private int rightCount = 0;
    private boolean solved = false;

    private TextView tvQuestion;
    private TextView tvNeed;
    private TextView tvFeedback;
    private Button[] btns = new Button[4];

    /** 是否处于前台，供 MonitorService 判断是否被别的应用抢走焦点 */
    static volatile boolean isForeground = false;

    private final Runnable relaunch = new Runnable() {
        @Override
        public void run() {
            if (solved || isFinishing()) {
                return;
            }
            Intent i = new Intent(QuizActivity.this, QuizActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            try {
                startActivity(i);
            } catch (Exception ignored) {
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        needCount = new Prefs(this).getNeedCount();

        tvQuestion = (TextView) findViewById(R.id.tvQuestion);
        tvNeed = (TextView) findViewById(R.id.tvNeed);
        tvFeedback = (TextView) findViewById(R.id.tvFeedback);

        btns[0] = (Button) findViewById(R.id.btn0);
        btns[1] = (Button) findViewById(R.id.btn1);
        btns[2] = (Button) findViewById(R.id.btn2);
        btns[3] = (Button) findViewById(R.id.btn3);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            btns[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAnswer(idx);
                }
            });
        }
        loadQuestion();
    }

    private void loadQuestion() {
        Prefs p = new Prefs(this);
        current = bank.next(p.getGrade(), p.allowChinese(), p.allowEnglish(), p.allowMath());
        tvQuestion.setText(current.stem);
        tvFeedback.setText("");
        int n = Math.min(4, current.options.length);
        for (int i = 0; i < 4; i++) {
            if (i < n) {
                btns[i].setVisibility(View.VISIBLE);
                btns[i].setText(current.options[i]);
                btns[i].setEnabled(true);
            } else {
                btns[i].setVisibility(View.INVISIBLE);
            }
        }
        tvNeed.setText("还需答对 " + (needCount - rightCount) + " 题");
        btns[0].requestFocus();
    }

    private void onAnswer(int idx) {
        if (current == null) {
            return;
        }
        if (idx == current.answerIndex) {
            rightCount++;
            if (rightCount >= needCount) {
                solved = true;
                tvFeedback.setText("答对了，继续看吧");
                handler.removeCallbacks(relaunch);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 700);
            } else {
                tvFeedback.setText("答对了，" + (needCount - rightCount) + " 题后继续");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadQuestion();
                    }
                }, 700);
            }
        } else {
            rightCount = 0;
            tvFeedback.setText("答错了。" + current.explain);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadQuestion();
                }
            }, 2600);
        }
    }

    @Override
    public void onBackPressed() {
        // 吞掉返回键
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isForeground = false;
        // 单个 Activity 抢焦点不可靠（视频 App 会反抢），
        // 真正的守护交给 MonitorService 的定时复查。
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;
        handler.removeCallbacks(relaunch);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isForeground = false;
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (solved) {
            MonitorService.notifyQuizDone();
        } else {
            // 被返回键/系统强行退出：立即重建，题目不下线
            long now = System.currentTimeMillis();
            if (now - sLastRebuildMs > 800) {
                sLastRebuildMs = now;
                Intent i = new Intent(this, QuizActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(i);
                } catch (Exception ignored) {
                }
            }
        }
        super.onDestroy();
    }
}
