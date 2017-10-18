package com.holenet.nightsky.item;

public class TextLog extends BaseLog {
    private String text;

    public TextLog(int pk, String createdAt, Watch watch, String text) {
        super(pk, createdAt, watch);
        this.text = text;
    }

    public TextLog(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
