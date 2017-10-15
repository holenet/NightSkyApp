package com.holenet.nightsky.item;

public class Watch {
    private Piece piece;
    private int start, end;
    private String date;

    public Watch(Piece piece, int start, int end) {
        this.piece = piece;
        this.start = start;
        this.end = end;
    }

    public Watch(Piece piece, int start, int end, String date) {
        this.piece = piece;
        this.start = start;
        this.end = end;
        this.date = date;
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
