package com.holenet.nightsky.item;

public class FileItem {
    public enum FileType {
        audio, picture, etc,
    }

    private int id;
    private String filePath;
    private String name;
    private FileType type;
    private String[] datetime;

    public FileItem(int id, String filePath, String name, FileType type, String[] datetime) {
        this.id = id;
        this.filePath = filePath;
        this.name = name;
        this.type = type;
        this.datetime = datetime;
    }

    public FileItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public String[] getDatetime() {
        return datetime;
    }

    public void setDatetime(String[] datetime) {
        this.datetime = datetime;
    }
}
