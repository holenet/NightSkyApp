package com.holenet.nightsky.post;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.holenet.nightsky.item.Post;

public abstract class PostBaseFragment  extends Fragment{
    Post post;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        post = getArguments().getParcelable("Post");
    }
}
