package com.holenet.nightsky.music;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.nightsky.DatabaseHelper;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.Music;

import java.util.ArrayList;
import java.util.List;

public class MusicListFragment extends Fragment {
    Context context;
    MusicActivity activity;

    DatabaseHelper dbHelper;

    ListView lVlists;
    ListsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getContext();
        activity = (MusicActivity) getActivity();

        dbHelper = new DatabaseHelper(context);

        View v = inflater.inflate(R.layout.fragment_music_list, container, false);

        lVlists = (ListView) v.findViewById(R.id.lVmusics);
        adapter = new ListsAdapter(context, R.layout.item_music_list, new ArrayList<MusicList>());
        lVlists.setAdapter(adapter);
        lVlists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SharedPreferences pref = context.getSharedPreferences("settings_music", 0);
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt(getString(R.string.pref_key_curr_list_id), adapter.getItem(i).id);
                editor.apply();
                activity.notifyCurrListChanged();
            }
        });

        refresh();

        return v;
    }

    void refresh() {
        adapter.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("select _id, name from "+DatabaseHelper.musicListTable, null);
        for(int i=0; i<c.getCount(); i++) {
            c.moveToNext();
            int listId = c.getInt(0);
            String name = c.getString(1);

            Cursor cc = db.rawQuery("select music_id from "+DatabaseHelper.musicLinkTable+" where list_id = "+listId, null);
            int count = cc.getCount();
            int length = 0;
            for(int j=0; j<count; j++) {
                cc.moveToNext();
                int musicId = cc.getInt(0);
                Cursor ccc = db.rawQuery("select length from "+DatabaseHelper.musicTable+" where device_id = "+musicId, null);
                ccc.moveToNext();
                length += ccc.getInt(0);
                ccc.close();
            }
            cc.close();

            adapter.add(new MusicList(listId, name, count, length));
        }
        c.close();

        adapter.notifyDataSetChanged();
    }

    public void createList() {
        final EditText eTname = new EditText(context);
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        params.margin
//        eTname.setLayoutParams(.);
        new AlertDialog.Builder(context)
                .setTitle("Create new play list")
                .setView(eTname)
                .setPositiveButton("create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = eTname.getText().toString();
                        SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
                        ContentValues values = new ContentValues();
                        values.put("name", name);
                        db.insert(DatabaseHelper.musicListTable, null, values);
                        refresh();
                        Toast.makeText(context, "Created", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    class MusicList {
        int id;
        String name;
        int count;
        int length;

        public MusicList(int id, String name, int count, int length) {
            this.id = id;
            this.name = name;
            this.count = count;
            this.length = length;
        }
    }

    class ListsAdapter extends ArrayAdapter<MusicList> {
        private List<MusicList> items;

        public ListsAdapter(Context context, int resource, List<MusicList> items) {
            super(context, resource, items);
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if(v==null) {
                v = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_music_list, null);
            }

            MusicList list = items.get(position);
            if(list!=null) {
                TextView tVname = (TextView) v.findViewById(R.id.tVname);
                if(tVname!=null)
                    tVname.setText(list.name);
                TextView tVcount = (TextView) v.findViewById(R.id.tVcount);
                if(tVcount!=null)
                    tVcount.setText(String.valueOf(list.count));
                TextView tVlength = (TextView) v.findViewById(R.id.tVlength);
                if(tVlength!=null)
                    tVlength.setText(Music.formatSeconds(list.length/1000));
            }

            return v;
        }
    }
}
