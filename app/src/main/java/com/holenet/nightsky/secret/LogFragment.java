package com.holenet.nightsky.secret;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.holenet.nightsky.R;
import com.holenet.nightsky.item.BaseLog;
import com.holenet.nightsky.item.ImageLog;
import com.holenet.nightsky.item.TextLog;

import java.util.ArrayList;
import java.util.List;

public class LogFragment extends Fragment {
    String date;

    public static LogFragment newInstance(String date) {
        LogFragment fragment = new LogFragment();
        Bundle args = new Bundle();
        args.putString("date", date);
        fragment.setArguments(args);
        return fragment;
    }

    ListView lVlogs;
    LogAdapter adapter;
    EditText eTlogText;
    Button bTlogSave;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        date = getArguments().getString("date");
    }

    LogLoadTask loadTask;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_log, container, false);

        lVlogs = v.findViewById(R.id.lVlogs);
        eTlogText = v.findViewById(R.id.eTlogText);
        bTlogSave = v.findViewById(R.id.bTlogSave);

        adapter = new LogAdapter(getContext(), R.layout.item_log_text, new ArrayList<BaseLog>());
        lVlogs.setAdapter(adapter);

        return v;
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
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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
                    }
                    // TODO: setImage
                }
                TextView tVdatetime = v.findViewById(R.id.tVdatetime);
                if(tVdatetime!=null)
                    tVdatetime.setText(log.getCreatedAt());
            }

            return v;
        }
    }

    private class LogLoadTask extends AsyncTask<Void, BaseLog, String> {
        @Override
        protected String doInBackground(Void... voids) {


            return null;
        }
    }
}
