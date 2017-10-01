package com.holenet.nightsky;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PostReadFragment extends PostBaseFragment {
    Context context;
    boolean lastFocused;

    public static PostReadFragment newInstance(Post post) {
        PostReadFragment fragment = new PostReadFragment();
        Bundle args = new Bundle();
        args.putParcelable("Post", post);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        post = getArguments().getParcelable("Post");
    }

    ConstraintLayout cLrootContent;
    ScrollView sVpost;
    TextView tVtitle, tVauthor, tVdate, tVtime, tVtext;
    RelativeLayout rLcomments;
    ListView lVcomments;
    CommentsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();

        View v = inflater.inflate(R.layout.fragment_post_read, container, false);

//        cLrootContent = (ConstraintLayout) v.findViewById(R.id.cLrootContent);

        sVpost = (ScrollView) v.findViewById(R.id.sVpost);
        tVtitle = (TextView) v.findViewById(R.id.tVtitle);
        tVauthor = (TextView) v.findViewById(R.id.tVauthor);
        tVdate = (TextView) v.findViewById(R.id.tVdate);
        tVtime = (TextView) v.findViewById(R.id.tVtime);
        tVtext = (TextView) v.findViewById(R.id.tVtext);

        rLcomments = (RelativeLayout) v.findViewById(R.id.rLcomments);
        lVcomments = (ListView) v.findViewById(R.id.lVcomments);
        adapter = new CommentsAdapter(context, R.layout.item_comment, new ArrayList<Comment>());
        lVcomments.setAdapter(adapter);
        lVcomments.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(context, "comment No."+position+" clicked", Toast.LENGTH_SHORT).show();
            }
        });
        lVcomments.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                Log.e("onScroll", i+" : "+i1+" : "+i2);
                lastFocused = i+i1==i;
            }
        });

        refresh();

        return v;
    }

    void refresh() {
        if(tVtitle!=null)
            tVtitle.setText(post.getTitle());
        if(tVauthor!=null)
            tVauthor.setText(post.getAuthor());
        if(tVdate!=null)
            tVdate.setText(post.getDatetime()[0]);
        if(tVtime!=null)
            tVtime.setText(post.getDatetime()[1]);
        if(tVtext!=null)
            tVtext.setText(post.getText());
        if(lVcomments!=null) {
            adapter.setItems(post.getComments());
            adapter.notifyDataSetChanged();
        }
        if(rLcomments!=null) {
            showComments(commentsVisible);
        }
        if(lVcomments!=null)
            scrollComments();
    }

    ValueAnimator anim;
    boolean commentsVisible = false;
    void showComments(final boolean show) {
        if(commentsVisible==show)
            return;
        commentsVisible = show;
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        if (anim != null && anim.isRunning())
            anim.cancel();
        final float weight = ((LinearLayout.LayoutParams)sVpost.getLayoutParams()).weight;
        ValueAnimator.setFrameDelay(24);
        anim = ValueAnimator.ofFloat(weight, show ? 0 : 1);
        anim.setDuration((long)(shortAnimTime*(show ? weight : 1-weight)));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (float) valueAnimator.getAnimatedValue();
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) sVpost.getLayoutParams();
                params.weight = val;
                sVpost.setLayoutParams(params);
                params = (LinearLayout.LayoutParams) rLcomments.getLayoutParams();
                params.weight = 1-val;
                rLcomments.setLayoutParams(params);
            }
        });
        anim.start();
    }

    void scrollComments() {
        Log.e("scrollComments", lastFocused+"");
        if(lastFocused) {
            lVcomments.setSelection(adapter.getCount()-1);
        }
    }

    class CommentsAdapter extends ArrayAdapter<Comment> {
        private List<Comment> items;

        CommentsAdapter(Context context, int layout, List<Comment> items) {
            super(context, layout, items);
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if(v==null) {
                v = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_comment, null);
            }

            Comment comment = items.get(position);
            if(comment!=null) {
                TextView tVauthor = (TextView) v.findViewById(R.id.tVauthor);
                if(tVauthor!=null)
                    tVauthor.setText(comment.getAuthor());
                TextView tVdatetime = (TextView) v.findViewById(R.id.tVdatetime);
                if(tVdatetime!=null)
                    tVdatetime.setText(TextUtils.join(" ", comment.getDatetime()));
                TextView tVtext = (TextView) v.findViewById(R.id.tVtext);
                if(tVtext!=null)
                    tVtext.setText(comment.getText());
            }

            return v;
        }

        public void setItems(List<Comment> items) {
            this.items.clear();
            for(int i=0; i<items.size(); i++) {
                this.items.add(items.get(i));
            }
        }
    }
}