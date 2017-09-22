package com.holenet.nightsky;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class PostEditFragment extends PostBaseFragment {
    PostActivity activity;

    public static PostEditFragment newInstance(Post post) {
        PostEditFragment fragment = new PostEditFragment();
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

    EditText eTtitle, eTtext;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (PostActivity) getActivity();

        View v = inflater.inflate(R.layout.fragment_post_edit, container, false);

        eTtitle = (EditText) v.findViewById(R.id.eTtitle);
        eTtext = (EditText) v.findViewById(R.id.eTtext);

        eTtitle.setText(post.getTitle());
        eTtext.setText(post.getText());

        return v;
    }

    void save() {
        post.setTitle(eTtitle.getText().toString());
        post.setText(eTtext.getText().toString());
    }

    void clear() {
        eTtitle.setText("");
        eTtext.setText("");
    }
}
