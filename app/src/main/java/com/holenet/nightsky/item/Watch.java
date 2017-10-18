package com.holenet.nightsky.item;

public class Watch {
    private int pk;
    private Piece piece;
    private int start, end;
    private String date;

    public Watch(int pk) {
        this.pk = pk;
    }

    public Watch(Piece piece, int start, int end) {
        this.piece = piece;
        this.start = start;
        this.end = end;
    }

    public Watch(int pk, Piece piece, int start, int end, String date) {
        this.pk = pk;
        this.piece = piece;
        this.start = start;
        this.end = end;
        this.date = date;
    }

    public int getPk() {
        return pk;
    }

    public void setPk(int pk) {
        this.pk = pk;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
