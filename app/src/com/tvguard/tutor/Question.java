package com.tvguard.tutor;

/** 一道题 */
public class Question {

    public final String stem;
    public final String[] options;
    public final int answerIndex;
    public final String explain;

    public Question(String stem, String[] options, int answerIndex, String explain) {
        this.stem = stem;
        this.options = options;
        this.answerIndex = answerIndex;
        this.explain = explain;
    }
}
