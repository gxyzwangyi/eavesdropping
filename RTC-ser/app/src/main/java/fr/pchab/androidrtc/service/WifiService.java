package fr.pchab.androidrtc.service;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Point;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


import java.net.Socket;
import java.util.Date;

import fr.pchab.androidrtc.dao.OfferEvent;
import fr.pchab.androidrtc.dao.PeerEvent;
import fr.pchab.androidrtc.dao.TestEvent;


public class WifiService extends Service {


    @Override
    public void onCreate() {
        super.onCreate();
//        EventBus.getDefault().register(this);
//        EventBus.getDefault().post(new PeerEvent("discover"));

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread(new Runnable() {
            @Override
            public void run() {
//                EventBus.getDefault().post(new PeerEvent("discover"));

                Log.d("LongRunningService", "executed at " + new Date().toString());


            }
        }).start();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int tenmin = 10 * 1000; // 这是10秒的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + tenmin;
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void testEventBus(TestEvent testEvent){
        String message=testEvent.name;
        Log.e("test",message.toString());
     }



}

