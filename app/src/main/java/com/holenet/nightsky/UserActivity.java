package com.holenet.nightsky;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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

public class UserActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    final static int REQUEST_FILE_UPLOAD = 101;

    DrawerLayout drawer;
    String username;
    TextView tVusername;

    FragmentManager fragmentManager;
    LoginFragment loginFragment;
    AllFragment allFragment;
    PostsFragment postsFragment;

    RelativeLayout rLfragment;
    ProgressBar pBloading;
    UserLogoutTask logoutTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rLfragment = (RelativeLayout) findViewById(R.id.rLfragment);
        pBloading = (ProgressBar) findViewById(R.id.pBloading);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if(slideOffset<0.3)
                    ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            }
        };

        drawer.addDrawerListener(toggle);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        tVusername = (TextView) findViewById(R.id.tVusername);

        fragmentManager = getSupportFragmentManager();

        loginFragment = new LoginFragment();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.rLfragment, loginFragment);
        fragmentTransaction.commit();
    }

    private void showFragment(int which) {
        Fragment fragment = fragmentManager.getFragments().get(0);
        switch (which) {
            case 0: if(fragment instanceof LoginFragment) return;
                fragment = loginFragment = new LoginFragment(); break;
            case 1: if(fragment instanceof AllFragment) return;
                fragment = allFragment = new AllFragment(); break;
            case 2: if(fragment instanceof PostsFragment) return;
                fragment = postsFragment = new PostsFragment(); break;
            default: fragment = null;
        }
        String fragmentName = fragment.getClass().getSimpleName().replace("Fragment", "");
        getSupportActionBar().setTitle(fragmentName);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.rLfragment, fragment, fragmentName);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment fragment = fragmentManager.getFragments().get(0);
            if(fragment instanceof LoginFragment && ((LoginFragment) fragment).mode==LoginFragment.MODE_REGISTER) {
                ((LoginFragment) fragment).changeMode(false);
            } else {
                super.onBackPressed();
            }
        }
    }

    public void onLogged(boolean in, String username) {
        // TODO: hide upload and download menuItem when user is not logged in
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(drawer.getWindowToken(), 0);
        if(in) {
            this.username = username;
            if(tVusername==null)
                tVusername = (TextView) findViewById(R.id.tVusername);
            if(tVusername!=null)
                tVusername.setText(username);
        }
        drawer.setDrawerLockMode(in ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        showFragment(in ? 2 : 0);
    }

    public void requestLogin() {
        onLogged(false, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.mIupload) {
            if(fragmentManager.getFragments().get(0) instanceof AllFragment) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                try {
                    startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), REQUEST_FILE_UPLOAD);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "Please install a File Manager", Toast.LENGTH_SHORT).show();
                }
            }
        } else if(id == R.id.mIdownload) {
        } else if(id == R.id.mIexit) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nVall) {
            showFragment(1);
        } else if (id == R.id.nVpost) {
            showFragment(2);
        } else if (id == R.id.nVmusic) {
//            showFragment(musicFragment);
        } else if (id == R.id.nVgallery) {
//            showFragment(galleryFragment);
        }

        if (id == R.id.nVsetting) {
            Intent intent = new Intent(UserActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nVlogout) {
            if(logoutTask==null) {
                showProgress(true);
                logoutTask = new UserLogoutTask();
                logoutTask.execute((Void) null);
            }
        } else if (id == R.id.nVexit) {
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
        rLfragment.animate().setDuration((long)(shortAnimTime*(show ? alpha : 1-alpha))).alpha(
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
        pBloading.animate().setDuration((long)(shortAnimTime*(show ? 1-alpha : alpha))).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
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
