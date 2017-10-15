package com.holenet.nightsky.music;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.nightsky.DatabaseHelper;
import com.holenet.nightsky.NetworkManager;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.Music;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class MusicActivity extends AppCompatActivity implements MusicService.CallbackInterface, NavigationView.OnNavigationItemSelectedListener {
    private MusicService service;
    private boolean bound = false;

    DrawerLayout drawer;

    FragmentManager fragmentManager;
    Fragment currentFragment;
    MusicNowFragment nowFragment;
    MusicListFragment listFragment;
    MusicAllFragment allFragment;

//    MusicUploadTask uploadTask;
    MusicDownloadTask downloadTask;

    RelativeLayout rLfragment;
    ProgressBar pBloading;

    SeekBar sBseek;
    TextView tVcurr, tVtotal;
    TextView tVtitle, tVartist;
    Button bTplay, bTprev, bTnext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if(slideOffset<0.3)
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        rLfragment = (RelativeLayout) findViewById(R.id.rLfragment);
        pBloading = (ProgressBar) findViewById(R.id.pBloading);

        sBseek = (SeekBar) findViewById(R.id.sBseek);
        sBseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b)
                    service.seekTo(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        tVcurr = (TextView) findViewById(R.id.tVcurr);
        tVtotal = (TextView) findViewById(R.id.tVtotal);
        tVtitle = (TextView) findViewById(R.id.tVtitle);
        tVartist = (TextView) findViewById(R.id.tVartist);
        bTplay = (Button) findViewById(R.id.bTplay);
        bTplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bound) {
                    bTplay.setText(service.playOrPause() ? "Pause" : "Play");
                } else {
                    Log.e("bTplay", "error");
                }
            }
        });
        bTprev = (Button) findViewById(R.id.bTprev);
        bTprev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bound) {
                    bTplay.setText(service.prev() ? "Pause" : "Play");
                } else {
                    Log.e("bTprev", "error");
                }
            }
        });
        bTnext = (Button) findViewById(R.id.bTnext);
        bTnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bound) {
                    bTplay.setText(service.next() ? "Pause" : "Play");
                } else {
                    Log.e("bTnext", "error");
                }
            }
        });

        fragmentManager = getSupportFragmentManager();

        showFragment(0);
    }

    private void showFragment(int which) {
        switch(which) {
            case 0:
                if(currentFragment instanceof MusicNowFragment) return;
                currentFragment = nowFragment = new MusicNowFragment();
                getSupportActionBar().setTitle("Now Playing");
                break;
            case 1:
                if(currentFragment instanceof MusicListFragment) return;
                currentFragment = listFragment = new MusicListFragment();
                getSupportActionBar().setTitle("Play Lists");
                break;
            case 2:
                if(currentFragment instanceof MusicAllFragment) return;
                currentFragment = allFragment = new MusicAllFragment();
                getSupportActionBar().setTitle("All Musics");
                break;
            default:
                currentFragment = null;
                getSupportActionBar().setTitle("ERROR");
        }
        if(menu!=null) {
            menu.findItem(R.id.mIadd).setVisible(currentFragment instanceof MusicListFragment);
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.rLmusicFragment, currentFragment, currentFragment.getClass().getSimpleName());
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id==R.id.nVnow) {
            showFragment(0);
        } else if(id==R.id.nVlist) {
            showFragment(1);
        } else if(id==R.id.nVall) {
            showFragment(2);
        } else if(id==R.id.nVsetting) { // TODO: add folder, album, artist etc.
            // TODO: start music player setting activity
            Toast.makeText(service, "settings_music", Toast.LENGTH_SHORT).show();
        } else if(id==R.id.nVback) {
            finish();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    boolean showing;
    public void showProgress(boolean show) {
        if(showing==show)
            return;
        showing = show;

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        float alpha = pBloading.getAlpha();
        pBloading.clearAnimation();
        pBloading.setAlpha(alpha);
        pBloading.animate().setDuration((long)(shortAnimTime*(show ? 1-alpha : alpha))).alpha(show ? 1 : 0);
    }

    protected void requestLogin() {
        setResult(NetworkManager.RESULT_CODE_LOGIN_FAILED);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        startService(intent);

        try {
            thread.start();
        } catch(Exception e){}
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(bound) {
            unbindService(connection);
            bound = false;
        }

        thread.kill();
        try {
            thread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    Menu menu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_music, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed(); break;
            case R.id.mIadd:
                listFragment.createList();
                break;
            case R.id.mIrefresh:
                if(currentFragment instanceof MusicNowFragment)
                    nowFragment.refresh();
                if(currentFragment instanceof MusicListFragment)
                    listFragment.refresh();
                if(currentFragment instanceof MusicAllFragment)
                    allFragment.refresh();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) iBinder;
            service = binder.bind(MusicActivity.this);
            bound = true;
            onMusicChanged(service.getCurrentMusic());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
        }
    };

    @Override
    public void onMusicChanged(Music music) {
        if(music!=null) {
            sBseek.setMax(music.getLength());
            tVtotal.setText(music.getLengthFormat());
            tVtitle.setText(music.getTitle());
            tVartist.setText(music.getArtist());
        } else {
            sBseek.setMax(0);
            sBseek.setProgress(0);
            tVtotal.setText(Music.formatSeconds(0));
            tVtitle.setText("No music");
            tVartist.setText("");
        }
    }

    TThread thread = new TThread();
    class TThread extends  Thread {
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(bound) {
                    int currSec = service.currentSec();
                    sBseek.setProgress(currSec);
                    tVcurr.setText(Music.formatSeconds(currSec/1000));
                }
            }
        };
        boolean quit;

        @Override
        public void run() {
            while(!quit) {
                handler.sendEmptyMessage(0);
                try {
                    Thread.sleep(137);
                } catch(Exception e) {}
            }
        }

        public void kill() {
            quit = true;
        }
    }

    protected void notifyCurrListChanged() {
        if(bound) {
            service.reset();
        }
    }

    protected void notifyCurrMusicChanged(int deviceId) {
        if(bound) {
            service.initializePlayersById(deviceId);
            service.start();
        }
    }

    static class MusicDownloadTask extends AsyncTask<Void, Void, Integer> {
        MusicActivity activity;
        Music music;

        MusicDownloadTask(MusicActivity activity, Music music) {
            this.activity = activity;
            this.music = music;
        }

        @Override
        protected void onPreExecute() {
            activity.showProgress(true);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            File file = new File(Environment.getExternalStorageDirectory()+File.separator+music.getPath());
            File dir = file.getParentFile();
            if(!dir.exists()) {
                if(!dir.mkdirs())
                    return -1;
            }

            int result = NetworkManager.download(activity, NetworkManager.CLOUD_DOMAIN+"file/download/"+music.getServerId(), file);
            if(result==-1) return result;

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(file.getAbsolutePath());
                int length = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                music.setLength(length);
            } catch(Exception e) {
                e.printStackTrace();
            }

            ContentValues values = new ContentValues();
            values.put("server_id", music.getServerId());
            values.put("title", music.getTitle());
            values.put("artist", music.getArtist());
            values.put("album", music.getAlbum());
            values.put("length", music.getLength());
            values.put("path", music.getPath());
            SQLiteDatabase db = new DatabaseHelper(activity).getWritableDatabase();
            db.insert(DatabaseHelper.musicTable, null, values);

            return 200;
        }

        @Override
        protected void onPostExecute(Integer result) {
            activity.downloadTask = null;
            activity.showProgress(false);

            if(result==-1) {
                Toast.makeText(activity, R.string.error_network, Toast.LENGTH_SHORT).show();
                return;
            }
            if(result==NetworkManager.RESULT_CODE_LOGIN_FAILED) {
                Toast.makeText(activity, R.string.error_login, Toast.LENGTH_SHORT).show();
                activity.requestLogin();
                return;
            }

            if(activity.currentFragment instanceof MusicNowFragment)
                activity.nowFragment.refresh();
            if(activity.currentFragment instanceof MusicAllFragment)
                activity.allFragment.refresh();
            // TODO: call refresh method of other fragments
        }

        @Override
        protected void onCancelled() {
            activity.downloadTask = null;
            activity.showProgress(false);
        }
    }
}
