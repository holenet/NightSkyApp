package com.holenet.nightsky.music;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.holenet.nightsky.DatabaseHelper;
import com.holenet.nightsky.R;
import com.holenet.nightsky.main.UserActivity;
import com.holenet.nightsky.item.Music;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    interface CallbackInterface {
        void onMusicChanged(Music music);
    }
    MusicActivity activity;

    class MusicBinder extends Binder {
        MusicService bind(MusicActivity activity) {
            MusicService.this.activity = activity;
            return MusicService.this;
        }
    }
    private final IBinder binder = new MusicBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private MediaPlayer playerPrev;
    private MediaPlayer playerCurr;
    private MediaPlayer playerNext;
    private List<Music> musicList = new ArrayList<>();
    int current;

    NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        /*musicUris = new ArrayList<>();
        File dir = new File(Environment.getExternalStorageDirectory()+File.separator+"NightSky"+File.separator+"Musics");
        if(dir.exists()) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            for(String path : dir.list()) {
                Log.e("path__", path);
                try {
                    retriever.setDataSource(dir.getAbsolutePath()+File.separator+path);
                    musicUris.add(Uri.fromFile(new File(dir.getAbsolutePath()+File.separator+path)));
                    Log.e("path", path);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }*/

        reset();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        start();

        return START_STICKY;
    }

    public void reset() {
        boolean isPlay;
        if(playerCurr==null)
            isPlay = true;
        else {
            isPlay = playerCurr.isPlaying();
            playerCurr.stop();
            playerCurr.release();
            if(playerPrev!=null)
                playerPrev.release();
            if(playerNext!=null)
                playerNext.release();
        }
        initializePlayList();
        initializePlayers(0);

        if(isPlay)
            start();
        else if(activity!=null)
            activity.onMusicChanged(null);
    }

    @Override
    public void onDestroy() {
        playerCurr.stop();
        playerCurr.release();
    }

    private MediaPlayer.OnCompletionListener nextListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            movePlayers(true);
        }
    };

    private void initializePlayList() {
        SharedPreferences pref = getSharedPreferences("settings_music", 0);
        int listId = pref.getInt(getString(R.string.pref_key_curr_list_id), -1);
        if(listId==-1)
            return;
        musicList = new ArrayList<>();
        SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
        Cursor c = db.rawQuery("select music_id from "+DatabaseHelper.musicLinkTable+" where list_id = "+listId, null);
        for(int i=0; i<c.getCount(); i++) {
            c.moveToNext();
            int deviceId = c.getInt(0);

            Cursor cc = db.rawQuery("select server_id, title, artist, album, length, path from "+DatabaseHelper.musicTable+" where device_id = "+deviceId, null);
            cc.moveToNext();
            int serverId = cc.getInt(0);
            String title = cc.getString(1);
            String artist = cc.getString(2);
            String album = cc.getString(3);
            int length = cc.getInt(4);
            String path = cc.getString(5);

            Music music = new Music(deviceId, serverId, title, artist, album, length, path);
            musicList.add(music);

            cc.close();
        }
        c.close();
    }

    public void initializePlayersById(int musicId) {
        int index = -1;
        for(int i=0; i<musicList.size(); i++) {
            if(musicList.get(i).getDeviceId()==musicId) {
                index = i;
            }
        }
        if(index!=-1) {
            initializePlayers(index);
        }
    }

    private void initializePlayers(int current) {
        this.current = current;
        int num = musicList.size();
        if(playerCurr!=null)
            playerCurr.release();
        if(playerPrev!=null)
            playerPrev.release();
        if(playerNext!=null)
            playerNext.release();
        if(num==0) {
            playerCurr = playerPrev = playerCurr = null;
        } else {
            playerCurr = MediaPlayer.create(this, musicList.get(current).getUri());
            playerCurr.setOnCompletionListener(nextListener);
            if(num==1) {
                playerCurr.setLooping(true);
            } else {
                if(num==2) {
                    playerPrev = playerNext = MediaPlayer.create(this, musicList.get((current+1)%num).getUri());
                } else {
                    playerPrev = MediaPlayer.create(this, musicList.get((current-1+num)%num).getUri());
                    playerNext = MediaPlayer.create(this, musicList.get((current+1)%num).getUri());
                }
                playerCurr.setNextMediaPlayer(playerNext);
            }
        }
    }

    private void movePlayers(boolean next) {
        int num = musicList.size();
        current = (current+(next?1:-1)+num)%num;
        if(num==1) {
            playerCurr.seekTo(0);
        } else if(num>=2) {
            playerCurr.stop();
            playerCurr.release();
            if(next) {
                if(playerPrev!=playerNext)
                    playerPrev.release();
                playerCurr = playerNext;
            } else {
                if(playerPrev!=playerNext)
                    playerNext.release();
                playerCurr = playerPrev;
            }
            playerPrev = MediaPlayer.create(this, musicList.get((current-1+num)%num).getUri());
            playerNext = MediaPlayer.create(this, musicList.get((current+1)%num).getUri());
            playerCurr.setNextMediaPlayer(playerNext);
            playerCurr.setOnCompletionListener(nextListener);
            playerCurr.start();
        }
        if(num>=1) {
            activity.onMusicChanged(musicList.get(current));
        } else {
            activity.onMusicChanged(null);
        }
    }

    public Music getCurrentMusic() {
        if(playerCurr==null)
            return null;
        if(current<0 || musicList.size()<=current)
            return null;
        return musicList.get(current);
    }

    public boolean start() {
        if(playerCurr==null) {
            if(activity!=null)
                activity.onMusicChanged(null);
            return false;
        }
        playerCurr.start();
        if(activity!=null)
            activity.onMusicChanged(musicList.get(current));

        Intent intent = new Intent(this, UserActivity.class);
        intent.putExtra("music_num", current);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent content = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder notiBuilder;
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O) {
            notiBuilder = new Notification.Builder(this, "Music");
        } else {
            notiBuilder = new Notification.Builder(this);
        }
        notiBuilder.setTicker("Music")
                .setContentTitle("Music Player")
                .setContentText("Playing...")
                .setSmallIcon(android.R.drawable.presence_audio_online)
                .setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(content);
        Notification noti = notiBuilder.build();
        notificationManager.notify(2, noti);

        return playerCurr.isPlaying();
    }

    public boolean playOrPause() {
        if(playerCurr==null)
            return false;
        if(playerCurr.isPlaying())
            playerCurr.pause();
        else
            playerCurr.start();
        return playerCurr.isPlaying();
    }

    public boolean prev() {
        if(playerCurr==null)
            return false;
        Log.e("curr="+playerCurr.getCurrentPosition(), "dur="+playerCurr.getDuration());
        if(playerCurr.getCurrentPosition()<3000 || playerPrev==null) {
            movePlayers(false);
        } else {
            playerCurr.seekTo(0);
        }
        return playerCurr.isPlaying();
    }

    public boolean next() {
        if(playerCurr==null)
            return false;
        movePlayers(true);
        return playerCurr.isPlaying();
    }

    public void seekTo(int msec) {
        if(playerCurr==null)
            return;
        playerCurr.seekTo(msec);
    }

    public boolean stop() {
        if(playerCurr==null)
            return false;
        playerCurr.stop();
        return playerCurr.isPlaying();
    }

    public int currentSec() {
        if(playerCurr==null)
            return 0;
        return playerCurr.getCurrentPosition();
    }
}
