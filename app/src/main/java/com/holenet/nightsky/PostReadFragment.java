package com.holenet.nightsky;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

    TextView tVtitle, tVauthor, tVdate, tVtime, tVtext;
    ListView lVcomments;
    CommentsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();

        View v = inflater.inflate(R.layout.fragment_post_read, container, false);

        tVtitle = (TextView) v.findViewById(R.id.tVtitle);
        tVauthor = (TextView) v.findViewById(R.id.tVauthor);
        tVdate = (TextView) v.findViewById(R.id.tVdate);
        tVtime = (TextView) v.findViewById(R.id.tVtime);
        tVtext = (TextView) v.findViewById(R.id.tVtext);

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