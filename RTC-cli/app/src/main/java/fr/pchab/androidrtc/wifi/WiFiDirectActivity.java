/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.pchab.androidrtc.wifi;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.pchab.androidrtc.R;
import fr.pchab.androidrtc.ServerlessRTCClient;
import fr.pchab.androidrtc.dao.AnswerEvent;
import fr.pchab.androidrtc.dao.DialogEvent;
import fr.pchab.androidrtc.dao.MakeEvent;
import fr.pchab.androidrtc.dao.StartEvent;
import fr.pchab.androidrtc.dao.StopEvent;
import fr.pchab.androidrtc.dao.ToastEvent;
import fr.pchab.androidrtc.dao.TurnEvent;
import fr.pchab.androidrtc.service.RecordService;
import fr.pchab.webrtcclient.PeerConnectionParameters;
import rx.Subscriber;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends AppCompatActivity implements ChannelListener, DeviceListFragment.DeviceActionListener,ServerlessRTCClient.RtcListener {




    public static final String TAG = "wifidirectdemo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private WifiP2pDevice device;
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;

    private WifiP2pInfo info;
    ProgressDialog m_pDialog;

    private static final String VIDEO_CODEC_VP8 = "VP8";

    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final int RECORD_REQUEST_CODE  = 101;
    private static final int STORAGE_REQUEST_CODE = 102;
    private static final int AUDIO_REQUEST_CODE   = 103;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private RecordService recordService;

    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.

    public Subscriber<String> mySubscriber;
    int  m_count;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private ConnectionQuality mConnectionClass = ConnectionQuality.UNKNOWN;
    private int mTries = 0;
    private String mURL = "https://ss0.bdstatic.com/5aV1bjqh_Q23odCf/static/superman/img/logo/bd_logo1_31bdc765.png";
    private Button connect;
     public ServerlessRTCClient p2p_client;
    public String flag="1";
    private ConnectionClassManager mConnectionClassManager;
    private DeviceBandwidthSampler mDeviceBandwidthSampler;

//    private Surface mRecorderSurface;
//    private TextureView mPreview;
//    private MediaRecorder mMediaRecorder;
//    private File mOutputFile;
//    private MyRecorder mRecorder = null;
//
//    private boolean isRecording = false;
//



    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        EventBus.getDefault().register(this);
        Permission();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);


         mConnectionClassManager = ConnectionClassManager.getInstance();
         mDeviceBandwidthSampler = DeviceBandwidthSampler.getInstance();

        checkWifi();


        ConnectionQuality cq = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();

        Toast.makeText(this,"Network:"+cq,Toast.LENGTH_SHORT).show();

        final DeviceDetailFragment Dfragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        Dfragment.resetViews();



        setIsWifiP2pEnabled(true);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);


        if (!isWifiP2pEnabled) {
            Log.e("aaa", "off wifi");

        }

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
//                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                        | LayoutParams.FLAG_TURN_SCREEN_ON
        );




        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                Log.e("run1", "");
                init();
            }
        });









        init();

        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);


