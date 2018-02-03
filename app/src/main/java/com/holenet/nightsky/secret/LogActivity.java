package com.holenet.nightsky.secret;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.holenet.nightsky.DatabaseHelper;
import com.holenet.nightsky.NetworkManager;
import com.holenet.nightsky.Parser;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.ImageLog;
import com.holenet.nightsky.item.Watch;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;

public class LogActivity extends AppCompatActivity {
    private class FragInfo {
        String date;
        LogFragment fragment;

        FragInfo(String date, LogFragment fragment) {
            this.date = date;
            this.fragment = fragment;
        }

        public void setToday() {
            date = Parser.getTodayDate();
            if(fragment!=null && fragment.isAdded())
                fragment.setToday();
        }
    }
    List<FragInfo> fragInfos;

    DateListTask listTask;
    WatchUpdateTask watchUpdateTask;

    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;

    private ProgressBar pBloading;

    private ConstraintLayout cLfullscreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(Parser.getTodayDate());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

        fragInfos = new ArrayList<>();

        pagerAdapter = new PagerAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            int lastCurrent = -1;
            @Override
            public void onPageSelected(int position) {
                if(position<fragInfos.size()) {
                    getSupportActionBar().setTitle(fragInfos.get(position).date);
                    if(lastCurrent!=-1 && lastCurrent<fragInfos.size()) {
                        if(fragInfos.get(lastCurrent).fragment!=null) {
                            android.view.ActionMode actionMode = fragInfos.get(lastCurrent).fragment.multiChoiceAction;
                            if(actionMode!=null) {
                                actionMode.finish();
                            }
                        }
                    }
                    updateProgress();
                }
                lastCurrent = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        pBloading = (ProgressBar) findViewById(R.id.pBloading);

        cLfullscreen = (ConstraintLayout) findViewById(R.id.cLfullscreen);
        cLfullscreen.findViewById(R.id.bTexit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fullscreen(false, null);
            }
        });
        cLfullscreen.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // TODO: implement pinch-zoom and drag-move and double-click-zoom
                return true;
            }
        });

        refresh(true);
    }

    private void refresh(boolean first) {
        if(listTask!=null)
            return;
        listTask = new DateListTask(first);
        listTask.execute((Void) null);
        if(first) {
            if(watchUpdateTask==null) {
                watchUpdateTask = new WatchUpdateTask();
                watchUpdateTask.execute((Void) null);
            }
        }
    }

    Menu menu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.mIrefresh:
                refresh(false);
                if(fragInfos.size()>0) {
                    FragInfo info = fragInfos.get(viewPager.getCurrentItem());
                    if(info.fragment!=null && info.fragment.isAdded()) {
                        info.fragment.refresh();
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    boolean showing;
    public void updateProgress() {
        boolean show = false;
        if(viewPager.getCurrentItem()<fragInfos.size()) {
            FragInfo info = fragInfos.get(viewPager.getCurrentItem());
            if(info.fragment!=null) {
                LogFragment fragment = info.fragment;
                show = fragment.loadTask!=null
                        || fragment.saveTask!=null
                        || fragment.deleteTask!=null
                        || fragment.registerTask!=null
                        || fragment.linkTask!=null;
            }
        }
        if(listTask!=null || watchUpdateTask!=null) {
            show = true;
        }
        if(showing==show)
            return;
        showing = show;

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        float alpha = pBloading.getAlpha();
        pBloading.clearAnimation();
        pBloading.setAlpha(alpha);
        pBloading.animate().setDuration((long)(shortAnimTime*(show ? 1-alpha : alpha))).alpha(show ? 1 : 0);
    }

    protected void fullscreen(boolean show, ImageLog log) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(show ? Color.BLACK : getResources().getColor(R.color.colorPrimaryDark));
        }
        cLfullscreen.setVisibility(show ? View.VISIBLE : View.GONE);
        if(show) {
            ImageView iVimage = (ImageView) cLfullscreen.findViewById(R.id.iVimage);
            iVimage.setImageURI(Uri.fromFile(new File(log.getAbsolutePath(this))));
            iVimage.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    Log.e("onTouch", event.getPointerCount()+": "+event.getAction()+"/"+event.getActionMasked());
                    return true;
                }
            });
            getSupportActionBar().hide();
        } else {
            getSupportActionBar().show();
        }
    }

    protected void requestPurge(LogFragment fragment) {
        Log.e("onPurge", fragment.date+"/"+Parser.getTodayDate());
        for(int i=0; i<fragInfos.size(); i++) {
            if(fragInfos.get(i).fragment==fragment) {
                fragInfos.remove(i);
                pagerAdapter.notifyDataSetChanged();
                return;
            }
        }
    }

    protected void requestLogin() {
        setResult(NetworkManager.RESULT_CODE_LOGIN_FAILED);
        finish();
    }

    private class DateListTask extends AsyncTask<Void, Void, String> {
        boolean first;

        public DateListTask(boolean first) {
            this.first = first;
        }

        @Override
        protected void onPreExecute() {
            updateProgress();
        }

        @Override
        protected String doInBackground(Void... voids) {
            return NetworkManager.get(LogActivity.this, NetworkManager.SECRET_DOMAIN+"log/date/list/");
        }

        @Override
        protected void onPostExecute(String result) {
            listTask = null;
            updateProgress();

            List<String> dates = Parser.getDatesJSON(result);
            fragInfos.clear();
            for(String date: dates) {
                fragInfos.add(new FragInfo(date, null));
            }
            String todayDate = Parser.getTodayDate();
            if(fragInfos.size()==0 || !fragInfos.get(fragInfos.size()-1).date.equals(todayDate)) {
                fragInfos.add(new FragInfo(todayDate, null));
            }
            pagerAdapter.notifyDataSetChanged();
            if(first) {
                viewPager.setCurrentItem(pagerAdapter.getCount()-1);
            }
        }

        @Override
        protected void onCancelled() {
            listTask = null;
            updateProgress();
        }
    }

    private class WatchUpdateTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            updateProgress();
        }

        @Override
        protected String doInBackground(Void... voids) {
            return NetworkManager.get(LogActivity.this, NetworkManager.SECRET_DOMAIN+"watch/list/");
        }

        @Override
        protected void onPostExecute(String result) {
            watchUpdateTask = null;
            updateProgress();

            List<Watch> watches = Parser.getWatchesJSON(result);
            if(watches==null) {
                return;
            }
            SQLiteDatabase db = new DatabaseHelper(LogActivity.this).getWritableDatabase();
            db.delete(DatabaseHelper.watchTable, null, null);
            for(Watch watch: watches) {
                ContentValues values = new ContentValues();
                values.put("pk", watch.getPk());
                values.put("piece_pk", watch.getPiece().getPk());
                values.put("start", watch.getStart());
                values.put("end", watch.getEnd());
                values.put("etc", watch.getEtc());
                values.put("date", watch.getDate());
                db.insert(DatabaseHelper.watchTable, null, values);
            }
            db.close();
        }

        @Override
        protected void onCancelled() {
            watchUpdateTask = null;
            updateProgress();
        }
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            FragInfo info = fragInfos.get(position);
            if(info.fragment==null) {
                info.fragment = LogFragment.newInstance(info.date);
                Log.e("newFragment", position+"");
            }
            return info.fragment;
        }

        @Override
        public int getCount() {
            return fragInfos.size();
        }

        @Override
        public int getItemPosition(Object object) {
            LogFragment fragment = (LogFragment) object;
            String date = fragment.date;
            int position = -1;
            for(int i=0; i<fragInfos.size(); i++) {
                FragInfo info = fragInfos.get(i);
                if(info.date.equals(date)) {
                    position = i;
                    break;
                }
            }
            if(position>=0)
                return position;
            else
                return POSITION_NONE;
        }
    }

    TThread todayUpdateThread = new TThread();
    class TThread extends Thread {
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String todayDate = Parser.getTodayDate();
                if(fragInfos.size()==0)
                    return;
                FragInfo lastFrag = fragInfos.get(fragInfos.size()-1);
                if(!todayDate.equals(lastFrag.date) && lastFrag.fragment!=null) {
                    boolean isCurrentLast = viewPager.getCurrentItem()==pagerAdapter.getCount()-1;
                    if(!lastFrag.fragment.isEmpty()) {
                        fragInfos.add(fragInfos.size()-1, new FragInfo(lastFrag.date, null));
                    }
                    lastFrag.setToday();
                    pagerAdapter.notifyDataSetChanged();
                    if(isCurrentLast) {
                        viewPager.setCurrentItem(pagerAdapter.getCount()-1);
                    }
                }
            }
        };

        boolean quit;

        @Override
        public void run() {
            while(!quit) {
                handler.sendEmptyMessage(0);
                try {
                    Thread.sleep(1000);
                } catch(Exception e) {}
            }
        }

        public void kill() {
            quit = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            todayUpdateThread.start();
        } catch(Exception e) {}
    }

    @Override
    protected void onStop() {
        super.onStop();

        todayUpdateThread.kill();
        try {
            todayUpdateThread.join();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
