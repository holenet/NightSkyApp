package com.holenet.nightsky;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static android.app.Activity.RESULT_OK;

public class FileFragment extends Fragment {
    Context context;
    UserActivity activity;

    FileListTask listTask;
    FileUploadTask uploadTask;

    TextView tVcontent;
    FloatingActionButton fABrefresh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        activity = (UserActivity) getActivity();

        View v = inflater.inflate(R.layout.fragment_file, container, false);

        tVcontent = (TextView) v.findViewById(R.id.tVcontent);
        fABrefresh = (FloatingActionButton) v.findViewById(R.id.fABrefresh);
        fABrefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });

        refresh();

        return v;
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

    public class FileListTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            return NetworkManager.get(context, NetworkManager.CLOUD_DOMAIN+"file/");
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

            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.d("view_name", String.valueOf(viewName));
            tVcontent.setText("");
            List<String[]> files = Parser.getFileListHTML(result);
            for(String[] file: files) {
                tVcontent.append("path: "+file[0]+"\n");
                tVcontent.append(file[1]+"["+file[3]+"]\n");
                tVcontent.append(file[2]+"\n\n");
            }
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
            return NetworkManager.upload(context, uri, NetworkManager.CLOUD_DOMAIN+"file/upload/");
        }

        @Override
        protected void onPostExecute(Integer result) {
            uploadTask = null;
            activity.showProgress(false);

            Log.d("fileupload result", result+"");
            if(result==null || result==-1) {
                Toast.makeText(context, R.string.error_network, Toast.LENGTH_SHORT).show();
            } else {

            }
        }

        @Override
        protected void onCancelled() {
            uploadTask = null;
            activity.showProgress(false);
        }
    }
}