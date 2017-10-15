package com.holenet.nightsky.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.nightsky.NetworkManager;
import com.holenet.nightsky.Parser;
import com.holenet.nightsky.R;
import com.holenet.nightsky.music.MusicActivity;
import com.holenet.nightsky.secret.SecretActivity;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class UserActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    final static int REQUEST_FILE_UPLOAD = 101;

    DrawerLayout drawer;
    String username;
    TextView tVusername;

    // TODO: Add GalleryFragment (and SummaryFragment);
    FragmentManager fragmentManager;
    Fragment currentFragment;
    LoginFragment loginFragment;
    PostFragment postFragment;
    MusicFragment musicFragment;
    FileFragment fileFragment;

    RelativeLayout rLfragment;
    ProgressBar pBloading;
    UserLogoutTask logoutTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

        rLfragment = (RelativeLayout) findViewById(R.id.rLfragment);
        pBloading = (ProgressBar) findViewById(R.id.pBloading);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if(slideOffset<0.3)
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            }
        };

        drawer.addDrawerListener(toggle);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        tVusername = (TextView) findViewById(R.id.tVusername);

        fragmentManager = getSupportFragmentManager();

        if(requestPermission()) {
            showFragment(0);
        }

        serviceNotice(false);
    }

    private void serviceNotice(boolean restart) {
        Intent intent = new Intent(this, NoticeService.class);

        if(getSharedPreferences("settings_notice", 0).getBoolean(getString(R.string.pref_key_notice), false)) {
            boolean isRunning = false;
            if(restart) {
                stopService(intent);
            } else {
                ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if(NoticeService.class.getName().equals(service.service.getClassName())) {
//                    Toast.makeText(this, AlarmService.class.getName()+" : "+service.service.getClassName(), Toast.LENGTH_SHORT).show();
                        isRunning = true;
                    }
                }
            }
            if(!isRunning)
                startService(intent);
        } else {
            stopService(intent);
        }
    }

    private void onNoticeResult() {
        int recentId = getIntent().getIntExtra("recent_id", -1);
        if(recentId!=-1) {
            getIntent().putExtra("recent_id", -1);
            postFragment.recentId = recentId;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length<=0 || grantResults[0]!=PackageManager.PERMISSION_GRANTED) {
            finish();
        } else {
            drawer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showFragment(0);
                }
            }, 100);
        }
    }

    private boolean requestPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.error_permission, Toast.LENGTH_SHORT).show();
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.e("request", "??");
            } else {
                Log.e("request", "permmm");
            }
            Log.e("requestPermission", "");
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return false;
        }
        return true;
    }

    private void showFragment(int which) {
        if(menu!=null) {
            menu.findItem(R.id.mIupload).setVisible(which!=0 && which!=2);
        }
        switch(which) {
            case 0:
                if(currentFragment instanceof LoginFragment) return;
                currentFragment = loginFragment = new LoginFragment();
                break;
            case 2:
                if(currentFragment instanceof PostFragment) return;
                currentFragment = postFragment = new PostFragment();
                break;
            case 3:
                if(currentFragment instanceof MusicFragment) return;
                currentFragment = musicFragment = new MusicFragment();
                break;
            case 5:
                if(currentFragment instanceof FileFragment) return;
                currentFragment = fileFragment = new FileFragment();
                break;
            default:
                currentFragment = null;
        }
        String fragmentName = currentFragment.getClass().getSimpleName().replace("Fragment", "");
        getSupportActionBar().setTitle(fragmentName);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.rLfragment, currentFragment, fragmentName);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(currentFragment instanceof LoginFragment && ((LoginFragment) currentFragment).mode==LoginFragment.MODE_REGISTER) {
                ((LoginFragment) currentFragment).changeMode(false);
            } else {
                super.onBackPressed();
            }
        }
    }

    public void onLogged(boolean in, String username) {
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(drawer.getWindowToken(), 0);
        if(in) {
            this.username = username;
            if(tVusername==null)
                tVusername = (TextView) findViewById(R.id.tVusername);
            if(tVusername!=null)
                tVusername.setText(username);
        }
        drawer.setDrawerLockMode(in ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        showFragment(in ? 2 : 0);
        if(in) {
            onNoticeResult();
        }
    }

    public void requestLogin() {
        onLogged(false, null);
    }

    Menu menu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_user, menu);
        menu.findItem(R.id.mIsecret).setVisible(false);
        menu.findItem(R.id.mIsecret).setVisible(getSharedPreferences("secret", 0).getBoolean("activated", false));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id==R.id.mIsecret) {
            Intent intent = new Intent(this, SecretActivity.class);
            startActivity(intent);
        } else if(id==R.id.mIupload) {
            if(currentFragment instanceof FileFragment) {
                fileFragment.requestSelectFile();
            }
            if(currentFragment instanceof MusicFragment) {
                Intent intent = new Intent(this, MusicActivity.class);
                musicFragment.startActivityForResult(intent, MusicFragment.REQUEST_MUSIC_DETAIL);
            }
        } else if(id==R.id.mIexit) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        /*if (id == R.id.nVall) {
            showFragment(1);
        } else*/
        if(id==R.id.nVpost) {
            showFragment(2);
        } else if(id==R.id.nVmusic) {
            showFragment(3);
        } else if(id==R.id.nVgallery) {
//            showFragment(4);
        } else if(id==R.id.nVfile) {
            showFragment(5);
        } else if(id==R.id.nVsetting) {
            Intent intent = new Intent(UserActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if(id==R.id.nVlogout) {
            if(logoutTask==null) {
                showProgress(true);
                logoutTask = new UserLogoutTask();
                logoutTask.execute((Void) null);
            }
        } else if(id==R.id.nVexit) {
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        float alpha = rLfragment.getAlpha();
        rLfragment.clearAnimation();
        rLfragment.setAlpha(alpha);
        rLfragment.setVisibility(View.VISIBLE);
        rLfragment.animate().setDuration((long) (shortAnimTime*(show ? alpha : 1-alpha))).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rLfragment.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        alpha = pBloading.getAlpha();
        pBloading.clearAnimation();
        pBloading.setAlpha(alpha);
        pBloading.setVisibility(View.VISIBLE);
        pBloading.animate().setDuration((long) (shortAnimTime*(show ? 1-alpha : alpha)))
                .alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pBloading.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    public class UserLogoutTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return NetworkManager.get(UserActivity.this, NetworkManager.MAIN_DOMAIN+"accounts/logout/");
        }

        @Override
        protected void onPostExecute(final String result) {
            logoutTask = null;
            showProgress(false);

            if(result==null) {
                Toast.makeText(UserActivity.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }

            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.d("view_name", String.valueOf(viewName));
            onLogged(false, null);
        }

        @Override
        protected void onCancelled() {
            logoutTask = null;
            showProgress(false);
        }
    }
}
