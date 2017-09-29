package com.holenet.nightsky;

import android.animation.ValueAnimator;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PostReadFragment extends PostBaseFragment {
    Context context;

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

        cLrootContent = (ConstraintLayout) v.findViewById(R.id.cLrootContent);

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
        final int maxHeight = cLrootContent.getMeasuredHeight()/2;
        if(maxHeight==0)
            return;
        final int height = rLcomments.getMeasuredHeight();
        ValueAnimator.setFrameDelay(24);
        anim = ValueAnimator.ofFloat(height, show ? maxHeight : 0);
        anim.setDuration((long)(shortAnimTime*(show ? 1-height/maxHeight : height/maxHeight)));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            int reverse = show ? 0 : -1;
            float lastHeight;
            float lastFraction;
            float a;
            float v;
            float c;
            float c0;
            float t;
            float t0;

            float lastVal;

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if(reverse == 0) {
                    if(rLcomments.getMeasuredHeight()-lVcomments.getMeasuredHeight()>5) {
                        c0 = lVcomments.getMeasuredHeight();
                        c = rLcomments.getMeasuredHeight();
                        t0 = valueAnimator.getAnimatedFraction();
                        v = (c-c0)/(t0-lastFraction);
                        t = 1-t0;
                        a = -(v*t+c)/(t*t);
                        reverse = 1;
                        Log.e("reverse", "c: "+c+" t0: "+t0+" v: "+v+" t: "+t+" a: "+a);
                    } else {
                        lastHeight = rLcomments.getMeasuredHeight();
                        lastFraction = valueAnimator.getAnimatedFraction();
                    }
                }

                // TODO: divide setting params.height to response more reflexively
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) rLcomments.getLayoutParams();
                float val = (float) valueAnimator.getAnimatedValue();
                if(reverse == 1) {
                    float x = valueAnimator.getAnimatedFraction()-t0;
                    params.height = (int)(a*x*x+v*x+c+c0);
                    Log.e("x", x+"");
                } else {
                    params.height = (int)(val);
                }
                rLcomments.setLayoutParams(params);
                sVpost.smoothScrollBy(0, (int)(val-lastVal));

                Log.e("onAnimationUpdate", "height: "+rLcomments.getMeasuredHeight()+"/"+lVcomments.getMeasuredHeight());
            }
        });
        anim.start();
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