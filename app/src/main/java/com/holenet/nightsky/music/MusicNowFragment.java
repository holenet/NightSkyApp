package com.holenet.nightsky.music;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.holenet.nightsky.DatabaseHelper;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.Music;

import java.util.ArrayList;
import java.util.List;

public class MusicNowFragment extends Fragment {
    Context context;
    MusicActivity activity;

    DatabaseHelper dbHelper;

    ListView lVmusics;
    MusicsAdapter adapter;

    MusicListTask listTask;

    SharedPreferences pref;
    int currentListId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getContext();
        activity = (MusicActivity) getActivity();

        dbHelper = new DatabaseHelper(context);

        View v = inflater.inflate(R.layout.fragment_music_list, container, false);

        lVmusics = v.findViewById(R.id.lVmusics);
        adapter = new MusicsAdapter(context, R.layout.item_music, new ArrayList<Music>());
        lVmusics.setAdapter(adapter);
        lVmusics.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                activity.notifyCurrMusicChanged(adapter.getItem(i).getDeviceId());
            }
        });

        pref = context.getSharedPreferences("settings_music", 0);

        refresh();

        return v;
    }

    public void refresh() {
        currentListId = pref.getInt(getString(R.string.pref_key_curr_list_id), -1);
        if(currentListId==-1) {
            adapter.clear();
            adapter.notifyDataSetChanged();
            return;
        }
        if(listTask!=null)
            return;
        listTask = new MusicListTask(currentListId);
        listTask.execute((Void) null);
    }

    class MusicListTask extends AsyncTask<Void, Music, Void> {
        int listId;

        public MusicListTask(int listId) {
            this.listId = listId;
        }

        @Override
        protected void onPreExecute() {
            activity.showProgress(true);
            adapter.clear();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("select music_id from "+DatabaseHelper.musicLinkTable+" where list_id = "+listId, null);
            int cnt = c.getCount();
            for(int i=0; i<cnt; i++) {
                c.moveToNext();
                int deviceId = c.getInt(0);

                Cursor cc = db.rawQuery("select server_id, title, artist, album, length, path from "+DatabaseHelper.musicTable+" where device_id = "+deviceId, null);
                cc.moveToNext();
                int serverId = cc.getInt(0);
                String title = cc.getString(1);
                String artist = cc.getString(2);
                String album = cc.getString(3);
                int length = cc.getInt(4);
                String path = cc.getString(5);

                Music music = new Music(deviceId, serverId, title, artist, album, length, path);
                publishProgress(music);

                cc.close();
            }
            c.close();

            return null;
        }

        @Override
        protected void onProgressUpdate(Music... values) {
            for(Music music : values) {
                adapter.addItem(music);
            }
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            listTask = null;
            activity.showProgress(false);
        }

        @Override
        protected void onCancelled() {
            listTask = null;
            activity.showProgress(false);
        }
    }

    class MusicsAdapter extends ArrayAdapter<Music> {
        private List<Music> items;

        public MusicsAdapter(Context context, int resource, List<Music> items) {
            super(context, resource, items);
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if(v==null) {
                v = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_music, null);
            }

            Music music = items.get(position);
            if(music!=null) {
                ImageView iVdevice = v.findViewById(R.id.iVdevice);
                if(iVdevice!=null)
                    iVdevice.setImageResource(music.getDeviceId()<0 ? R.color.blank : R.color.device);
                ImageView iVserver = v.findViewById(R.id.iVserver);
                if(iVserver!=null)
                    iVserver.setImageResource(music.getServerId()<0 ? R.color.blank : R.color.server);
                TextView tVtitle = v.findViewById(R.id.tVtitle);
                if(tVtitle!=null)
                    tVtitle.setText(music.getTitle());
                TextView tVartist= v.findViewById(R.id.tVartist);
                if(tVartist!=null)
                    tVartist.setText(music.getArtist());
            }

            return v;
        }

        public void addItem(Music item) {
            for(int i=0; i<items.size(); i++) {
                if(items.get(i).equals(item)) {
                    items.get(i).addProperty(item);
                    return;
                }
            }
            items.add(item);
        }
    }
}
