package com.holenet.nightsky.item;

public class Watch {
    private int pk;
    private Piece piece;
    private int start, end;
    private String etc;
    private String date;

    public Watch(int pk) {
        this.pk = pk;
    }

    public Watch(Piece piece, int start, int end) {
        this.piece = piece;
        this.start = start;
        this.end = end;
    }

    public Watch(Piece piece, String etc) {
        this.piece = piece;
        this.etc = etc;
    }

    public Watch(int pk, Piece piece, int start, int end, String date) {
        this.pk = pk;
        this.piece = piece;
        this.start = start;
        this.end = end;
        this.date = date;
    }

    public Watch(int pk, Piece piece, String etc, String date) {
        this.pk = pk;
        this.piece = piece;
        this.etc = etc;
        this.date = date;
    }

    public Watch(int pk, Piece piece, int start, int end, String etc, String date) {
        this.pk = pk;
        this.piece = piece;
        this.start = start;
        this.end = end;
        this.etc = etc;
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

    public String getEtc() {
        return etc;
    }

    public void setEtc(String etc) {
        this.etc = etc;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRange() {
        if(etc!=null) {
            return "["+etc+"]";
        } else {
            return "["+(start==end ? start : start+"-"+end)+"]";
        }
    }

    @Override
    public String toString() {
        return piece.getTitle()+" "+getRange();
    }
}
