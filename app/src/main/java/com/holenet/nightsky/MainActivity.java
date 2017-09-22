package com.holenet.nightsky;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    final static int REQUEST_LOGIN = 100;
    final static int REQUEST_FILE_UPLOAD = 101;

    PostListTask listTask;
    UserLogoutTask logoutTask;
    FileUploadTask uploadTask;

    LinearLayout lLcontent;
    ProgressBar pBloading;
    TextView tVcontent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });

        pBloading = (ProgressBar) findViewById(R.id.pBloading);
        lLcontent = (LinearLayout) findViewById(R.id.lLcontent);
        tVcontent = (TextView) findViewById(R.id.tVcontent);

        refresh();
    }

    private void refresh() {
        if(listTask!=null)
            return;
        showProgress(true);
        listTask = new PostListTask();
        listTask.execute((Void) null);
    }

    private void logout() {
        if(logoutTask!=null)
            return;
        showProgress(true);
        logoutTask = new UserLogoutTask();
        logoutTask.execute((Void) null);
    }

    private void upload(Uri uri) {
        if(uploadTask!=null)
            return;
        showProgress(true);
        uploadTask = new FileUploadTask(uri);
        uploadTask.execute((Void) null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int MENU_ID = item.getItemId();

        if(MENU_ID==R.id.mIlogout) {
            logout();
        } else if(MENU_ID==R.id.mIupload) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            try {
                startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), REQUEST_FILE_UPLOAD);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "Please install a File Manager", Toast.LENGTH_SHORT).show();
            }
        } else if(MENU_ID==R.id.mIdownload) {
            Intent intent = new Intent();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_LOGIN) {
            refresh();
        } else if(requestCode==REQUEST_FILE_UPLOAD) {
            if(resultCode==RESULT_OK) {
                Uri uri = data.getData();
                Log.d("File Uri", uri.toString());
                Log.d("File Uri Path", uri.getPath());
                upload(uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_longAnimTime);

        float alpha = lLcontent.getAlpha();
        lLcontent.clearAnimation();
        lLcontent.setAlpha(alpha);
        lLcontent.setVisibility(View.VISIBLE);
        lLcontent.animate().setDuration((long)(shortAnimTime*(show ? alpha : 1-alpha))).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                lLcontent.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        alpha = pBloading.getAlpha();
        pBloading.clearAnimation();
        pBloading.setAlpha(alpha);
        pBloading.setVisibility(View.VISIBLE);
        pBloading.animate().setDuration((long)(shortAnimTime*(show ? 1-alpha : alpha))).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pBloading.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    public class PostListTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return NetworkManager.get(MainActivity.this, NetworkManager.MAIN_DOMAIN+"blog/");
        }

        @Override
        protected void onPostExecute(final String result) {
            listTask = null;
            showProgress(false);

            if(result==null) {
                Toast.makeText(MainActivity.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }

            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.d("view_name", String.valueOf(viewName));
            boolean isLogedin = !"login".equals(viewName);
            if(isLogedin) {
                tVcontent.setText("");
                List<Post> posts = Parser.getPostListHTML(result);
                for(Post post: posts) {
                    tVcontent.append(" "+post.getTitle()+" ["+post.getDatetime()+"]\n");
                    tVcontent.append(post.getText()+"\n\n");
                }
            } else {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivityForResult(intent, REQUEST_LOGIN);
            }
        }

        @Override
        protected void onCancelled() {
            listTask = null;
            showProgress(false);
        }
    }

    public class UserLogoutTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return NetworkManager.get(MainActivity.this, NetworkManager.MAIN_DOMAIN+"accounts/logout/");
        }

        @Override
        protected void onPostExecute(final String result) {
            logoutTask = null;
            showProgress(false);

            if(result==null) {
                Toast.makeText(MainActivity.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }

            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.d("view_name", String.valueOf(viewName));
            finish();
        }

        @Override
        protected void onCancelled() {
            logoutTask = null;
            showProgress(false);
        }
    }


    public class FileUploadTask extends AsyncTask<Void, Void, Integer> {
        Uri uri;

        public FileUploadTask(Uri uri) {
            this.uri = uri;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return NetworkManager.upload(MainActivity.this, uri, NetworkManager.MAIN_DOMAIN+"cowinfo/upload/");
        }

        @Override
        protected void onPostExecute(final Integer result) {
            uploadTask = null;
            showProgress(false);

            Log.d("result", ""+result);
            if(result==null || result==-1) {
                Toast.makeText(MainActivity.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            } else if(result== HttpURLConnection.HTTP_OK) {
                Toast.makeText(MainActivity.this, "File Uploaded.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Upload error...["+result+"]", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            uploadTask = null;
            showProgress(false);
        }
    }
}
