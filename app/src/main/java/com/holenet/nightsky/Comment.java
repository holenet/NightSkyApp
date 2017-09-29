package com.holenet.nightsky;

import android.os.Parcel;
import android.os.Parcelable;

public class Comment implements Parcelable {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.author);
        dest.writeStringArray(this.datetime);
        dest.writeString(this.text);
    }

    protected Comment(Parcel in) {
        this.id = in.readInt();
        this.author = in.readString();
        this.datetime = in.createStringArray();
        this.text = in.readString();
    }

    public static final Parcelable.Creator<Comment> CREATOR = new Parcelable.Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel source) {
            return new Comment(source);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };
}