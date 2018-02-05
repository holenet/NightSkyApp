package com.holenet.nightsky;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.holenet.nightsky.item.Piece;
import com.holenet.nightsky.item.Watch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public final static int databaseVersion = 8;

    public final static String musicListTable = "music_list";
    public final static String musicTable = "music";
    public final static String musicLinkTable = "music_link";

    public final static String logTable = "log";

    public final static String pieceTable = "piece";

    public final static String watchTable = "watch";

    public DatabaseHelper(Context context) {
        super(context, context.getExternalFilesDir(null).toString()+File.separator+"database.db", null, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SQL = "create table "+musicListTable+"("
                + " _id integer PRIMARY KEY autoincrement, "
                + " name text)";
        try {
            db.execSQL(CREATE_SQL);
        } catch(Exception e) {
            e.printStackTrace();
        }

        CREATE_SQL = "create table "+musicTable+"("
                + " device_id integer PRIMARY KEY autoincrement, "
                + " server_id int, "
                + " title text, "
                + " artist text, "
                + " album text, "
                + " length int, "
                + " path text)";
        try {
            db.execSQL(CREATE_SQL);
        } catch(Exception e) {
            e.printStackTrace();
        }

        CREATE_SQL = "create table "+musicLinkTable+"("
                + " _id integer PRIMARY KEY autoincrement, "
                + " list_id int, "
                + " music_id int)";
        try {
            db.execSQL(CREATE_SQL);
        } catch(Exception e) {
            e.printStackTrace();
        }

        CREATE_SQL = "create table "+logTable+"("
                + " _id integer PRIMARY KEY autoincrement, "
                + " pk int, "
                + " type text, "
                + " watch_pk int, "
                + " text text)";
        try {
            db.execSQL(CREATE_SQL);
        } catch(Exception e) {
            e.printStackTrace();
        }

        CREATE_SQL = "create table "+pieceTable+"("
                + " _id integer PRIMARY KEY autoincrement, "
                + " pk int, "
                + " title text)";
        try {
            db.execSQL(CREATE_SQL);
        } catch(Exception e) {
            e.printStackTrace();
        }

        CREATE_SQL = "create table "+watchTable+"("
                + " _id integer PRIMARY KEY autoincrement, "
                + " pk int, "
                + " piece_pk int, "
                + " start int, "
                + " end int, "
                + " etc text, "
                + " date text)";
        try {
            db.execSQL(CREATE_SQL);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("drop table if exists "+musicListTable);
        db.execSQL("drop table if exists "+musicTable);
        db.execSQL("drop table if exists "+musicLinkTable);
        db.execSQL("drop table if exists "+logTable);
        db.execSQL("drop table if exists "+pieceTable);
        db.execSQL("drop table if exists "+watchTable);
        onCreate(db);
    }

    public boolean refresh(Context context) {
        // TODO: implement this method
        return false;
    }

    public static boolean updatePiece(Context context, Watch watch) {
        if(watch==null)
            return false;
        SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        Cursor c = db.rawQuery("select piece_pk, start, end, etc from "+DatabaseHelper.watchTable+" where pk = "+watch.getPk(), null);
        c.moveToNext();
        int piecePk = c.getInt(0);
        watch.setStart(c.getInt(1));
        watch.setEnd(c.getInt(2));
        watch.setEtc(c.getString(3));
        Cursor cc = db.rawQuery("select title from "+DatabaseHelper.pieceTable+" where pk = "+piecePk, null);
        cc.moveToNext();
        watch.setPiece(new Piece(piecePk, cc.getString(0)));
        cc.close();
        c.close();

        return true;
    }

    public static boolean updateWatchList(Context context, Piece piece) {
        if(piece==null)
            return false;
        SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        List<Watch> watches = new ArrayList<>();
        Cursor c = db.rawQuery("select pk, start, end, etc, date from "+DatabaseHelper.watchTable+" where piece_pk = "+piece.getPk(), null);
        for(int i=0; i<c.getCount(); i++) {
            c.moveToNext();
            int pk = c.getInt(0);
            watches.add(new Watch(pk));
        }
        c.close();
        db.close();
        piece.setWatches(watches);

        return true;
    }

    public static List<Watch> getWatchList(Context context) {
        SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        List<Watch> watches = new ArrayList<>();
        Cursor c = db.rawQuery("select pk from "+DatabaseHelper.watchTable, null);
        for(int i=0; i<c.getCount(); i++) {
            c.moveToNext();
            int pk = c.getInt(0);
            watches.add(new Watch(pk));
        }
        c.close();
        db.close();
        return watches;
    }

    public static List<Piece> getPieceList(Context context) {
        SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        final List<Piece> pieces = new ArrayList<>();
        Cursor c = db.rawQuery("select pk, title from "+DatabaseHelper.pieceTable, null);
        for(int i=0; i<c.getCount(); i++) {
            c.moveToNext();
            int pk = c.getInt(0);
            String title = c.getString(1);
            pieces.add(new Piece(pk, title));
        }
        c.close();
        db.close();
        return pieces;
    }

    public static Watch getRecentWatch(Context context, Piece piece) {
        SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        Watch watch = null;
        Cursor c = db.rawQuery("select pk, start, end, date from "+DatabaseHelper.watchTable+" where piece_pk = "+piece.getPk(), null);
        if(c.getCount()>0) {
            c.moveToNext();
            watch = new Watch(c.getInt(0), piece, c.getInt(1), c.getInt(2), c.getString(3));
        }
        c.close();
        db.close();
        return watch;
    }

    public static Piece getPiece(Context context, int pk) {
        SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        Piece piece = null;
        Cursor c = db.rawQuery("select title from "+pieceTable+" where pk = "+pk, null);
        if(c.getCount()>0) {
            c.moveToNext();
            piece = new Piece(pk, c.getString(0));
        }
        c.close();
        db.close();
        return piece;
    }
}
