package com.holenet.nightsky.item;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Post implements Parcelable {
    int id;
    private String title;
    private String author;
    private String[] datetime;
    private String text;
    private List<Comment> comments;
    private int commentCount;

    public Post(int id, String title, String author, String[] datetime, String text) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.datetime = datetime;
        this.text = text;
        comments = new ArrayList<>();
    }

    public Post(int id, String title, String author, String[] datetime, String text, List<Comment> comments) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.datetime = datetime;
        this.text = text;
        this.comments = comments;
        commentCount = comments.size();
    }

    public Post() {
        id = -1;
        title = "";
        author = "";
        datetime = new String[] {"", ""};
        text = "";
        comments = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        commentCount = comments.size();
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        commentCount++;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public boolean setCommentCount(int count) {
        this.commentCount = count;
        return comments.isEmpty();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.title);
        dest.writeString(this.author);
        dest.writeStringArray(this.datetime);
        dest.writeString(this.text);
        dest.writeList(this.comments);
    }

    protected Post(Parcel in) {
        this.id = in.readInt();
        this.title = in.readString();
        this.author = in.readString();
        this.datetime = in.createStringArray();
        this.text = in.readString();
        this.comments = new ArrayList<>();
        in.readList(this.comments, Comment.class.getClassLoader());
    }

    public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel source) {
            return new Post(source);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    public Post copy() {
        return new Post(getId(), getTitle(), getAuthor(), new String[]{getDatetime()[0], getDatetime()[1]}, getText(), getComments());
    }
}