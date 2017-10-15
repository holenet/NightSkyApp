package com.holenet.nightsky.item;

public abstract class BaseLog {
    private String createdAt;
    private String modifiedAt;
    private Watch watch;

    public BaseLog() {
    }

    public BaseLog(String createdAt, String modifiedAt, Watch watch) {
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.watch = watch;
    }

    public BaseLog(String createdAt) {
        this.createdAt = createdAt;
    }

    public BaseLog(Watch watch) {
        this.watch = watch;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(String modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Watch getWatch() {
        return watch;
    }

    public void setWatch(Watch watch) {
        this.watch = watch;
    }
}
