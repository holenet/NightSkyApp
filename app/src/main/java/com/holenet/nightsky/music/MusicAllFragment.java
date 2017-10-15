package com.holenet.nightsky.music;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.nightsky.DatabaseHelper;
import com.holenet.nightsky.NetworkManager;
import com.holenet.nightsky.Parser;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.Music;

import java.util.ArrayList;
import java.util.List;

public class MusicAllFragment extends Fragment {
    Context context;
    MusicActivity activity;

    DatabaseHelper dbHelper;

    ListView lVmusics;
    MusicsAdapter adapter;

    MusicListTask listTask;

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
        registerForContextMenu(lVmusics);

        refresh();

        return v;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
        Music music = adapter.getItem(position);

        if(music.getDeviceId()>=0)
            menu.add(0, 0, Menu.NONE, "Add to play list");
        else
            menu.add(0, 1, Menu.NONE, "Download");
        if(music.getServerId()<0)
            menu.add(0, 2, Menu.NONE, "Upload");
        menu.add(0, 3, Menu.NONE, "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int id = item.getItemId();
        final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
        final Music music = adapter.getItem(position);

        if(id==0) { // add to play list
            final SQLiteDatabase db = dbHelper.getReadableDatabase();
            final List<String> listNames = new ArrayList<>();
            final List<Integer> listIds = new ArrayList<>();
            Cursor c = db.rawQuery("select _id, name from "+DatabaseHelper.musicListTable, null);
            for(int i=0; i<c.getCount(); i++) {
                c.moveToNext();
                int listId = c.getInt(0);
                String name = c.getString(1);
                listIds.add(listId);
                listNames.add(name);
            }
            c.close();
            String[] items = new String[listNames.size()+1];
            for(int i=0; i<listNames.size(); i++) {
                items[i] = listNames.get(i);
            }
            items[listNames.size()] = "Create New Play List";
            new AlertDialog.Builder(context)
                    .setTitle("Add to play list")
                    .setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(i>=listNames.size()) {
                                final EditText eTname = new EditText(context);
                                new AlertDialog.Builder(context)
                                        .setTitle("Create new play list")
                                        .setView(eTname)
                                        .setPositiveButton("create", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                String listName = eTname.getText().toString();
                                                SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
                                                ContentValues values = new ContentValues();
                                                values.put("name", listName);
                                                int listId = (int) db.insert(DatabaseHelper.musicListTable, null, values);

                                                values = new ContentValues();
                                                values.put("list_id", listId);
                                                values.put("music_id", listName);
                                                db.insert(DatabaseHelper.musicLinkTable, null, values);
                                                Toast.makeText(context, "Added to "+listName, Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .setNegativeButton("cancel", null)
                                        .show();

                            } else {
                                int listId = listIds.get(i);
                                String listName = listNames.get(i);
                                ContentValues values = new ContentValues();
                                values.put("list_id", listId);
                                values.put("music_id", music.getDeviceId());
                                db.insert(DatabaseHelper.musicLinkTable, null, values);
                                Toast.makeText(context, "Added to "+listName, Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .create().show();
        } else if(id==1) { // download
            if(activity.downloadTask==null) {
                activity.downloadTask = new MusicActivity.MusicDownloadTask(activity, music);
                activity.downloadTask.execute((Void) null);
            }
        } else if(id==2) { // upload
            Toast.makeText(context, "Upload Music", Toast.LENGTH_SHORT).show();
        } else if(id==3) { // delete
            Toast.makeText(context, "Delete Music", Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    public void refresh() {
        if(listTask!=null)
            return;
        listTask = new MusicListTask();
        listTask.execute((Void) null);
    }

    class MusicListTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            activity.showProgress(true);
            adapter.clear();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result = NetworkManager.get(context, NetworkManager.CLOUD_DOMAIN+"music/?JSON");
            if(result!=null && !result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                List<Music> musics = Parser.getMusicListJSON(result);
                for(Music music : musics) {
                    adapter.addItem(music);
                }
            }

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("select device_id, server_id, title, artist, album, length, path from "+DatabaseHelper.musicTable, null);
            int cnt = c.getCount();
            for(int i = 0; i<cnt; i++) {
                c.moveToNext();
                int deviceId = c.getInt(0);
                int serverId = c.getInt(1);
                String title = c.getString(2);
                String artist = c.getString(3);
                String album = c.getString(4);
                int length = c.getInt(5);
                String path = c.getString(6);

                Music music = new Music(deviceId, serverId, title, artist, album, length, path);
                if(adapter.getItemByServerId(music.getServerId())==null) {
                    ContentValues values = new ContentValues();
                    values.put("server_id", -1);
                    db.update(DatabaseHelper.musicTable, values, "device_id = "+deviceId, null);
                    music.setServerId(-1);
                }
                adapter.addItem(music);
            }
            c.close();

            return result;

/*
            File dir = new File(Environment.getExternalStorageDirectory()+File.separator+"NightSky"+File.separator+"Musics");
            if(dir.exists()) {
                for(String path : dir.list()) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    Log.e("path", dir.getAbsolutePath()+File.separator+path);
                    try {
                        retriever.setDataSource(dir.getAbsolutePath()+File.separator+path);
                        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                        byte[] albumImage = retriever.getEmbeddedPicture();
                        int length = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

                        Music music = new Music(title, artist, album);
                        if(music.getTitle()==null || music.getTitle().isEmpty())
                            music.setTitle(path.split("\\.")[0]);
                        music.setLength(length);
                        items.add(music);
                        Log.e("added", path);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            publishProgress(items);
*/
        }

        @Override
        protected void onPostExecute(String result) {
            listTask = null;
            activity.showProgress(false);

            if(result==null) {
                Toast.makeText(context, R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(context, R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            adapter.notifyDataSetChanged();
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
                v = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_music, null);
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
                TextView tVartist = v.findViewById(R.id.tVartist);
                if(tVartist!=null)
                    tVartist.setText(music.getArtist());
            }

            return v;
        }

        public void addItem(Music item) {
            for(int i = 0; i<items.size(); i++) {
                if(items.get(i).equals(item)) {
                    items.get(i).addProperty(item);
                    return;
                }
            }
            items.add(item);
        }

        public Music getItemByServerId(int serverId) {
            for(Music music: items) {
                if(music.getServerId()==serverId)
                    return music;
            }
            return null;
        }
    }
}
