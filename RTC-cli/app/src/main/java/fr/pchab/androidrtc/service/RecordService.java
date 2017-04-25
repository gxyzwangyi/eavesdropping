package fr.pchab.androidrtc.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import fr.pchab.androidrtc.Utils;

import static android.drm.DrmStore.Playback.START;
import static android.drm.DrmStore.Playback.STOP;


public class RecordService extends Service {
  private MediaProjection mediaProjection;
  private MediaRecorder mediaRecorder;
  private VirtualDisplay virtualDisplay;

  private boolean running;
  private int width = 720;
  private int height = 1080;
  private int dpi;
  Handler handler;
  public String input=new String();
  public String output=new String();

  @Override
  public IBinder onBind(Intent intent) {
    return new RecordBinder();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;
  }

  @Override
  public void onCreate() {

    super.onCreate();
    HandlerThread serviceThread = new HandlerThread("service_thread",
        android.os.Process.THREAD_PRIORITY_BACKGROUND);
    serviceThread.start();
    handler = new Handler();

    running = false;
    mediaRecorder = new MediaRecorder();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  public void setMediaProject(MediaProjection project) {
    mediaProjection = project;
  }

  public boolean isRunning() {
    return running;
  }

  public void setConfig(int width, int height, int dpi) {
    this.width = width;
    this.height = height;
    this.dpi = dpi;
  }

  public boolean startRecord() {
    if (mediaProjection == null || running) {
      return false;
    }

    initRecorder();
    createVirtualDisplay();
    mediaRecorder.start();
    running = true;
    return true;
  }

  public boolean stopRecord() {
    if (!running) {
      return false;
    }
    running = false;
    mediaRecorder.stop();
    mediaRecorder.reset();
    virtualDisplay.release();
    mediaProjection.stop();

    LoadingThread loadingThread = new LoadingThread();
    //开启线程
    loadingThread.start();





    return true;
  }



  Handler mHandler = new Handler() {
    public void handleMessage(Message msg) {
      if (msg.what == START) {
            /*sendMessage方法更新UI的操作必须在handler的handleMessage回调中完成*/
      Log.e("handler","加密好了");
      Toast.makeText(getApplication(),"加密完成，文件在"+output,Toast.LENGTH_LONG).show();
      }

      else  {
            /*sendMessage方法更新UI的操作必须在handler的handleMessage回调中完成*/
        Log.e("handler","失败");
        Toast.makeText(getApplication(),"错误"+msg.obj,Toast.LENGTH_LONG).show();
      }



    }
  };


  private class LoadingThread extends Thread{
    @Override
    public void run() {
      //执行耗时操作
      try {
        Utils.encrypt(input,output);

        Message msg = new Message();

        msg.what = START;

        mHandler.sendMessage(msg);


      } catch (Exception e) {
        e.printStackTrace();



        Message msg = new Message();

        msg.what = STOP;
        msg.obj = e.getMessage();

        mHandler.sendMessage(msg);


      }

    }
  }



  private void createVirtualDisplay() {
    virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
  }

  private void initRecorder() {
    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//    mediaRecorder.setOutputFile(getsaveDirectory() +System.currentTimeMillis() + ".mp4");

    input=getsaveDirectory() +System.currentTimeMillis() + ".mp4";
    output=input+"d";


    mediaRecorder.setOutputFile(input);

    Log.e("","A"+System.currentTimeMillis() + ".mp4");
    mediaRecorder.setVideoSize(width, height);
    mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
    mediaRecorder.setVideoFrameRate(30);
    try {
      mediaRecorder.prepare();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getsaveDirectory() {
    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
      String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScreenRecord" + "/";

      File file = new File(rootDir);
      if (!file.exists()) {
        if (!file.mkdirs()) {
          return null;


        }
      }

      Toast.makeText(getApplicationContext(), rootDir, Toast.LENGTH_SHORT).show();

      return rootDir;
    } else {
      return null;
    }
  }

  public class RecordBinder extends Binder {
    public RecordService getRecordService() {
      return RecordService.this;
    }
  }
}