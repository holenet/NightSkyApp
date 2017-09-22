package com.holenet.nightsky;

public class Comment {
    int id;
    String author;
    String[] datetime;
    String text;

    public Comment(int id, String author, String[] datetime, String text) {
        this.id = id;
        this.author = author;
        this.datetime = datetime;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String[] getDatetime() {
        return datetime;
    }

    public void setDatetime(String[] datetime) {
        this.datetime = datetime;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}