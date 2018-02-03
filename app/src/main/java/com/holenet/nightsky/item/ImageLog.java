package com.holenet.nightsky.item;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.io.File;

public class ImageLog extends BaseLog {
    private String path;
    private Drawable drawable;

    public ImageLog(String path) {
        this.path = path;
    }

    public ImageLog(int pk, String createdAt, Watch watch, String path) {
        super(pk, createdAt, watch);
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getAbsolutePath(Context context) { return context.getExternalFilesDir(null).toString()+File.separator+path; }

    public void setPath(String path) {
        this.path = path;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }
}
