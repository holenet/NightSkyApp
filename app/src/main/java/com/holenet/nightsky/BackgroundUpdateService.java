package com.holenet.nightsky;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class BackgroundUpdateService extends Service {
    public BackgroundUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    boolean quit;
    long delay = 5000;

    @Override
    public void onCreate() {
        super.onCreate();
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

        UpdateThread thread = new UpdateThread(this, hander);
        thread.start();
        return START_STICKY;
    }

    private class UpdateThread extends Thread {
        BackgroundUpdateService parent;
        Handler handler;

        public UpdateThread(BackgroundUpdateService parent, Handler handler) {
            this.parent = parent;
            this.handler = handler;
        }

        @Override
        public void run() {
            while(!quit) {
                Message msg = new Message();

                try {
                    Thread.sleep(delay);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    Handler hander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==0) {

            }
        }
    };
}
