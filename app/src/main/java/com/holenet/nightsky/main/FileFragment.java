package com.holenet.nightsky.main;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.nightsky.NetworkManager;
import com.holenet.nightsky.Parser;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class FileFragment extends Fragment {
    Context context;
    UserActivity activity;

    FileListTask listTask;
    FileUploadTask uploadTask;
    FileDownloadTask downloadTask;

    ListView lVfiles;
    FilesAdapter adapter;
    FloatingActionButton fABrefresh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        activity = (UserActivity) getActivity();

        View v = inflater.inflate(R.layout.fragment_file, container, false);

        lVfiles = v.findViewById(R.id.lVfiles);
        lVfiles.setEmptyView(v.findViewById(R.id.tVempty));
        adapter = new FilesAdapter(context, R.layout.item_file, new ArrayList<FileItem>());
        lVfiles.setAdapter(adapter);
        lVfiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(downloadTask!=null)
                    return;
                activity.showProgress(true);
                downloadTask = new FileDownloadTask(adapter.getItem(i));
                downloadTask.execute((Void) null);
            }
        });
        fABrefresh = v.findViewById(R.id.fABrefresh);
        fABrefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });

        refresh();

        return v;
    }

    public void requestSelectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), UserActivity.REQUEST_FILE_UPLOAD);
        } catch(android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "Please install a File Manager", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==UserActivity.REQUEST_FILE_UPLOAD) {
            if(resultCode==RESULT_OK) {
                Uri uri = data.getData();
                upload(uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void upload(Uri uri) {
        if(uploadTask!=null)
            return;
        activity.showProgress(true);
        uploadTask = new FileUploadTask(uri);
        uploadTask.execute((Void) null);
    }

    public void refresh() {
        if(listTask!=null) {
            return;
        }
        activity.showProgress(true);
        listTask = new FileListTask();
        listTask.execute((Void) null);
    }

    class FileListTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            return NetworkManager.get(context, NetworkManager.CLOUD_DOMAIN+"file/?JSON");
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

            List<FileItem> files = Parser.getFileListJSON(result);
            adapter.clear();
            for(FileItem file: files) {
                adapter.add(file);
            }
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onCancelled() {
            listTask = null;
            activity.showProgress(false);
        }
    }

    class FileUploadTask extends AsyncTask<Void, Void, Integer> {
        Uri uri;

        FileUploadTask(Uri uri) {
            this.uri = uri;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return NetworkManager.upload(context, uri, NetworkManager.CLOUD_DOMAIN+"file/upload/", "user_file");
        }

        @Override
        protected void onPostExecute(Integer result) {
            uploadTask = null;
            activity.showProgress(false);

            if(result==null || result==-1) {
                Toast.makeText(context, R.string.error_network, Toast.LENGTH_SHORT).show();
            } else {
                refresh();
            }
        }

        @Override
        protected void onCancelled() {
            uploadTask = null;
            activity.showProgress(false);
        }
    }

    class FileDownloadTask extends AsyncTask<Void, Void, Integer> {
        FileItem file;

        public FileDownloadTask(FileItem file) {
            this.file = file;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            File dir = new File(Environment.getExternalStorageDirectory()+File.separator+"NightSky"+File.separator+"Musics");
            if(!dir.exists())
                if(!dir.mkdirs())
                    return -1;
            File path = new File(dir.getAbsolutePath()+File.separator+file.getName());

            return NetworkManager.download(context, NetworkManager.CLOUD_DOMAIN+"file/download/"+file.getId()+"/", path);
        }

        @Override
        protected void onPostExecute(Integer result) {
            downloadTask = null;
            activity.showProgress(false);

            if(result==-1) {
                Toast.makeText(context, R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result==NetworkManager.RESULT_CODE_LOGIN_FAILED) {
                Toast.makeText(context, R.string.error_login, Toast.LENGTH_SHORT).show();
//                requestLogin();
                return;
            }

                Toast.makeText(context, "Downloaded.", Toast.LENGTH_SHORT).show();
                // TODO: update device_in on the item
        }
    }

    class FilesAdapter extends ArrayAdapter<FileItem> {
        private List<FileItem> items;

        public FilesAdapter(Context context, int resource, List<FileItem> items) {
            super(context, resource, items);
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if(v==null) {
                v = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_file, null);
            }

            FileItem file = items.get(position);
            if(file!=null) {
                TextView tVname = v.findViewById(R.id.tVname);
                if(tVname!=null)
                    tVname.setText(file.getName());
            }

            return v;
        }
    }
}