package fr.pchab.androidrtc.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;

import fr.pchab.androidrtc.App;
import fr.pchab.androidrtc.dao.PeerEvent;

/**
 * Created by wangyi on 17/3/6.
 */

public class AlarmReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
//        Intent i = new Intent(context, WifiService.class);
//        context.startService(i);

        Log.d("AlarmReceiver", "executed at " + new Date().toString());


        App app = new App();

        while(app.getsize()==0){
            Log.e("AlarmReceiver", "kkkk");

                    EventBus.getDefault().post(new PeerEvent("AlarmReceiver p2p search"));

            try {
                Thread.sleep(5000);
            }
            catch (Exception e){

            }

        }



    }
}

