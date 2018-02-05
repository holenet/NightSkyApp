package com.holenet.nightsky.secret;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.holenet.nightsky.DatabaseHelper;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.Piece;

public class PieceActivity extends AppCompatActivity {
    Piece piece;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piece);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        piece = DatabaseHelper.getPiece(PieceActivity.this, getIntent().getIntExtra("piece_pk", 0));
        DatabaseHelper.updateWatchList(PieceActivity.this, piece);

        refresh();
    }

    private void refresh() {
        getSupportActionBar().setTitle(piece.getTitle());
    }
}
