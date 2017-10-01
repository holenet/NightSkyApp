package com.holenet.nightsky;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.holenet.nightsky.PostActivity.Mode.*;
import static com.holenet.nightsky.PostActivity.LoadState.*;

public class PostActivity extends AppCompatActivity {
    enum Mode {
        edit, read,
    }

    enum LoadState {
        unloaded, loading, loaded,
    }

    class FragInfo {
        Mode mode;
        LoadState loadState;
        Post post;

        public FragInfo(Mode mode, LoadState loadState, Post post) {
            this.mode = mode;
            this.loadState = loadState;
            this.post = post;
        }
    }

    List<FragInfo> fragInfos;
    List<PostBaseFragment> fragments;
    int currentPage;
    int lastPage;
    Mode mode = read;
    FragInfo tempFragInfo;

    CommentSendTask commentTask;

    private PagerAdapter pagerAdapter;
    private KeyDisableViewPager viewPager;

    private ProgressBar pBloading;
    private RelativeLayout rLcomment;
    private ConstraintLayout cLcomment;
    private EditText eTcommentText;
    private ImageButton bTsend;
    private Button bTtoggleComment;

    private int lastPosition;
    private float lastPositionOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fragInfos = new ArrayList<>();
        fragments = new ArrayList<>();
        for(int i=0; i<getIntent().getIntExtra("post_count_all", 0); i++) {
            fragInfos.add(new FragInfo(read, unloaded, null));
        }
        Log.e("post_count_all", fragInfos.size()+"");

        pagerAdapter = new PagerAdapter(getSupportFragmentManager());

