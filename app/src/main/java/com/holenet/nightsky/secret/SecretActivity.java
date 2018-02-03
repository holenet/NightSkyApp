package com.holenet.nightsky.secret;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.nightsky.DatabaseHelper;
import com.holenet.nightsky.NetworkManager;
import com.holenet.nightsky.Parser;
import com.holenet.nightsky.R;
import com.holenet.nightsky.item.Piece;

import java.util.ArrayList;
import java.util.List;

public class SecretActivity extends AppCompatActivity {

    PieceUpdateTask pieceUpdateTask;

    FloatingActionButton fABlog;
    RecyclerView rVpieces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fABlog = (FloatingActionButton) findViewById(R.id.fABlog);
        fABlog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SecretActivity.this, LogActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        rVpieces = (RecyclerView) findViewById(R.id.rVlogs);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rVpieces.setHasFixedSize(true);
        rVpieces.setLayoutManager(layoutManager);

        List<Piece> pieces = new ArrayList<>();
        pieces.add(new Piece(19238, "abcd"));
        pieces.add(new Piece(19238, "abcd"));
        pieces.add(new Piece(19238, "abcd"));
        pieces.add(new Piece(19238, "abcd"));
        pieces.add(new Piece(19238, "abcd"));
        rVpieces.setAdapter(new RecyclerAdapter(getApplicationContext(), pieces, R.layout.activity_secret));

        refresh();
    }

    private void refresh() {
        if(pieceUpdateTask==null) {
            pieceUpdateTask = new PieceUpdateTask();
            pieceUpdateTask.execute((Void) null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        finish();
    }

    private class PieceUpdateTask extends AsyncTask<Void, Void, String> {
/*
        @Override
        protected void onPreExecute() {
            updateProgress();
        }
*/

        @Override
        protected String doInBackground(Void... voids) {
            return NetworkManager.get(SecretActivity.this, NetworkManager.SECRET_DOMAIN+"piece/list/");
        }

        @Override
        protected void onPostExecute(String result) {
            pieceUpdateTask = null;
//            updateProgress();

            List<Piece> pieces = Parser.getPiecesJSON(result);
            if(pieces==null) {
                return;
            }
            SQLiteDatabase db = new DatabaseHelper(SecretActivity.this).getWritableDatabase();
            db.delete(DatabaseHelper.pieceTable, null, null);
            for(Piece piece: pieces) {
                ContentValues values = new ContentValues();
                values.put("pk", piece.getPk());
                values.put("title", piece.getTitle());
                db.insert(DatabaseHelper.pieceTable, null, values);
            }
            db.close();
        }

        @Override
        protected void onCancelled() {
            pieceUpdateTask = null;
//            updateProgress();
        }
    }

    public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
        Context context;
        List<Piece> items;
        int item_layout;

        public RecyclerAdapter(Context context, List<Piece> items, int item_layout) {
            this.context = context;
            this.items = items;
            this.item_layout = item_layout;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_piece, null);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Piece piece = items.get(position);
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.side_nav_bar);
            holder.iVimage.setImageResource(R.drawable.side_nav_bar);
            holder.tVtitle.setText(piece.getTitle());
            holder.cVpiece.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, piece.getTitle(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return this.items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iVimage;
            TextView tVtitle;
            CardView cVpiece;

            public ViewHolder(View itemView) {
                super(itemView);
                iVimage = (ImageView) itemView.findViewById(R.id.iVimage);
                tVtitle = (TextView) itemView.findViewById(R.id.tVtitle);
                cVpiece = (CardView) itemView.findViewById(R.id.cVpiece);
            }
        }
    }
}
