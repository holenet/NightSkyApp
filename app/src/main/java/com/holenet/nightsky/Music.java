package com.holenet.nightsky;

public class Music {
    private int deviceId = -1;
    private int serverId = -1;
    private String title;
    private String artist;
    private String album;
    private int length;

    public Music(String title, String artist, String album) {
        this.title = title;
        this.artist = artist;
        this.album = album;
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
        int m = length/60;
        int s = length%60;
        return m+":"+s;
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
