package com.holenet.nightsky.item;

public abstract class BaseLog {
    private int pk;
    private String createdAt;
    private String modifiedAt;
    private Watch watch;

    public BaseLog(int pk, String createdAt, Watch watch) {
        this.pk = pk;
        this.createdAt = createdAt;
        this.watch = watch;
    }

    public BaseLog() {
    }

    public int getPk() {
        return pk;
    }

    public void setPk(int pk) {
        this.pk = pk;
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
