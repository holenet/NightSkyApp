package com.holenet.nightsky;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Environment;
import android.renderscript.AllocationAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicActivity extends AppCompatActivity {

//    DatabaseHelper dbHelper;

    MusicListTask listTask;

    ListView lVmusics;
    MusicsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

//        dbHelper = new DatabaseHelper(this);

        lVmusics = (ListView) findViewById(R.id.lVmusics);
        adapter = new MusicsAdapter(this, R.layout.item_music, new ArrayList<Music>());

        refresh();
    }

    void refresh() {
        if(listTask!=null)
            return;

        listTask = new MusicListTask();
        listTask.execute((Void) null);
        //showProgress(false);
    }

    class MusicListTask extends AsyncTask<Void, List<Music>, String> {
        @Override
        protected String doInBackground(Void... voids) {
            List<Music> items = new ArrayList<>();
            File dir = new File(Environment.getExternalStorageDirectory()+File.separator+"NightSky"+File.separator+"Musics");
            if(dir.exists()) {
                for(String path : dir.list()) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(path);
                    String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    byte[] albumImage = retriever.getEmbeddedPicture();

                    Music music = new Music(title, artist, album);
                    items.add(music);
                }
            }
            publishProgress(items);

            // TODO: load server musics

            return null;
        }

        @Override
        protected void onProgressUpdate(List<Music>... values) {
            adapter.clear();
            List<Music> items = values[0];
            for(Music item: items) {
                adapter.addItem(item);
            }
        }

        @Override
        protected void onPostExecute(String s) {
            listTask = null;
            adapter.notifyDataSetChanged();
//            showProgress(false);
        }

        @Override
        protected void onCancelled() {
            listTask = null;
//            showProgress(false);
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
                v = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_music, null);
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
