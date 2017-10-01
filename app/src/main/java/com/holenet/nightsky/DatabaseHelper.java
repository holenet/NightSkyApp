package com.holenet.nightsky;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class DatabaseHelper extends SQLiteOpenHelper {
    final static String musicListTable = "music_list";
    final static String musicTable = "music";
    final static int databaseVersion = 1;

    public DatabaseHelper(Context context) {
        super(context, context.getExternalFilesDir(null).toString()+File.separator+"database.db", null, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SQL = "create table "+musicListTable+"("
                + " _id integer PRIMARY KEY autoincrement, "
                + " datetime text, "
                + " name text)";
        try {
            db.execSQL(CREATE_SQL);
        } catch(Exception e) {
            e.printStackTrace();
        }

        CREATE_SQL = "create table "+musicTable+"("
                + " device_id integer PRIMARY KEY autoincrement, "
                + " server_id int, "
                + " list_id int, "
                + " title text, "
                + " artist text, "
                + " album text, "
                + " path text)";
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
        onCreate(db);
    }
}
