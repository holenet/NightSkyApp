package com.holenet.nightsky.item;

import java.util.ArrayList;
import java.util.List;

public class Piece {
    private int pk;
    private String title;
    private String comment;
    private List<Watch> watches;

    public Piece(int pk) {
        this.pk = pk;
    }

    public Piece(int pk, String title) {
        this.pk = pk;
        this.title = title;
        watches = new ArrayList<>();
    }

    public Piece(String title, String comment) {
        this.title = title;
        this.comment = comment;
        watches = new ArrayList<>();
    }

    public Piece(int pk, String title, String comment) {
        this.pk = pk;
        this.title = title;
        this.comment = comment;
        watches = new ArrayList<>();
    }

    public int getPk() {
        return pk;
    }

    public void setPk(int pk) {
        this.pk = pk;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Watch> getWatches() {
        return watches;
    }

    public void setWatches(List<Watch> watches) {
        this.watches = watches;
        setPieceForWatch(watches);
    }

    public void addWatch(Watch watch) {
        watches.add(watch);
        setPieceForWatch(watch);
    }

    private void setPieceForWatch(List<Watch> watches) {
        for(Watch watch: watches) {
            setPieceForWatch(watch);
        }
    }
    private void setPieceForWatch(Watch watch) {
        watch.setPiece(Piece.this);
    }

    public String getStartedAt() {
        String mn = null;
        for(Watch watch: watches) {
            if(mn==null || mn.compareTo(watch.getDate())<0) {
                mn = watch.getDate();
            }
        }
        return mn;
    }

    public String getEndedAt() {
        String mx = null;
        for(Watch watch: watches) {
            if(mx==null || mx.compareTo(watch.getDate())>0) {
                mx = watch.getDate();
            }
        }
        return mx;
    }

    public int[] getCountWatch() {
        List<Integer> count = new ArrayList<>();
        for(Watch watch: watches) {
            int s = watch.getStart();
            int e = watch.getEnd();
            if(count.size()<e) {
                for(int i=0; i<e-count.size(); i++) {
                    count.add(0);
                }
            }
            for(int i=s; i<e+1; i++) {
                count.set(i-1, count.get(i-1)+1);
            }
        }
        int[] array = new int[count.size()];
        for(int i=0; i<count.size(); i++) {
            array[i] = count.get(i);
        }
        return array;
    }
}
