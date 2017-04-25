package fr.pchab.androidrtc;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import fr.pchab.androidrtc.service.RecordService;

/**
 * Created by wangyi on 17/3/28.
 */

public class RecordApplication extends Application {

    private static RecordApplication application;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        application = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 启动 Marvel service
        startService(new Intent(this, RecordService.class));
    }

    public static RecordApplication getInstance() {
        return application;
    }
}
