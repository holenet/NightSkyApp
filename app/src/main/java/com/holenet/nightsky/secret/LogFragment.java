package com.holenet.nightsky.secret;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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

    boolean isFullScrolled;
    View footer;
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
        View v = inflater.inflate(R.layout.fragment_log, container, false);

        lVlogs = (ListView) v.findViewById(R.id.lVlogs);
        if(logs==null) {
            logs = new ArrayList<>();
        }
        adapter = new LogAdapter(getContext(), R.layout.item_log, logs);
        lVlogs.setAdapter(adapter);
        footer = new View(activity);
        lVlogs.addFooterView(footer);
        lVlogs.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                checkFullScroll();
//                Log.e("onScrollStateChanged", i+"/"+isFullScrolled);
            }
            @Override
            public void onScroll(AbsListView absListView, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
            }
        });
        lVlogs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(!multiMode) {
                    BaseLog log = adapter.getItem(i);
                    if(log instanceof ImageLog) {
                        ImageLog imageLog = (ImageLog) log;
                        if(imageLog.getDrawable()==null) {
                            ImageLoadTask loadTask = new ImageLoadTask((ImageView)view.findViewById(R.id.iVimage), imageLog);
                            loadTask.execute((Void) null);
                        } else {
                            activity.fullscreen(true, imageLog);
                        }
                    }

//                    String watchString = DatabaseHelper.getWatchString(getContext(), adapter.getItem(i));
//                    if(!watchString.isEmpty())
//                        Toast.makeText(activity, watchString, Toast.LENGTH_SHORT).show();
                }
            }
        });
        lVlogs.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                changeMultiMode(true);
                lVlogs.setItemChecked(i, true);
                return true;
            }
        });

        if(!today) {
            v.findViewById(R.id.cLnewLog).setVisibility(View.GONE);
        } else {
            iBimage = (ImageButton) v.findViewById(R.id.iBimage);
            iBimage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestSelectImage();
                }
            });
            iBlink = (ImageButton) v.findViewById(R.id.iBlink);
            iBlink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showWatchPicker(null);
                }
            });
            tVtitle = (TextView) v.findViewById(R.id.tVtitle);
            tVrange = (TextView) v.findViewById(R.id.tVrange);
            eTlogText = (EditText) v.findViewById(R.id.eTlogText);
            eTlogText.setText(activity.getSharedPreferences("log_settings", 0).getString("temp_log_text", ""));
            eTlogText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isFullScrolled) {
                        adapter.fullScroll(100, 200);
                    }
                }
            });
            eTlogText.addTextChangedListener(new TextWatcher() {
                int lastLineCount = eTlogText.getLineCount();
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }
                @Override
                public void afterTextChanged(Editable editable) {
                    saveLogText(false);
                    int newLineCount = eTlogText.getLineCount();
                    if(lastLineCount!=newLineCount) {
                        if(isFullScrolled) {
                            adapter.fullScroll(50, 50);
                        }
                        lastLineCount = newLineCount;
                    }
                }
            });
            eTlogText.requestFocus();
            bTlogSave = (Button) v.findViewById(R.id.bTlogSave);
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

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (LogActivity) context;
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveLogText(false);
    }

    private void saveLogText(boolean empty) {
        if(eTlogText!=null) {
            SharedPreferences.Editor editor = activity.getSharedPreferences("log_settings", 0).edit();
            editor.putString("temp_log_text", empty ? "" : eTlogText.getText().toString());
            editor.apply();
        }
    }

    private boolean checkFullScroll() {
        return isFullScrolled = lVlogs.getChildCount()>0 && lVlogs.getChildAt(lVlogs.getChildCount()-1)==footer;
    }

    public void refresh() {
        if(loadTask!=null)
            return;
        loadTask = new LogLoadTask();
        loadTask.execute((Void) null);
    }

    boolean multiMode = false;
    public void changeMultiMode(boolean multiMode) {
        if(this.multiMode==multiMode)
            return;
        this.multiMode = multiMode;
        if(multiMode) {
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
                            new AlertDialog.Builder(activity)
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
                    changeMultiMode(false);
                    num = 0;
                }
            });
        } else {
            for(int i=0; i<adapter.getCount(); i++) {
                lVlogs.setItemChecked(i, false);
            }
            lVlogs.setChoiceMode(ListView.CHOICE_MODE_NONE);
        }
    }

    private void showWatchPicker(final List<BaseLog> logs) {
        final List<Watch> watches = DatabaseHelper.getWatchList(activity);
        for(Watch watch: watches) {
            DatabaseHelper.updateWatch(activity, watch);
        }
        final String[] items = new String[watches.size()+2];
        items[0] = "Create New Watch";
        items[items.length-1] = "Don't Link Any Watch";
        for(int i=1; i<watches.size()+1; i++) {
            Watch watch = watches.get(i-1);
            items[i] = watch.toString();
        }
        new AlertDialog.Builder(activity)
                .setTitle("Choose a Watch")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i==0) {
                            showPiecePicker(logs);
                        } else if(i==items.length-1) {
                            if(logs==null) {
                                tVtitle.setText("");
                                tVrange.setText("");
                                bTlogSave.setTag(null);
                            } else {
                                if(linkTask!=null) {
                                    Toast.makeText(activity, R.string.error_try_later, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                linkTask = new WatchLinkTask(null, logs);
                                linkTask.execute((Void) null);
                            }
                        } else {
                            Watch watch = watches.get(i-1);
                            if(logs==null) {
                                tVtitle.setText(watch.getPiece().getTitle());
                                tVrange.setText(watch.getRange());
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
        final List<Piece> pieces = DatabaseHelper.getPieceList(activity);
        String[] items = new String[pieces.size()+1];
        items[0] = "Register New Piece";
        for(int i=1; i<pieces.size()+1; i++) {
            items[i] = pieces.get(i-1).getTitle();
        }
        new AlertDialog.Builder(activity)
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
        final LinearLayout layout = (LinearLayout)(((LayoutInflater)activity.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_register_piece, null));
        final EditText eTtitle = (EditText) layout.findViewById(R.id.eTtitle);
        final EditText eTcomment = (EditText) layout.findViewById(R.id.eTcomment);
        new AlertDialog.Builder(activity)
                .setTitle("Register New Piece")
                .setView(layout)
                .setPositiveButton("register", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String title = eTtitle.getText().toString();
                        if(title.isEmpty()) {
                            Toast.makeText(activity, "Title of piece can not be blank", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String comment = eTcomment.getText().toString();

                        if(registerTask!=null) {
                            Toast.makeText(activity, R.string.error_try_later, Toast.LENGTH_SHORT).show();
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
        final LinearLayout layout = (LinearLayout)(((LayoutInflater)activity.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_range_picker, null));
        final NumberPicker nPstart = (NumberPicker) layout.findViewById(R.id.nPstart);
        final NumberPicker nPend = (NumberPicker) layout.findViewById(R.id.nPend);
        final EditText eTetc = (EditText) layout.findViewById(R.id.eTetc);
        nPstart.setMinValue(0);
        nPstart.setMaxValue(1000);
        nPstart.setWrapSelectorWheel(false);
        nPstart.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                if(nPend.getValue()<i1)
                    nPend.setValue(i1);
            }
        });
        nPend.setMinValue(0);
        nPend.setMaxValue(1000);
        nPend.setWrapSelectorWheel(false);
        nPend.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                if(nPstart.getValue()>i1)
                    nPstart.setValue(i1);
            }
        });
        eTetc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if(s.length()>0) {
                    nPstart.setEnabled(false);
                    nPend.setEnabled(false);
                } else {
                    nPstart.setEnabled(true);
                    nPend.setEnabled(true);
                }
            }
        });
        new AlertDialog.Builder(activity)
                .setTitle("Pick Range")
                .setView(layout)
                .setPositiveButton("link", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int start = nPstart.getValue();
                        int end = nPend.getValue();
                        String etc = eTetc.getText().toString();

                        Watch watch;
                        if(etc.isEmpty()) {
                            if(start>end) {
                                Toast.makeText(activity, "End should not be larger than Start", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            watch = new Watch(piece, start, end);
                        } else {
                            watch = new Watch(piece, etc);
                        }

                        if(logs==null) {
                            tVtitle.setText(piece.getTitle());
                            tVrange.setText(watch.getRange());
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

    public void requestSelectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a Image to Upload"), REQUEST_IMAGE_SELECT);
        } catch(android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, "Please install a File Manager", Toast.LENGTH_SHORT).show();
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
                return NetworkManager.get(activity, NetworkManager.SECRET_DOMAIN+"log/list/");
            else
                return NetworkManager.get(activity, NetworkManager.SECRET_DOMAIN+"log/list/"+date.split(" ")[0]+"/");
        }

        @Override
        protected void onPostExecute(String result) {
            loadTask = null;
            activity.updateProgress();

            if(result==null) {
                Toast.makeText(activity, R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(activity, R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            List<BaseLog> logs = Parser.getLogsJSON(result);
            adapter.setItems(logs);
            adapter.notifyDataSetChanged();
            if(today) {
                adapter.fullScroll(500, 150);
            }
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

        LogSaveTask(Uri uri) {
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
                if(log.getPk()==0) {
                    String result = NetworkManager.post(activity, NetworkManager.SECRET_DOMAIN+"log/new/text/", data);
                    publishProgress(result);
                    if(result!=null && !result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                        BaseLog log = Parser.getLogJSON(result);
                        List<BaseLog> logs = new ArrayList<>();
                        logs.add(log);
                        return new WatchLinkTask(watch, logs).doInBackground();
                    }
                } else
                    publishProgress(NetworkManager.post(activity, NetworkManager.SECRET_DOMAIN+"log/edit/text/"+log.getPk()+"/", data));
            } else {
                publishProgress(String.valueOf(NetworkManager.upload(activity, NetworkManager.SECRET_DOMAIN+"log/new/image/", "image", uri)));
            }

            if(watch!=null) {
                List<BaseLog> logs = new ArrayList<>();
                logs.add(log);
                return new WatchLinkTask(watch, logs).doInBackground();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String result = values[0];
            Log.e("onProgressUpdate", result+"");

            if(result==null) {
                cancel(true);
                Toast.makeText(activity, R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                cancel(true);
                Toast.makeText(activity, R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            BaseLog log = Parser.getLogJSON(result);
            if(log!=null) {
                if(adapter.findByPk(log.getPk())==null) {
                    this.log = log;
                    adapter.add(log);
                }
                adapter.notifyDataSetChanged();
                adapter.fullScroll(500, 150);
                saveLogText(true);
            } else {
                Toast.makeText(activity, R.string.error_unknown, Toast.LENGTH_LONG).show();
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
                Toast.makeText(activity, R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(activity, R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            boolean isNew = watch.getPk()==0;
            watch = Parser.getWatchJSON(result);
            if(watch!=null) {
                if(isNew) {
                    SQLiteDatabase db = new DatabaseHelper(activity).getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("pk", watch.getPk());
                    values.put("piece_pk", watch.getPiece().getPk());
                    values.put("start", watch.getStart());
                    values.put("end", watch.getEnd());
                    values.put("etc", watch.getEtc());
                    values.put("date", watch.getDate());
                    db.insert(DatabaseHelper.watchTable, null, values);
                    db.close();
                }
                log.setWatch(watch);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(activity, R.string.error_unknown, Toast.LENGTH_SHORT).show();
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
                publishProgress(NetworkManager.get(activity, NetworkManager.SECRET_DOMAIN+"log/delete/"+log.getPk()+"/"));
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String result = values[0];
            if(result==null) {
                cancel(true);
                Toast.makeText(activity, R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                cancel(true);
                Toast.makeText(activity, R.string.error_login, Toast.LENGTH_SHORT).show();
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
            if(isEmpty() && !today) {
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
            return NetworkManager.post(activity, NetworkManager.SECRET_DOMAIN+"piece/new/", data);
        }

        @Override
        protected void onPostExecute(String result) {
            registerTask = null;
            activity.updateProgress();

            if(result==null) {
                Toast.makeText(activity, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(activity, R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            piece = Parser.getPieceJSON(result);
            if(piece!=null) {
                SQLiteDatabase db = new DatabaseHelper(activity).getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("pk", piece.getPk());
                values.put("title", piece.getTitle());
                db.insert(DatabaseHelper.pieceTable, null, values);
                db.close();

                showRangePicker(piece, logs);
            } else {
                Toast.makeText(activity, R.string.error_unknown, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            registerTask = null;
            activity.updateProgress();
        }
    }

    private class WatchLinkTask extends AsyncTask<Void, String, String> {
        Watch watch;
        List<BaseLog> logs;
        int success;

        public WatchLinkTask(Watch watch, List<BaseLog> logs) {
            this.watch = watch;
            this.logs = logs;
            this.success = 0;
        }

        @Override
        protected void onPreExecute() {
            activity.updateProgress();
        }

        @Override
        protected String doInBackground(Void... voids) {
            if(watch==null) {
                for(BaseLog log: logs) {
                    publishProgress(NetworkManager.get(activity, NetworkManager.SECRET_DOMAIN+"log/cut/watch/"+log.getPk()+"/"));
                }
                return null;
            }

            Map<String, String> data = new LinkedHashMap<>();
            JSONArray ja = new JSONArray();
            for(BaseLog log: logs) {
                ja.put(log.getPk());
            }
            data.put("logs", ja.toString());
            if(watch.getPk()==0) {
                data.put("piece", String.valueOf(watch.getPiece().getPk()));
                if(watch.getEtc()!=null) {
                    data.put("etc", watch.getEtc());
                } else {
                    data.put("start", String.valueOf(watch.getStart()));
                    data.put("end", String.valueOf(watch.getEnd()));
                }
                return NetworkManager.post(activity, NetworkManager.SECRET_DOMAIN+"watch/new/", data);
            } else {
                return NetworkManager.post(activity, NetworkManager.SECRET_DOMAIN+"watch/add/logs/"+watch.getPk()+"/", data);
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String result = values[0];

            if(result==null) {
                Toast.makeText(activity, R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(activity, R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            BaseLog log = Parser.getLogJSON(result);
            if(log!=null) {
                success++;
                adapter.replace(log);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(activity, R.string.error_unknown, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            linkTask = null;
            activity.updateProgress();

            if(watch==null) {
                Toast.makeText(activity, success+"/"+logs.size()+" Success", Toast.LENGTH_SHORT).show();
            }
            if(result==null) {
                Toast.makeText(activity, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(activity, R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            boolean isNew = watch.getPk()==0;
            watch = Parser.getWatchJSON(result);
            if(watch!=null) {
                if(isNew) {
                    SQLiteDatabase db = new DatabaseHelper(activity).getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("pk", watch.getPk());
                    values.put("piece_pk", watch.getPiece().getPk());
                    values.put("start", watch.getStart());
                    values.put("end", watch.getEnd());
                    values.put("etc", watch.getEtc());
                    values.put("date", watch.getDate());
                    db.insert(DatabaseHelper.watchTable, null, values);
                    db.close();
                }
                for(BaseLog log: logs) {
                    log.setWatch(watch);
                    adapter.replace(log);
                }
                adapter.notifyDataSetChanged();

                Toast.makeText(activity, logs.size()+" Logs linked to this Watch", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, R.string.error_unknown, Toast.LENGTH_SHORT).show();
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
            File imageFile = new File(log.getAbsolutePath(activity));
            File imageDir = imageFile.getParentFile();
            if(!imageDir.exists())
                if(!imageDir.mkdirs())
                    return -1;
            return NetworkManager.download(activity, NetworkManager.SECRET_DOMAIN+"/log/download/image/"+log.getPk()+"/", imageFile);
        }

        @Override
        protected void onPostExecute(Integer result) {
            downloadTasks.remove(this);
            activity.updateProgress();

            if(result==null) {
                Toast.makeText(activity, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }
            if(result==NetworkManager.RESULT_CODE_LOGIN_FAILED) {
                Toast.makeText(activity, R.string.error_login, Toast.LENGTH_SHORT).show();
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

    private class ImageLoadTask extends AsyncTask<Void, Void, Drawable> {
        ImageView iVimage;
        ImageLog log;

        public ImageLoadTask(ImageView iVimage, ImageLog log) {
            this.iVimage = iVimage;
            this.log = log;
        }

        @Override
        protected void onPreExecute() {
            if(iVimage.getTag()==log) {
                iVimage.setImageResource(R.drawable.ic_error_outline_black_24dp);
                iVimage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iVimage.setMaxHeight(iVimage.getWidth()/16*9);
                // TODO: set image width and height
            }
        }

        @Override
        protected Drawable doInBackground(Void... params) {
            Drawable drawable = Drawable.createFromPath(log.getAbsolutePath(activity));
            log.setDrawable(drawable);
            return drawable;
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            if(iVimage.getTag()==log) {
                iVimage.setImageDrawable(drawable);
                iVimage.setMaxHeight(100000000);
                if(isFullScrolled) {
                    adapter.fullScroll(100, 100);
                }
            }
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

            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(LAYOUT_INFLATER_SERVICE);

            BaseLog log = items.get(position);

            if(v==null)
                v = inflater.inflate(R.layout.item_log, null);

            if(log!=null) {
                TextView tVdatetime = (TextView) v.findViewById(R.id.tVdatetime);
                if(date==null)
                    tVdatetime.setText(log.getCreatedAt());
                else
                    tVdatetime.setText(Parser.getSimpleTime(log.getCreatedAt()));
                TextView tVwatch = (TextView) v.findViewById(R.id.tVwatch);
                if(DatabaseHelper.updateWatch(activity, log.getWatch())) {
                    tVwatch.setText(log.getWatch().toString());
                } else {
                    tVwatch.setText("");
                }
                TextView tVtext = (TextView) v.findViewById(R.id.tVtext);
                ImageView iVimage = (ImageView) v.findViewById(R.id.iVimage);
                tVtext.setVisibility(log instanceof TextLog ? View.VISIBLE : View.GONE);
                iVimage.setVisibility(log instanceof ImageLog ? View.VISIBLE : View.GONE);
                if(log instanceof TextLog) {
                    TextLog textLog = (TextLog) log;
                    tVtext.setText(textLog.getText());
                } else if(log instanceof ImageLog) {
                    ImageLog imageLog = (ImageLog) log;
                    File imageFile = new File(imageLog.getAbsolutePath(activity));
                    if(imageFile.exists()) {
                        iVimage.setTag(imageLog);
                        iVimage.setAdjustViewBounds(true);
                        if(imageLog.getDrawable()==null) {
                            ImageLoadTask loadTask = new ImageLoadTask(iVimage, imageLog);
                            loadTask.execute((Void) null);
                        } else {
                            iVimage.setImageDrawable(imageLog.getDrawable());
                        }
                    } else {
                        iVimage.setAdjustViewBounds(false);
                        ImageDownloadTask downloadTask = new ImageDownloadTask(imageLog);
                        downloadTask.execute((Void) null);
                        downloadTasks.add(downloadTask);
                    }
                }
            }
            v.setTag(log);
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

        public boolean replace(BaseLog log) {
            for(int i=0; i<items.size(); i++) {
                if(items.get(i).getPk()==log.getPk()) {
                    items.set(i, log);
                    return true;
                }
            }
            return false;
        }

        public BaseLog findByPk(int pk) {
            for(BaseLog log: items) {
                if(log.getPk()==pk)
                    return log;
            }
            return null;
        }

        public void fullScroll(final int duration, final int delay) {
            lVlogs.postDelayed(new Runnable() {
                @Override
                public void run() {
                    lVlogs.smoothScrollBy(100000, duration);
                    isFullScrolled = true;
                }
            }, delay);
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
