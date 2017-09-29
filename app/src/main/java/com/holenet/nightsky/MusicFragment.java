package com.holenet.nightsky;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MusicFragment extends Fragment {
    Context context;
    UserActivity activity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getContext();
        activity = (UserActivity) getActivity();

        View v = inflater.inflate(R.layout.fragment_music, container, false);

        return v;
    }
}
