package com.holenet.nightsky.secret;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.holenet.nightsky.R;

public class LogActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pagerAdapter = new PagerAdapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(pagerAdapter);
    }

    class PagerAdapter extends FragmentStatePagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = LogFragment.newInstance("2107-10-10");
            return fragment;
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }
    }
}
