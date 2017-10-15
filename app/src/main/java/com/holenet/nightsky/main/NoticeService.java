package com.holenet.nightsky.main;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.holenet.nightsky.NetworkManager;
import com.holenet.nightsky.Parser;
import com.holenet.nightsky.item.Post;

public class NoticeService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    final static boolean DEBUG = true;

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    boolean quit;
    NotificationManager notificationManager;
    long delay = 5000;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        pref = getSharedPreferences("post", 0);
        editor = pref.edit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        quit = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        quit = false;

        NoticeThread thread = new NoticeThread(this, handler);
        thread.start();
        return START_STICKY;
    }

    class NoticeThread extends Thread {
        NoticeService parent;
        Handler handler;

        public NoticeThread(NoticeService parent, Handler handler) {
            this.parent = parent;
            this.handler = handler;
        }

        @Override
        public void run() {
            while(!quit) {
                if(!DEBUG) {
                    String output = NetworkManager.get(NoticeService.this, NetworkManager.CLOUD_DOMAIN+"post/recent/");
                    Post post = Parser.getRecentPostSimpleJSON(output);
                    if(post.getId()!=pref.getInt("recent_id", -1) && !post.getAuthor().equals(getSharedPreferences("settings_login", 0).getString("username", ""))) {

                        Message msg = new Message();
                        msg.what = 0;
                        msg.obj = post;
                        handler.sendMessage(msg);
                    }

                    try {
                        Thread.sleep(delay);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Message msg = new Message();
                    msg.what = 0;
                    Post post = new Post();
                    post.setId(3);
                    post.setAuthor("abcdef");
                    post.setTitle("titletitletitle");
                    msg.obj = post;
                    handler.sendMessage(msg);

                    try {
                        Thread.sleep(30000);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==0) {
                Intent intent = new Intent(NoticeService.this, UserActivity.class);
                intent.putExtra("recent_id", msg.arg1);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent content = PendingIntent.getActivity(NoticeService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Post post = (Post) msg.obj;
                String author = post.getAuthor();
                String title = post.getTitle();

                Notification.Builder notiBuilder;
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    notiBuilder = new Notification.Builder(NoticeService.this, "Notice");
                } else {
                    notiBuilder = new Notification.Builder(NoticeService.this);
                }
                notiBuilder.setTicker("Notice")
                        .setContentTitle("New Post")
                        .setContentText((title.length()>20 ? title.substring(0, 20)+"..." : title)+"/"+author)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setAutoCancel(true)
                        .setContentIntent(content);

                Notification noti = notiBuilder.build();
                notificationManager.notify(1, noti);
                editor.putInt("recent_id", post.getId());
            }
        }
    };
}
