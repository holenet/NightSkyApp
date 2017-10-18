package com.holenet.nightsky.item;

import android.os.Environment;

import java.io.File;

public class ImageLog extends BaseLog {
    private String path;

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

    public String getAbsolutePath() { return Environment.getExternalStorageDirectory()+File.separator+path; }

    public void setPath(String path) {
        this.path = path;
    }
}