//        prepareVideoRecorder();



        find_peer();


    }


    private void checkWifi(){

                OkHttpClient client = new OkHttpClient();
                Request.Builder builder = new Request.Builder();
                Request request = builder.url("http://www.baidu.com").build();


                DeviceBandwidthSampler.getInstance().startSampling();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        DeviceBandwidthSampler.getInstance().stopSampling();
                        Log.e("TAG","onFailure:"+e);
                        WiFiDirectActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(WiFiDirectActivity.this, "网络环境太差或无法连接外网无法测速", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        DeviceBandwidthSampler.getInstance().stopSampling();
                        Log.e("TAG","onResponse:"+response);
                        final ConnectionQuality connectionQuality = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();
                        final double downloadKBitsPerSecond = ConnectionClassManager.getInstance().getDownloadKBitsPerSecond();
                        Log.e("TAG","网络状态:"+connectionQuality+" 下载速度:"+downloadKBitsPerSecond+" kb/s");

                       final String s ="网络状态:"+connectionQuality+"\n"+"下载速度:"+downloadKBitsPerSecond+" kb/s";
//                        Toast.makeText(WiFiDirectActivity.this,s,Toast.LENGTH_LONG).show();

                        WiFiDirectActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(WiFiDirectActivity.this, s, Toast.LENGTH_SHORT).show();
                            }
                        });


                    }
                });


    }



    private void intConnect(){

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(WiFiDirectActivity.this);
        alertDialog.setTitle("请填写对方设备的ip地址");
        alertDialog.setMessage("输入ip");


        final EditText input = new EditText(WiFiDirectActivity.this);


        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input); // uncomment this line

        SharedPreferences sharedPreferences = getSharedPreferences("video", Context.MODE_PRIVATE);
        String ip = sharedPreferences.getString("ip", "");
        input.setText(ip);


        alertDialog.setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (manager != null && channel != null) {

                            Log.e("makeoffer", "alertDialog makeoffer");

                            SendsdpService(input.getText().toString());



                            SharedPreferences sharedPreferences = getSharedPreferences("video", Context.MODE_PRIVATE);

                            SharedPreferences.Editor editor = sharedPreferences.edit();

                            editor.putString("ip", input.getText().toString());

                            editor.apply();

                            progressdialog();

                        } else {
                            Log.e(TAG, "channel or manager is null");
                        }

                    }
                });

        alertDialog.setNegativeButton("NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();



    }






    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);

        Log.e("run", "");
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, 1280, 720, 30, 1, VIDEO_CODEC_VP9, false, 1, AUDIO_CODEC_OPUS, true);


        p2p_client = new ServerlessRTCClient(this, params, VideoRendererGui.getEGLContext());

        p2p_client.init();

    }


    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {
        Log.e("add!!!", remoteStream.label());
        Log.e("add!!!", remoteStream.videoTracks.toString());
        Log.e("add!!!", remoteStream.toString());









//        WebRtcAudioRecord webRtcAudioRecord=new WebRtcAudioRecord();
//        webRtcAudioRecord.InitRecording();


        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType);

    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {

    }




    @Override
    public void onBackPressed() {

        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }

    public void onStateChanged(ServerlessRTCClient.State state) {

    }


    public void find_peer() {


        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
                Log.d("size", peers.size() + "");


                manager.requestPeers(channel, peerListListener);


            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            Log.d("aaaa", peerList.getDeviceList().toString());

            peers.clear();
            peers.addAll(peerList.getDeviceList());

            if (peers.size() == 0) {
                Log.e("peer", "No devices found");
            } else {

                manager.requestConnectionInfo(channel, connectionInfoListener);


            }

        }
    };


    private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(final WifiP2pInfo info1) {
            info = info1;
            Log.d("bbbb", info1.toString());
            WifiP2pDevice device = peers.get(0);
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
//            connect(config);
        }

    };


    /**
     * register the BroadcastReceiver with the intent values to be matched
     */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
        mConnectionClassManager.register(mListener);


    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        mConnectionClassManager.remove(mListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disconnect();
        EventBus.getDefault().post(new TurnEvent("end"));

        unbindService(connection);
        EventBus.getDefault().unregister(this);


    }


    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);

        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {

                    Log.e("makeoffer", "webrtc makeoffer");
                    SendsdpService("192.168.49.1");
                    progressdialog();

                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

            case R.id.atn_direct_discover:
                if (!isWifiP2pEnabled) {
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            case R.id.atn_wifi_check:
                checkWifi();
                return true;
            case R.id.atn_socket:
                intConnect();

                EventBus.getDefault().post(new DialogEvent("camera1"));

                return true;
            case R.id.video_quality:

                videoqualityDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectActivity.this, "Connect success",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry." + reason,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        try {
            fragment.resetViews();
        }
        catch (Exception e)
        {
            Log.e("xxxx",e.getMessage());
        }
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                try {
                    fragment.getView().setVisibility(View.GONE);
                }
                catch (Exception e)
                {
                    Log.e("xxxx",e.getMessage());
                }

            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {

        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void makeEventBus(MakeEvent makeEvent) {
        String message = flag+makeEvent.name;
        String ip = makeEvent.ip;

        Log.e("make", message.toString());

        Intent serviceIntent = new Intent(WiFiDirectActivity.this, NewService.class);
        serviceIntent.putExtra(NewService.EXTRAS,
                ip);
        serviceIntent.putExtra("p2p", message);
        WiFiDirectActivity.this.startService(serviceIntent);

    }







    public void SendsdpService(String ip) {
        Log.e("xxxx","SendsdpService"+ip);
        p2p_client.initmakeoffer();

        p2p_client.makeOffer(ip);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void answerEventBus(AnswerEvent answerEvent) {
        String message = answerEvent.name;
        Log.e("answer", message.toString());
        p2p_client.processAnswer(message);

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void startEventBus(StartEvent startEvent) {
        String message = startEvent.name;
        Log.e("start", message.toString());
        p2p_client.sendMessage(message);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void stopEventBus(StopEvent stopEvent) {
        String message = stopEvent.name;
        Log.e("stop", message.toString());
        p2p_client.sendMessage(message);

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void turnEventBus(TurnEvent turnEvent) {
        String message = turnEvent.name;
        Log.e("turn", message.toString());
        if (message == "end")
        {
            unrecorded();

        }

        p2p_client.sendMessage(message);

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void dialogEventBus(DialogEvent dialogEvent) {
        String message = dialogEvent.name;
        Log.e("dialog", message.toString());
        chooseCamera();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void ToastEventBus(ToastEvent toastEvent) {
        String message = toastEvent.name;

        Toast.makeText(getApplicationContext(), message,Toast.LENGTH_LONG).show();




    }





    private void  videoqualityDialog() {

        final String[] single_list = {"720p", "480p"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择视频录制质量");
        builder.setIcon(R.drawable.ic_videocam_white_48dp);


        SharedPreferences sharedPreferences = getSharedPreferences("video", Context.MODE_PRIVATE);
        int quality = sharedPreferences.getInt("quality", 0);
        Log.e("ppppp0",quality+"");

        builder.setSingleChoiceItems(single_list, quality, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                SharedPreferences sharedPreferences = getSharedPreferences("video", Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putInt("quality", which);

                editor.apply();

                Log.e("ppppp1",which+"");

                String str = single_list[which];
                Toast.makeText(WiFiDirectActivity.this, "你选择了"+str , Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }




    private void progressdialog() {

       // TODO Auto-generated method stub

          m_count = 0;

       // 创建ProgressDialog对象
       m_pDialog = new ProgressDialog( this);

       // 设置进度条风格，风格为长形
       m_pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

       // 设置ProgressDialog 标题
       m_pDialog.setTitle("提示");

       // 设置ProgressDialog 提示信息
       m_pDialog.setMessage("webrtc交换中");


       // 设置ProgressDialog 进度条进度
       m_pDialog.setProgress(100);

       // 设置ProgressDialog 的进度条是否不明确
       m_pDialog.setIndeterminate(false);

       // 设置ProgressDialog 是否可以按退回按键取消
       m_pDialog.setCancelable(true);

       // 让ProgressDialog显示
       m_pDialog.show();

       new Thread()
       {
           public void run()
           {
               try
               {
                   while (m_count <= 100)
                   {
                       // 由线程来控制进度。
                       m_pDialog.setProgress(m_count++);
                       Thread.sleep(240);
                   }
                   m_pDialog.cancel();
                   recorded();
               }
               catch (InterruptedException e)
               {
                   m_pDialog.cancel();
               }
           }
       }.start();

   }

    public void chooseCamera() {

        Dialog dialog = new AlertDialog.Builder(this).setIcon(
                android.R.drawable.btn_star).setTitle("摄像头").setMessage(
               "选择摄像头方向？").setPositiveButton("正面",
               new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    flag="1";

                }
               }).setNegativeButton("反面", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                  flag="0";
              }
             }).setNeutralButton("取消", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                  dialog.dismiss();

              }
             }).create();
             dialog.show();
            dialog.setCanceledOnTouchOutside(false);

    }

    ConnectionChangedListener mListener = new ConnectionChangedListener();

    private class ConnectionChangedListener implements
            ConnectionClassManager.ConnectionClassStateChangeListener {
        @Override
        public void onBandwidthStateChange(ConnectionQuality bandwidthState) {
            Log.e("onBandwidthStateChange", bandwidthState.toString());

            final ConnectionQuality connectionQuality = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();
            final double downloadKBitsPerSecond = ConnectionClassManager.getInstance().getDownloadKBitsPerSecond();

            Log.e("TAG","网络状态:"+connectionQuality+" 下载速度:"+downloadKBitsPerSecond+" kb/s");

            final String s ="网络状态:"+connectionQuality+"\n"+"下载速度:"+downloadKBitsPerSecond+" kb/s";
            WiFiDirectActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(WiFiDirectActivity.this, s, Toast.LENGTH_SHORT).show();
                }
            });

        }
    }















public void Permission(){


    if (ContextCompat.checkSelfPermission(WiFiDirectActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
    }

    if (ContextCompat.checkSelfPermission(WiFiDirectActivity.this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
    }

    Intent intent = new Intent(this, RecordService.class);
    bindService(intent, connection, BIND_AUTO_CREATE);

}







    public void recorded() {

        if (!recordService.isRunning()) {

            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
        }

    }


    public void unrecorded() {

        if (recordService.isRunning()) {
            recordService.stopRecord();


        }

    }













    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            recordService.setMediaProject(mediaProjection);
            recordService.startRecord();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST_CODE || requestCode == AUDIO_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
    }




    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
            recordService = binder.getRecordService();

            SharedPreferences sharedPreferences = getSharedPreferences("video", Context.MODE_PRIVATE);



            int quality = sharedPreferences.getInt("quality", 0);
            Log.e("ppppp",quality+"");

            if (quality==0){
                recordService.setConfig(720, 1280, metrics.densityDpi);

            }
            else {
                recordService.setConfig(480, 800, metrics.densityDpi);

            }


        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };






//
//    public void recorded(){
//
//            // BEGIN_INCLUDE(prepare_start_media_recorder)
//
//            new MediaPrepareTask().execute(null, null, null);
//
//            // END_INCLUDE(prepare_start_media_recorder)
//    }
//    public void unrecord(){
//
//        // stop recording and release camera
//        try {
//            Log.e("record","stop");
//            mMediaRecorder.stop();  // stop the recording
//        } catch (RuntimeException e) {
//            // RuntimeException is thrown when stop() is called immediately after start().
//            // In this case the output file is not properly constructed ans should be deleted.
//            Log.e("record", "RuntimeException: stop() is called immediately after start()");
//            //noinspection ResultOfMethodCallIgnored
//
//            mOutputFile.delete();
//
//        }
//        releaseMediaRecorder(); // release the MediaRecorder object
////            mCamera.lock();         // take camera access back from MediaRecorder
//
//        // inform the user that recording has stopped
//        isRecording = false;
////            releaseCamera();
//        // END_INCLUDE(stop_release_media_recorder)
//
//
//
//    }
//
//
//
//
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    private boolean prepareVideoRecorder(){
//
//        mMediaRecorder = new MediaRecorder();
//
//
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
//
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//
//        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
//        Log.e("地址",mOutputFile.getPath().toString());
//        if (mOutputFile == null) {
//            return false;
//
//
//        }
//        mMediaRecorder.setOutputFile(mOutputFile.getPath());
//        mMediaRecorder.setVideoSize(480, 800);
//        mMediaRecorder.setVideoFrameRate(20);
//        mMediaRecorder.setVideoEncodingBitRate(10000000);
//
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//
//
//
//        try {
//            mMediaRecorder.prepare();
//        } catch (IllegalStateException e) {
//            Log.e("record", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
//            releaseMediaRecorder();
//            return false;
//        } catch (IOException e) {
//            Log.e("record", "IOException preparing MediaRecorder: " + e.getMessage());
//            releaseMediaRecorder();
//            return false;
//        }
//        Log.e("record","prepare");
//        return true;
//    }
//
//
//
//    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {
//
//        @Override
//        protected Boolean doInBackground(Void... voids) {
//            // initialize video camera
//            if (prepareVideoRecorder()) {
//                // Camera is available and unlocked, MediaRecorder is prepared,
//                // now you can start recording
//                Log.e("start","true");
//
//
////                mRecorderSurface = mMediaRecorder.getSurface();
////                Log.e("sss1",mRecorderSurface.isValid() +"");
//
//                mMediaRecorder.start();
//
//                isRecording = true;
//            } else {
//                // prepare didn't work, release the camera
//                releaseMediaRecorder();
//                return false;
//            }
//            return true;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean result) {
//            Log.e("record","录制结束");
//        }
//    }
//
//
//
//
//    private void releaseMediaRecorder(){
//        if (mMediaRecorder != null) {
//            // clear recorder configuration
//            mMediaRecorder.reset();
//            // release the recorder object
//            mMediaRecorder.release();
//            mMediaRecorder = null;
//            // Lock camera for later use i.e taking it back from MediaRecorder.
//            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
////            mCamera.lock();
//        }
//    }



















}