        viewPager = (KeyDisableViewPager) findViewById(R.id.container);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            int scrollCount = 0;
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d("onPageScrolled", "pos:"+position+" curr:"+currentPage+" "+mode+" offset:"+positionOffset);
                lastPosition = position;
                lastPositionOffset = positionOffset;
                if(positionOffset==0.0f && scrollCount%3==0) {
                    updateProgress();
                }
            }

            @Override
            public void onPageSelected(int position) {
                Log.e("onPageSelected", position+"");
                currentPage = position;
                changeMode(fragInfos.get(position).mode);
                loadPageAround(position);
                if(mode==read) {
                    showComments(((PostReadFragment)fragments.get(currentPage)).commentsVisible);
                    if(fragInfos.get(position).post!=null)
                        bTtoggleComment.setText(String.format("Comments (%d)", fragInfos.get(position).post.getCommentCount()));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mode==edit;
            }
        });
        currentPage = fragInfos.size()-1-getIntent().getIntExtra("post_current_page", -1);
        viewPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(currentPage==fragInfos.size()) {
                    currentPage -= 1;
                    addPost();
                } else {
                    viewPager.setCurrentItem(currentPage, false);
                    loadPageAround(currentPage);
                }
            }
        }, 100);

        pBloading = (ProgressBar) findViewById(R.id.pBloading);
        rLcomment = (RelativeLayout) findViewById(R.id.rLcomments);
        cLcomment = (ConstraintLayout) findViewById(R.id.cLcomment);
        eTcommentText = (EditText) findViewById(R.id.eTcommentText);
        eTcommentText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                Log.e("onFocusChange", b+"");
                ((PostReadFragment)fragments.get(currentPage)).scrollComments();
            }
        });
        bTsend = (ImageButton) findViewById(R.id.bTsend);
        bTsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = eTcommentText.getText().toString();
                eTcommentText.setText("");
                if(text.isEmpty())
                    return;
                if(commentTask!=null)
                    return;
                Comment comment = new Comment();
                comment.setPostId(fragInfos.get(currentPage).post.getId());
                comment.setText(text);
                commentTask = new CommentSendTask(comment, currentPage);
                commentTask.execute((Void) null);
                updateProgress();
            }
        });
        bTtoggleComment = (Button) findViewById(R.id.bTtoggleComment);
        bTtoggleComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showComments(true);
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    Menu menu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id==android.R.id.home) {
            onBackPressed();
        } else if(id==R.id.mIedit) {
            if(fragInfos.get(currentPage).loadState!=loaded) {
                Toast.makeText(this, "Page is not loaded yet. Please wait...", Toast.LENGTH_SHORT).show();
                return true;
            }
            PostBaseFragment fragment = fragments.get(currentPage);
            if(fragment instanceof PostEditFragment) {
                Log.e("edit menu invoked", "in edit fragment");
                return true;
            }
            tempFragInfo = new FragInfo(read, loaded, fragment.post.copy());
            fragInfos.set(currentPage, new FragInfo(edit, loaded, fragment.post));
            changeMode(edit);
        } else if(id==R.id.mIaddOrPost) {
            Log.e("mIaddOrPost",mode+"");
            if(mode==edit) {
                attemptPost();
            } else {
                addPost();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateOptionItems() {
        if(menu==null)
            return;
        if(mode==edit) {
            menu.removeItem(R.id.mIedit);
            MenuItem item = menu.findItem(R.id.mIaddOrPost);
            item.setIcon(R.drawable.ic_open_in_browser_white_24dp);
            item.setTitle("Post");
        } else {
            menu.clear();
            getMenuInflater().inflate(R.menu.menu_post, menu);
        }
    }

    @Override
    public void onBackPressed() {
        if(mode==edit) {
            final boolean isAdd = ((PostEditFragment)fragments.get(currentPage)).post.getId()==-1;
            new AlertDialog.Builder(PostActivity.this)
                    .setTitle("You are editing")
                    .setIcon(R.drawable.ic_warning_black_24dp)
                    .setMessage("Modified data will NOT be saved.")
                    .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(isAdd) {
                                fragInfos.remove(currentPage);
                                ((PostEditFragment)fragments.get(currentPage)).clear();
                                fragments.get(currentPage).post.setId(91121);
                                pagerAdapter.notifyDataSetChanged();
                                viewPager.setCurrentItem(lastPage);
                            } else {
                                fragInfos.set(currentPage, tempFragInfo);
                            }
                            changeMode(read);
                        }
                    })
                    .setNegativeButton("cancel", null).show();
        } else {
            if(commentVisible)
                showComments(false);
            else
                super.onBackPressed();
        }
    }

    protected void addPost() {
        fragInfos.add(new FragInfo(edit, loaded, new Post()));
        pagerAdapter.notifyDataSetChanged();
        lastPage = currentPage;
        viewPager.setCurrentItem(fragInfos.size()-1);
        changeMode(edit);
    }

    protected void changeMode(final Mode nextMode) {
        Log.e("changeMode", mode+"/"+nextMode);
        if(mode==nextMode)
            return;
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mode = nextMode;
        getSupportActionBar().setTitle(nextMode.toString().substring(0,1).toUpperCase()+nextMode.toString().substring(1)+" Post");
        updateOptionItems();
        pagerAdapter.notifyDataSetChanged();

        if(rLcomment==null)
            return;
        float alpha = rLcomment.getAlpha();
        rLcomment.clearAnimation();
        rLcomment.setAlpha(alpha);
        rLcomment.setVisibility(View.VISIBLE);
        rLcomment.animate().setDuration((long)(shortAnimTime*(nextMode==edit ? alpha : 1-alpha))).alpha(
                nextMode==edit ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rLcomment.setVisibility(nextMode==edit ? View.GONE : View.VISIBLE);
            }
        });
        if(bTtoggleComment==null)
            return;
        alpha = bTtoggleComment.getAlpha();
        bTtoggleComment.clearAnimation();
        bTtoggleComment.setAlpha(alpha);
        bTtoggleComment.setVisibility(View.VISIBLE);
        bTtoggleComment.animate().setDuration((long)(shortAnimTime*(nextMode==edit ? alpha : 1-alpha))).alpha(
                nextMode==edit ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bTtoggleComment.setVisibility(nextMode==edit ? View.GONE : View.VISIBLE);
            }
        });
    }

    ValueAnimator anim;
    boolean commentVisible = false;
    protected void showComments(final boolean show) {
        Log.e("showComments", show+"");
        if(commentVisible==show)
            return;
        commentVisible = show;
        menu.findItem(R.id.mIedit).setVisible(!commentVisible);
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        final int heightBT = bTtoggleComment.getMeasuredHeight();
        Log.e("showComments", "height: "+heightBT);
        float y = bTtoggleComment.getTranslationY();
        Log.e("showComments", "y: "+y);
        bTtoggleComment.clearAnimation();
        bTtoggleComment.setVisibility(View.VISIBLE);
        bTtoggleComment.setTranslationY(y);
        bTtoggleComment.animate().setDuration((long)(shortAnimTime*(show ? 1-y/heightBT : y/heightBT)))
                .translationYBy(show ? heightBT-y : -y);

        float alpha = cLcomment.getAlpha();
        Log.e("showComments", "alpha: "+alpha);
        cLcomment.clearAnimation();
        cLcomment.setAlpha(alpha);
        cLcomment.setVisibility(View.VISIBLE);
        cLcomment.animate().setDuration((long)(shortAnimTime*(show ? 1-alpha : alpha)))
                .alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(!show) eTcommentText.setText("");
            }
        });

        if(anim!=null && anim.isRunning())
            anim.cancel();
        final int heightRL = cLcomment.getMeasuredHeight();
        ValueAnimator.setFrameDelay(24);
        anim = ValueAnimator.ofInt(heightRL, heightBT);
        anim.setDuration((long)(shortAnimTime*(show ? 1-alpha : alpha)));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (int) valueAnimator.getAnimatedValue();
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) cLcomment.getLayoutParams();
                params.height = val;
                cLcomment.setLayoutParams(params);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) cLcomment.getLayoutParams();
                params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                cLcomment.setLayoutParams(params);
            }
        });
        anim.start();

        ((PostReadFragment)fragments.get(currentPage)).showComments(show);
    }

    void attemptPost() {
        Log.e("attemptPost", currentPage+"/"+mode);
        FragInfo fragInfo = fragInfos.get(currentPage);
        if(fragInfo.mode==read) {
            Log.e("attemptPost invoked", "in read fragment");
            return;
        }
        if(fragInfo.loadState==loading) {
            Log.e("atteptPost invoked", "in loading state");
            return;
        }

        PostEditFragment pef = (PostEditFragment) fragments.get(currentPage);
        pef.save();
        Post post = pef.post.copy();

        if(post.getTitle().replace(" ", "").replace("\n", "").isEmpty() || post.getText().replace(" ", "").replace("\n", "").isEmpty()) {
            Toast.makeText(this, "You have to fill ALL fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        fragInfos.get(currentPage).loadState = loading;
        updateProgress();
        PostPostTask postTask = new PostPostTask(currentPage, post);
        postTask.execute((Void) null);
    }

    void loadPageAround(int position) {
        final int offset = 3;
        for(int i=-offset; i<1+offset; i++) {
            int offPosition = position+i;
            if(offPosition<0)
                continue;
            if(offPosition>=fragInfos.size())
                break;
            if(fragInfos.get(offPosition).loadState==unloaded) {
                fragInfos.get(offPosition).loadState = loading;
                PostLoadTask loadTask = new PostLoadTask(offPosition, false);
                loadTask.execute((Void) null);
                updateProgress();
            }
        }
    }

    void onFinishedLoad(int page, Post post) {
        if(isFinishing())
            return;
        fragInfos.set(page, new FragInfo(read, loaded, post));
        fragments.get(page).post = post;
        ((PostReadFragment)fragments.get(page)).refresh();
        if(pagerAdapter!=null)
            pagerAdapter.notifyDataSetChanged();
        if(page==currentPage)
            bTtoggleComment.setText("Comments ("+post.getCommentCount()+")");

    }

    boolean showing;
    private void updateProgress() {
        Log.e(" updateProgress", "showing: "+showing);

        boolean show = false;
        if(fragInfos.get(lastPosition).loadState==loading)
            show = true;
        if(lastPositionOffset!=0.0f && fragInfos.get(lastPosition+1).loadState==loading)
            show = true;
        if(commentTask!=null)
            show = true;
        Log.e(" updateProgress", "show: "+show);

        if(showing==show)
            return;
        showing = show;

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        float alpha = pBloading.getAlpha();
        pBloading.clearAnimation();
        pBloading.setAlpha(alpha);
        pBloading.animate().setDuration((long)(shortAnimTime*(show ? 1-alpha : alpha))).alpha(
                show ? 1 : 0);
    }

    private class PostPostTask extends AsyncTask<Void, Void, String> {
        int postNumber;
        Post post;

        public PostPostTask(int postNumber, Post post) {
            this.postNumber = postNumber;
            this.post = post;
        }

        @Override
        protected String doInBackground(Void... voids) {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("title", post.getTitle());
            data.put("text", post.getText());
            if(post.getId()!=-1)
                return NetworkManager.post(PostActivity.this, NetworkManager.CLOUD_DOMAIN+"post/"+post.getId()+"/edit/", data);
            else
                return NetworkManager.post(PostActivity.this, NetworkManager.CLOUD_DOMAIN+"post/new/", data);
        }

        @Override
        protected void onPostExecute(String result) {
            updateProgress();

            if(result==null) {
                Toast.makeText(PostActivity.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(PostActivity.this, R.string.error_login, Toast.LENGTH_SHORT).show();
                setResult(NetworkManager.RESULT_CODE_LOGIN_FAILED);
                finish();
                return;
            }

            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.d("view_name", String.valueOf(viewName));
            if(!"post_detail".equals(viewName)) {
                fragInfos.set(postNumber, new FragInfo(edit, loaded, post));
                pagerAdapter.notifyDataSetChanged();
            } else {
                fragInfos.set(postNumber, new FragInfo(read, loading, post));
                changeMode(read);
                PostLoadTask loadTask = new PostLoadTask(postNumber, false);
                loadTask.execute((Void) null);
            }
            updateProgress();
        }

        @Override
        protected void onCancelled() {
            updateProgress();
        }
    }

    private class PostLoadTask extends AsyncTask<Void, Void, String> {
        int postNumber;
        int postId;

        public PostLoadTask(int postNumber, boolean isId) {
            this.postNumber = postNumber;
            postId = isId ? postNumber : -1;
        }

        @Override
        protected String doInBackground(Void... voids) {
            Log.e("doInBackground/Load", "Num:"+postNumber+"Id:"+postId);
//            if(postId!=-1)
//                return NetworkManager.get(PostActivity.this, NetworkManager.CLOUD_DOMAIN+"post/"+postId+"/");
//            else
                return NetworkManager.get(PostActivity.this, NetworkManager.CLOUD_DOMAIN+"post/find-by-index/"+postNumber+"/");
        }

        @Override
        protected void onPostExecute(String result) {
            updateProgress();

            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(PostActivity.this, R.string.error_login, Toast.LENGTH_SHORT).show();
                setResult(NetworkManager.RESULT_CODE_LOGIN_FAILED);
                finish();
                return;
            }
            updateProgress();

            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.d("view_name", String.valueOf(viewName));

            Post post = Parser.getPostJSON(result);
            if(post!=null) {
                if(postNumber==currentPage) {
                    changeMode(read);
                }
                onFinishedLoad(postNumber, post);
            } else {
                Toast.makeText(PostActivity.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                fragInfos.set(postNumber, new FragInfo(read, unloaded, fragInfos.get(postNumber).post));
            }

            updateProgress();
        }

        @Override
        protected void onCancelled() {
            updateProgress();
        }
    }

    private class CommentSendTask extends AsyncTask<Void, Void, String> {
        Comment comment;
        int pageNumber;

        public CommentSendTask(Comment comment, int pageNumber) {
            this.comment = comment;
            this.pageNumber = pageNumber;
        }

        @Override
        protected String doInBackground(Void... voids) {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("text", comment.getText());
            return NetworkManager.post(PostActivity.this, NetworkManager.CLOUD_DOMAIN+"post/"+comment.getPostId()+"/comment/", data);
        }

        @Override
        protected void onPostExecute(String result) {
            commentTask = null;
            updateProgress();

            if(result==null) {
                Toast.makeText(PostActivity.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                Toast.makeText(PostActivity.this, R.string.error_login, Toast.LENGTH_LONG).show();
                setResult(NetworkManager.RESULT_CODE_LOGIN_FAILED);
                finish();
                return;
            }

            // TODO: scroll comments ListView by end
            String viewName = Parser.getMetaDataHTML(result, "view_name");
            Log.d("view_name", String.valueOf(viewName));
            if("post_detail".equals(viewName)) {
                fragInfos.set(pageNumber, new FragInfo(read, loading, fragInfos.get(pageNumber).post));
                PostLoadTask loadTask = new PostLoadTask(pageNumber, false);
                loadTask.execute((Void) null);
            }
            updateProgress();
        }
    }

    class PagerAdapter extends FragmentStatePagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.e("getItem", "begin/"+position);
            FragInfo fragInfo = fragInfos.get(position);
            Post post = fragInfo.post;
            if(fragInfo.loadState!=loaded)
                post = new Post(-404, "loading...", "", new String[] {"", ""}, "");
            PostBaseFragment item;
            if(fragInfo.mode==edit) {
                item = PostEditFragment.newInstance(post);
            } else {
                item = PostReadFragment.newInstance(post);
            }

            while(fragments.size()<fragInfos.size())
                fragments.add(PostReadFragment.newInstance(new Post()));
            fragments.set(position, item);
            Log.e("getItem", "end/\n"+item.getClass().getSimpleName()+"/"+post.getTitle());
            return item;
        }

        @Override
        public int getCount() {
            return fragInfos.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if(position<fragInfos.size())
                return fragInfos.get(position).post.getTitle();
            return null;
        }

        @Override
        public int getItemPosition(Object object) {
            Log.e("getItemPosition", "begin/");
            Mode fragMode = object instanceof PostEditFragment ? edit : read;
            int id = ((PostBaseFragment)object).post.getId();
            Log.e("getItemPosition", "middle/"+id);
            int position = -1;
            for(int i=0; i<fragInfos.size(); i++) {
                FragInfo fragInfo = fragInfos.get(i);
                if(fragInfo.loadState==loaded && fragInfo.mode==fragMode && fragInfo.post.getId()==id) {
                    position = i;
                    break;
                }
            }
            Log.e("getItemPosition", "end/"+position);
            if(position>=0)
                return position;
            else
                return POSITION_NONE;
        }
    }
}
