package com.holenet.nightsky.item;

import android.net.Uri;
import android.os.Environment;

import java.io.File;

public class Music {
    private int deviceId = -1;
    private int serverId = -1;
    private String title;
    private String artist;
    private String album;
    private int length;
    private String path;

    public Music(int deviceId, int serverId, String title, String artist, String album, int length, String path) {
        this.deviceId = deviceId;
        this.serverId = serverId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.length = length;
        this.path = path;
    }

    public Music(String title, String artist, String album, String path) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getLengthFormat() {
        return formatSeconds(length/1000);
    }

    public static String formatSeconds(int sec) {
        int h = sec/3600;
        int m = sec%3600/60;
        int s = sec%60;
        if(h==0)
            return String.format("%d:%02d", m, s);
        else
            return String.format("%d:%02d:%02d", h, m, s);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public File getFile() {
        if(path==null)
            return null;
        return new File(Environment.getExternalStorageDirectory()+File.separator+path);
    }

    public Uri getUri() {
        File file = getFile();
        if(file==null)
            return null;
        return Uri.fromFile(file);
    }

    public void addProperty(Music music) {
        if(deviceId<0)
            deviceId = music.getDeviceId();
        if(serverId<0)
            serverId = music.getServerId();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Music) {
            Music music = (Music) obj;
            return (deviceId>=0 && deviceId==music.getDeviceId()) ||
                    (serverId>=0 && serverId==music.getServerId());
        }
        return false;
    }
}
