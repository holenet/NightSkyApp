package com.holenet.nightsky;

public class FileItem {
    enum FileType {
        audio, picture, etc,
    }

    private String filePath;
    private String author;
    private FileType type;
    private String description;
    private String[] datetime;

    public FileItem(String filePath, String author, FileType type, String description, String[] datetime) {
        this.filePath = filePath;
        this.author = author;
        this.type = type;
        this.description = description;
        this.datetime = datetime;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getDatetime() {
        return datetime;
    }

    public void setDatetime(String[] datetime) {
        this.datetime = datetime;
    }
}
