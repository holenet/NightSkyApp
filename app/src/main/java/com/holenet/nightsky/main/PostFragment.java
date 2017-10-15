package com.holenet.nightsky.main;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.nightsky.NetworkManager;
import com.holenet.nightsky.Parser;
import com.holenet.nightsky.post.PostActivity;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.Post;

import java.util.ArrayList;
import java.util.List;

public class PostFragment extends Fragment {
    final static int REQUEST_POST_DETAIL = 1000;
    int count;

    Context context;
    UserActivity activity;

    PostListTask listTask;

    FloatingActionButton fABadd;

    ListView lVposts;
    PostsAdapter adapter;

    int recentId = -1;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        activity = (UserActivity) getActivity();

        View v = inflater.inflate(R.layout.fragment_post, container, false);

        fABadd = v.findViewById(R.id.fABadd);
        fABadd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDetailView(new Intent());
            }
        });

        lVposts = (ListView) v.findViewById(R.id.lVposts);
        adapter = new PostsAdapter(context, R.layout.item_post, new ArrayList<Post>());
        lVposts.setAdapter(adapter);
        lVposts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openDetailView(new Intent().putExtra("post_current_page", position));
            }
        });

        refresh();

        return v;
    }

    protected void openDetailView(Intent intent) {
        intent.setClass(context, PostActivity.class);
        intent.putExtra("post_count_all", count);
        startActivityForResult(intent, REQUEST_POST_DETAIL);
        activity.showProgress(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_POST_DETAIL) {
            if(resultCode==NetworkManager.RESULT_CODE_LOGIN_FAILED) {
                activity.requestLogin();
            } else {
                refresh();
            }
        }
    }

    public void refresh() {
        if(listTask!=null)
            return;
        activity.showProgress(true);
        listTask = new PostListTask();
        listTask.execute((Void) null);
    }

    public class PostListTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return NetworkManager.get(context, NetworkManager.CLOUD_DOMAIN+"?JSON");
        }

        @Override
        protected void onPostExecute(final String result) {
            listTask = null;
            activity.showProgress(false);

            if(result==null) {
                Toast.makeText(context, getString(R.string.error_network), Toast.LENGTH_LONG).show();
                return;
            }
            if(result.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED)) {
                activity.requestLogin();
            }

            List<Post> posts = Parser.getPostListJSON(result);

            adapter.setItems(posts);
            adapter.notifyDataSetChanged();

            count = posts.size();

            if(recentId!=-1) {
                int currentPage = -1;
                for(int i=0; i<posts.size(); i++) {
                    if(recentId==posts.get(i).getId()) {
                        currentPage = i;
                    }
                }
                if(currentPage!=-1) {
                    openDetailView(new Intent().putExtra("post_recent_id", recentId));
                }
                recentId = -1;
            }
        }

        @Override
        protected void onCancelled() {
            listTask = null;
            activity.showProgress(false);
        }
    }

    class PostsAdapter extends ArrayAdapter<Post> {
        private List<Post> items;

        PostsAdapter(Context context, int layout, List<Post> items) {
            super(context, layout, items);
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if(v==null) {
                v = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_post, null);
            }

            Post post = items.get(position);
            if(post!=null) {
                TextView tVtitle = v.findViewById(R.id.tVtitle);
                if(tVtitle!=null)
                    tVtitle.setText(post.getTitle());
                TextView tVtext = v.findViewById(R.id.tVtext);
                if(tVtext!=null)
                    tVtext.setText(post.getText());
                TextView tVauthor = v.findViewById(R.id.tVauthor);
                if(tVauthor!=null)
                    tVauthor.setText(post.getAuthor());
                TextView tVdate = v.findViewById(R.id.tVdate);
                if(tVdate!=null)
                    tVdate.setText(post.getDatetime()[0]);
                TextView tVcommentCount = v.findViewById(R.id.tVcommentCount);
                if(tVcommentCount!=null)
                    tVcommentCount.setText(String.valueOf(post.getCommentCount()));
            }

            return v;
        }

        public void setItems(List<Post> items) {
            this.items.clear();
            for(int i=0; i<items.size(); i++) {
                this.items.add(items.get(i));
            }
        }
    }
}
