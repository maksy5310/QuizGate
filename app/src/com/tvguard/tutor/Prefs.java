package com.tvguard.tutor;

import android.content.Context;
import android.content.SharedPreferences;

/** 轻量配置存取 */
public class Prefs {

    private static final String NAME = "tvguard";
    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public boolean isRunning() {
        return sp.getBoolean("running", false);
    }

    public void setRunning(boolean v) {
        sp.edit().putBoolean("running", v).apply();
    }

    public int getIntervalMin() {
        return sp.getInt("interval", 5);
    }

    public void setIntervalMin(int v) {
        sp.edit().putInt("interval", v).apply();
    }

    public int getGrade() {
        return sp.getInt("grade", 6);
    }

    public void setGrade(int v) {
        sp.edit().putInt("grade", v).apply();
    }

    public int getNeedCount() {
        return sp.getInt("need", 2);
    }

    public void setNeedCount(int v) {
        sp.edit().putInt("need", v).apply();
    }

    /** 是否允许语文题 */
    public boolean allowChinese() {
        return sp.getBoolean("sub_chinese", true);
    }

    /** 是否允许英语题 */
    public boolean allowEnglish() {
        return sp.getBoolean("sub_english", true);
    }

    /** 是否允许数学题 */
    public boolean allowMath() {
        return sp.getBoolean("sub_math", true);
    }

    public String subjectsMask() {
        return (allowChinese() ? "语" : "") + (allowEnglish() ? "英" : "") + (allowMath() ? "数" : "");
    }
}
