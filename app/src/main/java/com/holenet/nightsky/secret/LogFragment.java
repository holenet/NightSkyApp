package com.holenet.nightsky.secret;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.nightsky.DatabaseHelper;
import com.holenet.nightsky.NetworkManager;
import com.holenet.nightsky.Parser;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.BaseLog;
import com.holenet.nightsky.item.ImageLog;
import com.holenet.nightsky.item.Piece;
import com.holenet.nightsky.item.TextLog;
import com.holenet.nightsky.item.Watch;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static java.net.HttpURLConnection.HTTP_OK;

public class LogFragment extends Fragment {
    final static int REQUEST_IMAGE_SELECT = 222;

    LogActivity activity;
    String date;
    boolean today;
    List<BaseLog> logs;

    public static LogFragment newInstance(String date) {
        LogFragment fragment = new LogFragment();
        Bundle args = new Bundle();
        args.putString("date", date);
        fragment.setArguments(args);
        return fragment;
    }

    ListView lVlogs;
    LogAdapter adapter;
    ImageButton iBimage, iBlink;
    TextView tVtitle, tVrange;
    EditText eTlogText;
    Button bTlogSave;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        date = getArguments().getString("date");
        today = date.equals(Parser.getTodayDate());
    }

    LogLoadTask loadTask;
    LogSaveTask saveTask;
    LogDeleteTask deleteTask;

    PieceRegisterTask registerTask;

    WatchLinkTask linkTask;

    List<ImageDownloadTask> downloadTasks = new ArrayList<>();

    ActionMode multiChoiceAction;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (LogActivity) getActivity();

        View v = inflater.inflate(R.layout.fragment_log, container, false);

        lVlogs = v.findViewById(R.id.lVlogs);
        if(logs==null) {
            logs = new ArrayList<>();
        }
        adapter = new LogAdapter(getContext(), R.layout.item_log_text, logs);
        lVlogs.setAdapter(adapter);
        lVlogs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(!multiMode) {
                    Watch watch = adapter.getItem(i).getWatch();
                    if(watch!=null) {
                        SQLiteDatabase db = new DatabaseHelper(getContext()).getReadableDatabase();
                        Cursor c = db.rawQuery("select piece_pk, start, end from "+DatabaseHelper.watchTable+" where pk = "+watch.getPk(), null);
                        c.moveToNext();
                        int piecePk = c.getInt(0);
                        int start = c.getInt(1);
                        int end = c.getInt(2);
                        Cursor cc = db.rawQuery("select title from "+DatabaseHelper.pieceTable+" where pk = "+piecePk, null);
                        cc.moveToNext();
                        String title = cc.getString(0);
                        cc.close();
                        c.close();

                        Toast.makeText(activity, title+" ["+start+"-"+end+"]", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        lVlogs.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                changeMode(true);
                lVlogs.setItemChecked(i, true);
                return true;
            }
        });

        if(!today) {
            v.findViewById(R.id.cLnewLog).setVisibility(View.GONE);
        } else {
            iBimage = v.findViewById(R.id.iBimage);
            iBimage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestSelectFile();
                }
            });
            iBlink = v.findViewById(R.id.iBlink);
            iBlink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showWatchPicker(null);
                }
            });
            tVtitle = v.findViewById(R.id.tVtitle);
            tVrange = v.findViewById(R.id.tVrange);
            eTlogText = v.findViewById(R.id.eTlogText);
            bTlogSave = v.findViewById(R.id.bTlogSave);
            bTlogSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(eTlogText.getText().length()==0)
                        return;
                    if(saveTask!=null || deleteTask!=null) {
                        Toast.makeText(activity, R.string.error_try_later, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveTask = new LogSaveTask(new TextLog(eTlogText.getText().toString()));
                    saveTask.execute((Void) null);
                }
            });
        }

        refresh();

        return v;
    }

    public void refresh() {
        if(loadTask!=null || deleteTask!=null)
            return;
        loadTask = new LogLoadTask();
        loadTask.execute((Void) null);
    }

    boolean multiMode = false;
    public void changeMode(boolean multiMode) {
        if(this.multiMode==multiMode)
            return;
        this.multiMode = multiMode;
        if(this.multiMode) {
            lVlogs.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            lVlogs.setMultiChoiceModeListener(new ListView.MultiChoiceModeListener() {
                private int num = 0;

                @Override
                public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
                    Log.e("ActionMode", "itemCheckedStateChanged "+b);
                    num += b ? 1 : -1;
                    actionMode.setTitle(num+" Logs selected");
                }

                @Override
                public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                    Log.e("ActionMode", "create");
                    actionMode.getMenuInflater().inflate(R.menu.menu_log_list, menu);
                    multiChoiceAction = actionMode;
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    Log.e("ActionMode", "prepare");
                    return false;
                }

                @Override
                public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
                    Log.e("ActionMode", "itemClicked");
                    switch(menuItem.getItemId()) {
                        case R.id.mIlink:
                            SparseBooleanArray checkedItems = lVlogs.getCheckedItemPositions();
                            List<BaseLog> logs = new ArrayList<>(checkedItems.size());
                            for(int i=0; i<checkedItems.size(); i++) {
                                if(checkedItems.valueAt(i)) {
                                    logs.add(adapter.getItem(checkedItems.keyAt(i)));
                                }
                            }
                            showWatchPicker(logs);
                            actionMode.finish();
                            break;
                        case R.id.mIdelete:
                            new AlertDialog.Builder(getContext())
                                    .setTitle(date)
                                    .setMessage("Delete "+num+" Logs permanently")
                                    .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if(deleteTask!=null) {
                                                Toast.makeText(activity, R.string.error_try_later, Toast.LENGTH_SHORT).show();
                                            }
                                            SparseBooleanArray checkedItems = lVlogs.getCheckedItemPositions();
                                            List<BaseLog> logs = new ArrayList<>(checkedItems.size());
                                            for(i=0; i<checkedItems.size(); i++) {
                                                if(checkedItems.valueAt(i)) {
                                                    logs.add(adapter.getItem(checkedItems.keyAt(i)));
                                                }
                                            }
                                            deleteTask = new LogDeleteTask(logs);
                                            deleteTask.execute((Void) null);
                                            actionMode.finish();
                                        }
                                    })
                                    .setNegativeButton("cancel", null)
                                    .show();
                            break;
                    }
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    Log.e("ActionMode", "destroy");
                    multiChoiceAction = null;
                    num = 0;
                }
            });
        }
    }

    private void showWatchPicker(final List<BaseLog> logs) {
        final SQLiteDatabase db = new DatabaseHelper(getContext()).getReadableDatabase();
        final List<Watch> watches = new ArrayList<>();
        Cursor c = db.rawQuery("select pk, piece_pk, start, end, date from "+DatabaseHelper.watchTable, null);
        for(int i=0; i<c.getCount(); i++) {
            c.moveToNext();
            int pk = c.getInt(0);
            int piecePk = c.getInt(1);
            int start = c.getInt(2);
            int end = c.getInt(3);
            String date = c.getString(4);
            Cursor cc = db.rawQuery("select title from "+DatabaseHelper.pieceTable+" where pk = "+piecePk, null);
            cc.moveToNext();
            String title = cc.getString(0);
            cc.close();
            watches.add(new Watch(pk, new Piece(piecePk, title), start, end, date));
        }
        c.close();
        String[] items = new String[watches.size()+1];
        items[0] = "Create New Watch";
        for(int i=1; i<watches.size()+1; i++) {
            Watch watch = watches.get(i-1);
            String title = watch.getPiece().getTitle();
            int start = watch.getStart();
            int end = watch.getEnd();
            items[i] = String.format("%s [%d-%d]", title, start, end);
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Choose a Watch")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i==0) {
                            showPiecePicker(logs);
                        } else {
                            Watch watch = watches.get(i-1);
                            if(logs==null) {
                                tVtitle.setText(watch.getPiece().getTitle());
                                tVrange.setText(watch.getStart()+"-"+watch.getEnd());
                                bTlogSave.setTag(watch);
                            } else {
                                if(linkTask!=null) {
                                    Toast.makeText(activity, R.string.error_try_later, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                linkTask = new WatchLinkTask(watch, logs);
                                linkTask.execute((Void) null);
                            }
                        }
                    }
                })
                .show();
    }

    private void showPiecePicker(final List<BaseLog> logs) {
        final SQLiteDatabase db = new DatabaseHelper(getContext()).getReadableDatabase();
        final List<Piece> pieces = new ArrayList<>();
        Cursor c = db.rawQuery("select pk, title from "+DatabaseHelper.pieceTable, null);
        for(int i=0; i<c.getCount(); i++) {
            c.moveToNext();
            int pk = c.getInt(0);
            String title = c.getString(1);
            pieces.add(new Piece(pk, title));
        }
        c.close();
        String[] items = new String[pieces.size()+1];
        items[0] = "Register New Piece";
        for(int i=1; i<pieces.size()+1; i++) {
            items[i] = pieces.get(i-1).getTitle();
        }
        new AlertDialog.Builder(getContext())
                .setTitle("Choose a piece")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i==0) {
                            showRegisterPieceDialog(logs);
                        } else {
                            showRangePicker(pieces.get(i-1), logs);
                        }
                    }
                }).show();
    }

    private void showRegisterPieceDialog(final List<BaseLog> logs) {
        final LinearLayout layout = (LinearLayout)(((LayoutInflater)getContext().getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_register_piece, null));
        final EditText eTtitle = layout.findViewById(R.id.eTtitle);
        final EditText eTcomment = layout.findViewById(R.id.eTcomment);
        new AlertDialog.Builder(getContext())
                .setTitle("Register New Piece")
                .setView(layout)
                .setPositiveButton("register", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String title = eTtitle.getText().toString();
                        if(title.isEmpty()) {
                            Toast.makeText(getContext(), "Title of piece can not be blank", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String comment = eTcomment.getText().toString();

                        if(registerTask!=null) {
                            Toast.makeText(getContext(), R.string.error_try_later, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        registerTask = new PieceRegisterTask(new Piece(title, comment), logs);
                        registerTask.execute((Void) null);
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    private void showRangePicker(final Piece piece, final List<BaseLog> logs) {
        final LinearLayout layout = (LinearLayout)(((LayoutInflater)getContext().getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_range_picker, null));
        final NumberPicker nPstart = layout.findViewById(R.id.nPstart);
        nPstart.setMinValue(1);
        final NumberPicker nPend = layout.findViewById(R.id.nPend);
        nPend.setMinValue(1);
        new AlertDialog.Builder(getContext())
                .setTitle("Pick Range")
                .setView(layout)
                .setPositiveButton("link", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int start = nPstart.getValue();
                        int end = nPstart.getValue();
                        if(start>end) {
                            Toast.makeText(activity, "End should not be larger than Start", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Watch watch = new Watch(piece, start, end);
                        if(logs==null) {
                            tVtitle.setText(piece.getTitle());
                            tVrange.setText(start+"-"+end);
                            bTlogSave.setTag(watch);
                        } else {
                            if(linkTask!=null) {
                                Toast.makeText(activity, R.string.error_try_later, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            linkTask = new WatchLinkTask(watch, logs);
                            linkTask.execute((Void) null);
                        }
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    public void requestSelectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), REQUEST_IMAGE_SELECT);
        } catch(android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "Please install a File Manager", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_IMAGE_SELECT) {
            if(resultCode==RESULT_OK) {
                Uri uri = data.getData();
                if(saveTask!=null || deleteTask!=null) {
                    Toast.makeText(activity, R.string.error_try_later, Toast.LENGTH_SHORT).show();
                    return;
                }
                saveTask = new LogSaveTask(uri);
                saveTask.execute((Void) null);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class LogLoadTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            activity.updateProgress();
        }

        @Override
        protected String doInBackground(Void... voids) {
            if(date==null)
                return NetworkManager.get(getContext(), NetworkManager.SECRET_DOMAIN+"log/list/");
            else
                return NetworkManager.get(getContext(), NetworkManager.SECRET_DOMAIN+"log/list/"+date+"/");
        }

        @Override
        protected void onPostExecute(String result) {
            loadTask = null;
            activity.updateProgress();

            if(result==null) {
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(getContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            List<BaseLog> logs = Parser.getLogListJSON(result);
            adapter.setItems(logs);
            if(today)
                lVlogs.setSelection(adapter.getCount()-1);
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onCancelled() {
            loadTask = null;
            activity.updateProgress();
        }
    }

    private class LogSaveTask extends AsyncTask<Void, String, String> {
        BaseLog log;
        Watch watch;
        Uri uri;

        LogSaveTask(BaseLog log) {
            this.log = log;
        }

        public LogSaveTask(Uri uri) {
            this.uri = uri;
        }

        @Override
        protected void onPreExecute() {
            activity.updateProgress();
            eTlogText.setTag(eTlogText.getText().toString());
            eTlogText.setText("");
            watch = (Watch) bTlogSave.getTag();

            tVtitle.setText("");
            tVrange.setText("");
        }

        @Override
        protected String doInBackground(Void... voids) {
            Map<String, String> data = new LinkedHashMap<>();
            if(log!=null && log instanceof TextLog) {
                TextLog textLog = (TextLog) log;
                data.put("text", textLog.getText());
                if(log.getPk()==0)
                    publishProgress(NetworkManager.post(getContext(), NetworkManager.SECRET_DOMAIN+"log/new/text/", data));
                else
                    publishProgress(NetworkManager.post(getContext(), NetworkManager.SECRET_DOMAIN+"log/edit/text/"+log.getPk()+"/", data));
            } else {
                publishProgress(String.valueOf(NetworkManager.upload(getContext(), NetworkManager.SECRET_DOMAIN+"log/new/image/", "image", uri)));
            }

            if(watch!=null) {
                for(int i=0; i<100; i++) {
                    if(log.getPk()!=0) {
                        List<BaseLog> logs = new ArrayList<>();
                        logs.add(log);
                        return new WatchLinkTask(watch, logs).doInBackground();
                    } else {
                        try {
                            Thread.sleep(50);
                        } catch(Exception e) {}
                    }
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String result = values[0];

            if(result==null) {
                cancel(true);
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                cancel(true);
                Toast.makeText(getContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            log = Parser.getLogJSON(result);
            if(log!=null) {
                adapter.add(log);
                adapter.notifyDataSetChanged();
                lVlogs.setSelection(adapter.getCount()-1);
            } else {
                Toast.makeText(getContext(), R.string.error_unknown, Toast.LENGTH_LONG).show();
                eTlogText.setText((String)eTlogText.getTag());
            }
        }

        @Override
        protected void onPostExecute(String result) {
            saveTask = null;
            activity.updateProgress();

            if(watch==null)
                return;
            if(result==null) {
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(getContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            boolean isNew = watch.getPk()==0;
            watch = Parser.getWatchJSON(result);
            if(watch!=null) {
                if(isNew) {
                    SQLiteDatabase db = new DatabaseHelper(getContext()).getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("pk", watch.getPk());
                    values.put("piece_pk", watch.getPiece().getPk());
                    values.put("start", watch.getStart());
                    values.put("end", watch.getEnd());
                    values.put("date", watch.getDate());
                    db.insert(DatabaseHelper.watchTable, null, values);
                }
            } else {
                Toast.makeText(getContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            saveTask = null;
            activity.updateProgress();
        }
    }

    private class LogDeleteTask extends AsyncTask<Void, String, Void> {
        List<BaseLog> logs;
        int success;

        LogDeleteTask(List<BaseLog> logs) {
            this.logs = logs;
            success = 0;
        }

        @Override
        protected void onPreExecute() {
            activity.updateProgress();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for(BaseLog log: logs) {
                publishProgress(NetworkManager.get(getContext(), NetworkManager.SECRET_DOMAIN+"log/delete/"+log.getPk()+"/"));
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String result = values[0];
            if(result==null) {
                cancel(true);
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                cancel(true);
                Toast.makeText(getContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            if(result.contains("Success")) {
                success++;
                int pk = Integer.parseInt(result.split(" ")[1]);
                adapter.remove(pk);
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            deleteTask = null;
            activity.updateProgress();

            Toast.makeText(activity, success+" Logs deleted."+(logs.size()>success?"\n"+(logs.size()-success)+"Logs not deleted.":""), Toast.LENGTH_SHORT).show();
            if(isEmpty() || !today) {
                activity.requestPurge(LogFragment.this);
            }
        }

        @Override
        protected void onCancelled() {
            deleteTask = null;
            activity.updateProgress();
        }
    }

    private class PieceRegisterTask extends AsyncTask<Void, Void, String> {
        Piece piece;
        List<BaseLog> logs;

        public PieceRegisterTask(Piece piece, List<BaseLog> logs) {
            this.piece = piece;
            this.logs = logs;
        }

        @Override
        protected void onPreExecute() {
            activity.updateProgress();
        }

        @Override
        protected String doInBackground(Void... voids) {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("title", piece.getTitle());
            data.put("comment", piece.getComment());
            return NetworkManager.post(getContext(), NetworkManager.SECRET_DOMAIN+"piece/new/", data);
        }

        @Override
        protected void onPostExecute(String result) {
            registerTask = null;
            activity.updateProgress();

            if(result==null) {
                Toast.makeText(getContext(), getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(getContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            piece = Parser.getPieceJSON(result);
            if(piece!=null) {
                SQLiteDatabase db = new DatabaseHelper(getContext()).getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("pk", piece.getPk());
                values.put("title", piece.getTitle());
                db.insert(DatabaseHelper.pieceTable, null, values);

                showRangePicker(piece, logs);
            } else {
                Toast.makeText(getContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            registerTask = null;
            activity.updateProgress();
        }
    }

    private class WatchLinkTask extends AsyncTask<Void, Void, String> {
        Watch watch;
        List<BaseLog> logs;

        public WatchLinkTask(Watch watch, List<BaseLog> logs) {
            this.watch = watch;
            this.logs = logs;
        }

        @Override
        protected void onPreExecute() {
            activity.updateProgress();
        }

        @Override
        protected String doInBackground(Void... voids) {
            Map<String, String> data = new LinkedHashMap<>();
            JSONArray ja = new JSONArray();
            for(BaseLog log: logs) {
                ja.put(log.getPk());
            }
            data.put("logs", ja.toString());
            if(watch.getPk()==0) {
                data.put("piece", String.valueOf(watch.getPiece().getPk()));
                data.put("start", String.valueOf(watch.getStart()));
                data.put("end", String.valueOf(watch.getEnd()));
                return NetworkManager.post(getContext(), NetworkManager.SECRET_DOMAIN+"watch/new/", data);
            } else {
                return NetworkManager.post(getContext(), NetworkManager.SECRET_DOMAIN+"watch/add/logs/"+watch.getPk()+"/", data);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            linkTask = null;
            activity.updateProgress();

            if(result==null) {
                Toast.makeText(getContext(), getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(getContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            boolean isNew = watch.getPk()==0;
            watch = Parser.getWatchJSON(result);
            if(watch!=null) {
                if(isNew) {
                    SQLiteDatabase db = new DatabaseHelper(getContext()).getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("pk", watch.getPk());
                    values.put("piece_pk", watch.getPiece().getPk());
                    values.put("start", watch.getStart());
                    values.put("end", watch.getEnd());
                    values.put("date", watch.getDate());
                    db.insert(DatabaseHelper.watchTable, null, values);
                }

                Toast.makeText(activity, logs.size()+" Logs linked to this Watch", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.error_unknown, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
            activity.updateProgress();
        }
    }

    private class ImageDownloadTask extends AsyncTask<Void, Void, Integer> {
        ImageLog log;

        public ImageDownloadTask(ImageLog log) {
            this.log = log;
        }

        @Override
        protected void onPreExecute() {
            activity.updateProgress();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            File imageFile = new File(log.getAbsolutePath());
            File imageDir = imageFile.getParentFile();
            if(!imageDir.exists())
                if(!imageDir.mkdirs())
                    return -1;
            return NetworkManager.download(getContext(), NetworkManager.SECRET_DOMAIN+"/log/download/image/"+log.getPk()+"/", imageFile);
        }

        @Override
        protected void onPostExecute(Integer result) {
            downloadTasks.remove(this);
            activity.updateProgress();

            if(result==null) {
                Toast.makeText(getContext(), getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }
            if(result==NetworkManager.RESULT_CODE_LOGIN_FAILED) {
                Toast.makeText(getContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            if(result==HTTP_OK) {
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(activity, R.string.error_unknown, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            downloadTasks.remove(this);
            activity.updateProgress();
        }
    }

    private class LogAdapter extends ArrayAdapter<BaseLog> {
        private List<BaseLog> items;

        LogAdapter(Context context, int layout, List<BaseLog> items) {
            super(context, layout, items);
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);

            BaseLog log = items.get(position);
            if(log!=null) {
                if(log instanceof TextLog) {
                    TextLog textLog = (TextLog) log;
                    if(v==null || !(v.getTag() instanceof TextLog)) {
                        v = inflater.inflate(R.layout.item_log_text, null);
                        TextView tVtext = v.findViewById(R.id.tVtext);
                        if(tVtext!=null)
                            tVtext.setText(textLog.getText());
                    }
                } else {
                    ImageLog imageLog = (ImageLog) log;
                    if(v==null || !(v.getTag() instanceof ImageLog)) {
                        v = inflater.inflate(R.layout.item_log_image, null);
                        ImageView iVimage = v.findViewById(R.id.iVimage);
                        File imageFile = new File(imageLog.getAbsolutePath());
                        if(imageFile.exists()) {
                            iVimage.setImageURI(Uri.fromFile(imageFile));
                        } else {
                            ImageDownloadTask downloadTask = new ImageDownloadTask(imageLog);
                            downloadTask.execute((Void) null);
                            downloadTasks.add(downloadTask);
                        }
                    }
                }
                TextView tVdatetime = v.findViewById(R.id.tVdatetime);
                if(tVdatetime!=null) {
                    if(date==null)
                        tVdatetime.setText(log.getCreatedAt());
                    else
                        tVdatetime.setText(Parser.getSimpleTime(log.getCreatedAt()));
                }
            }

            return v;
        }

        public void setItems(List<BaseLog> items) {
            this.items.clear();
            for(int i=0; i<items.size(); i++) {
                this.items.add(items.get(i));
            }
        }

        public void remove(int pk) {
            for(int i=0; i<items.size(); i++) {
                if(items.get(i).getPk()==pk) {
                    items.remove(items.get(i));
                    return;
                }
            }
        }
    }

    public void setToday() {
        date = Parser.getTodayDate();
        today = true;

        refresh();
    }

    public boolean isEmpty() {
        return adapter!=null && adapter.isEmpty();
    }
}
