package com.holenet.nightsky;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class DatabaseHelper extends SQLiteOpenHelper {
    public final static int databaseVersion = 5;

    public final static String musicListTable = "music_list";
    public final static String musicTable = "music";
    public final static String musicLinkTable = "music_link";

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
        db.execSQL("drop table if exists "+pieceTable);
        db.execSQL("drop table if exists "+watchTable);
        onCreate(db);
    }

    public boolean refresh(Context context) {
        // TODO: implement this method
        return false;
    }
}
