package com.holenet.nightsky;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MusicFragment extends Fragment {
    Context context;
    UserActivity activity;

    SharedPreferences pref;
    DatabaseHelper dbHelper;

    MusicListTask listTask;

    ListView lVmusics;
    MusicsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getContext();
        activity = (UserActivity) getActivity();

        pref = context.getSharedPreferences("music_list", 0);
        dbHelper = new DatabaseHelper(context);

        View v = inflater.inflate(R.layout.fragment_music, container, false);

        lVmusics = v.findViewById(R.id.lVmusics);
        adapter = new MusicsAdapter(context, R.layout.item_music, new ArrayList<Music>());
        lVmusics.setAdapter(adapter);
        lVmusics.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(context, i+" : "+l, Toast.LENGTH_SHORT).show();
            }
        });

        refresh();

        return v;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length<=0 || grantResults[0]!=PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, R.string.error_permission, Toast.LENGTH_SHORT).show();
        } else {
            refresh();
        }
    }

    private boolean requestPermission() {
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(context, R.string.error_permission, Toast.LENGTH_LONG).show();
/*
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.e("request", "??");
            } else {
                Log.e("request", "permmm");
            }
*/
            Log.e("requestPermission", "");
            ActivityCompat.requestPermissions(activity, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return false;
        }
        return true;
    }

    void refresh() {
        if(!requestPermission()) {
            return;
        }
        if(listTask!=null)
            return;

        adapter.clear();
        int currentMusicListId = pref.getInt("current_id", -1);
        if(currentMusicListId==-1) {
            adapter.notifyDataSetChanged();
            return;
        }

        listTask = new MusicListTask(currentMusicListId);
        listTask.execute((Void) null);
    }

    class MusicListTask extends AsyncTask<Void, Void, String> {
        int listId;

        public MusicListTask(int listId) {
            this.listId = listId;
        }

        @Override
        protected String doInBackground(Void... voids) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("select device_id, server_id, title, artist, album, path from "+DatabaseHelper.musicTable, null);

            for(int i=0; i<c.getCount(); i++) {
                c.moveToNext();
                int deviceId = c.getInt(0);
                int serverId = c.getInt(1);
                String path = c.getString(5);

                String title, artist, album;
                title = artist = album = null;
                if(deviceId>=0) {
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    mediaMetadataRetriever.setDataSource(path);
                    title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    album = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    byte[] albumImage = mediaMetadataRetriever.getEmbeddedPicture();
                }
                if(serverId>=0) {
                    title = c.getString(2);
                    artist = c.getString(3);
                    album = c.getString(4);
                }
                Music music = new Music(title, artist, album);
                adapter.add(music);
            }

            c.close();

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            listTask = null;
            adapter.notifyDataSetChanged();
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
                TextView tVtitle = v.findViewById(R.id.tVtitle);
                if(tVtitle!=null)
                    tVtitle.setText(music.getTitle());
                TextView tVartist= v.findViewById(R.id.tVartist);
                if(tVartist!=null)
                    tVartist.setText(music.getArtist());
            }

            return v;
        }
    }
}
