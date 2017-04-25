package fr.pchab.androidrtc;

import android.app.Application;
import android.util.Log;

/**
 * Created by wangyi on 16/11/30.
 */

public class App extends Application {


    public  int peersize=0;


    public int setsize(int ps){
        peersize=ps;
        Log.e("set",peersize+"");
        return peersize;
    }


    public int getsize(){
        Log.e("get",peersize+"");

        return peersize;
    }

}
