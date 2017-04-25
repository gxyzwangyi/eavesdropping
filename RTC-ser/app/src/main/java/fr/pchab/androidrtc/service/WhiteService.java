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
import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import fr.pchab.androidrtc.App;
import fr.pchab.androidrtc.R;
import fr.pchab.androidrtc.RtcActivity;
import fr.pchab.androidrtc.ServerlessRTCClient;
import fr.pchab.androidrtc.dao.AnswerEvent;
import fr.pchab.androidrtc.dao.OfferEvent;

import fr.pchab.androidrtc.dao.PeerEvent;
import fr.pchab.androidrtc.dao.TestEvent;
import fr.pchab.androidrtc.wifi.WiFiDirectBroadcastReceiver;
import fr.pchab.webrtcclient.PeerConnectionParameters;
import fr.pchab.webrtcclient.WebRtcClient;

import static android.R.id.input;
import static android.R.id.message;

/**
 * 正常的系统前台进程，会在系统通知栏显示一个Notification通知图标
 *
 */
public class WhiteService extends Service implements ServerlessRTCClient.RtcListener,WifiP2pManager.ChannelListener, WifiP2pManager.PeerListListener{

    private final static String TAG = WhiteService.class.getSimpleName();

    private final static int FOREGROUND_ID = 1;

    public static final String ACTION_CLOSE_NOTICE = "cn.campusapp.action.closenotice";
    public static final int NOTICE_ID_TYPE_0 = R.string.app_name;
    public static final String NOTICE_ID_KEY = "NOTICE_ID";


    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP8 = "VP8";
    private static final String VIDEO_CODEC_VP9 = "VP9";

    private static final String AUDIO_CODEC_OPUS = "opus";




    // Local preview screen position before call is connected.
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;
    private ServerlessRTCClient p2p_client;

    public App app;
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;
    private AlarmReceiver alarmreceiver = null;
    WifiP2pManager.PeerListListener peerListListener;

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ServerSocket serverSocket;
    public int a=0;
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        app= new App();
        Log.i(TAG, "WhiteService->onCreate");
        EventBus.getDefault().register(this);

        initwifi();
        init();
        Log.e(TAG, peers.size()+"");




        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);

        alarmreceiver = new AlarmReceiver();

        registerReceiver(alarmreceiver, intentFilter);



//        while(peers.size()==0){
//            Log.e(TAG, "kkkk");
//
//            initwifi();
//            try {
//                Thread.sleep(5000);
//            }
//            catch (Exception e){
//
//            }
//
//        }
//
//





    }

    private void init() {

        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, 1280, 720, 30, 1, VIDEO_CODEC_VP9, false, 1, AUDIO_CODEC_OPUS, true);

        p2p_client = new ServerlessRTCClient(this,params,VideoRendererGui.getEGLContext());
        p2p_client.init();
//        new ServerAsyncTask(app).execute();

    }


    private void initwifi(){
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        setIsWifiP2pEnabled(true);
        Log.e("run","12");

        if (!isWifiP2pEnabled) {
            Log.e("aaa", "off wifi");
        }

        Log.e("run", peers.size()+"");


        discoverPeers();
//        startAlarmPush(app,10);

    }









    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void answerEventBus(AnswerEvent answerEvent){
        String message=answerEvent.name;
        Socket client=answerEvent.socket;

        Log.e("answer",message.toString());
        try {
            OutputStream os=client.getOutputStream();
            PrintWriter pw=new PrintWriter(os);

            pw.write(message);
            pw.flush();
            pw.close();
            os.close();
            client.close();
        }
        catch (Exception e){

        }

    }



    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void offerEventBus(OfferEvent offerEvent){
        String all=offerEvent.name;
        String message=all.substring(1);
        String flag=all.substring(0,1);
        Socket client=offerEvent.socket;
        Log.e("offer",message.toString());
        p2p_client.setCamera(flag);
        p2p_client.processOffer(message,client);
    }



    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void peerEventBus(PeerEvent peerEvent){
        String message=peerEvent.name;
        Log.e("peerEventBus",message.toString());
        discoverPeers();

    }



    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void testEventBus(TestEvent testEvent){
        String message=testEvent.name;

        if(message=="end"){
            Log.e("end","stopSelf");
            stopSelf();

        }

        Log.e("test",message.toString());

    }





    public void discoverPeers() {
        Log.e("discoverPeers","discoverPeers");
        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                app.setsize(peers.size());

            }

        };

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                new ServerAsyncTask(app).execute();

            }

            @Override
            public void onFailure(int arg0) {
            }
        });
        // discoverPeers是异步执行的，调用了之后会立刻返回，但是发现的过程一直在进行，
        // 直到发现了某个设备时就会通知你
    }








    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        Log.e("onPeersAvailable","3");

        peers.clear();
        peers.addAll(peerList.getDeviceList());
        app.setsize(peers.size());
        if (peers.size() == 0) {
            Log.d(TAG, "No devices found");
            return;
        }

    }



    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {

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




//
//    public static void startAlarmPush(Context context, long intervalMillis) {
//
//        AlarmManager am = (AlarmManager) context
//                .getSystemService(Service.ALARM_SERVICE);
//        Intent intent = new Intent(context, WifiService.class);
//        PendingIntent p_intent = PendingIntent.getService(context, 1, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        am.setRepeating(AlarmManager.RTC_WAKEUP, 0, intervalMillis, p_intent);
//    }
//
//
//
//    public static void stopAlarmPush(Context context) {
//        AlarmManager am = (AlarmManager) context
//                .getSystemService(Service.ALARM_SERVICE);
//        Intent intent = new Intent(context, WifiService.class);
//        PendingIntent p_intent = PendingIntent.getService(context, 1, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        am.cancel(p_intent);
//    }





    public  class ServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;


        ServerAsyncTask(Context context) {
            this.context = context;
//            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                String result="123";
//                ServerSocket serverSocket = new ServerSocket(8988);
//                serverSocket.setReuseAddress(true);
                if(serverSocket==null) {
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(8988));
                }

                Log.e(TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.e(TAG, "Server: connection done");


                // 从Socket当中得到InputStream对象
                InputStream inputStream = client.getInputStream();
                byte buffer[] = new byte[1024 * 16];
                int temp = 0;
                // 从InputStream当中读取客户端所发送的数据
                while ((temp = inputStream.read(buffer)) != -1) {
//                    System.out.println(new String(buffer, 0, temp));
                    result=new String(buffer, 0, temp);
                }




                Log.e("aaaa",result);



                EventBus.getDefault().post(new OfferEvent(result,client));








//                if (serverSocket.isClosed()){
//
//                    serverSocket.close();
//                    Log.e("aaaa","serverSocket");
//
//                }









                return result;
            } catch (IOException e) {


                Log.e("ServerAsyncTask", e.getMessage());
                return null;
            }

        }






        @Override
        protected void onPostExecute(String result) {
            if (result != null) {

                Log.e("ServerAsyncTask", result);


            }

        }


        @Override
        protected void onPreExecute() {
//            statusText.setText("Opening a server socket");
        }

    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "WhiteService->onStartCommand");


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setOngoing(true);
        builder.setPriority(android.support.v4.app.NotificationCompat.PRIORITY_MIN);
        builder.setVisibility(android.support.v4.app.NotificationCompat.VISIBILITY_SECRET);
        builder.setColor(R.color.trans);
        builder.setSmallIcon(R.drawable.empty);


        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.view_notification_type_0);
        remoteViews.setTextViewText(R.id.title_tv, "");
        remoteViews.setTextViewText(R.id.content_tv, "");
        remoteViews.setTextViewText(R.id.time_tv, getTime());
        remoteViews.setImageViewResource(R.id.icon_iv, R.color.trans);
        remoteViews.setInt(R.id.close_iv, "setColorFilter", getIconColor());



        //remoteViews.setInt(R.id.close_iv, "setTextColor", isDarkNotificationTheme( this)==true?Color.WHITE:Color.BLACK);
//        builder.setSmallIcon(R.mipmap.ic_launcher);
//
//        builder.setContentTitle(" ");
//        builder.setContentText(" ");
//        builder.setContentInfo("  ");
//        builder.setWhen(System.currentTimeMillis());
//        builder.setWhen(0);

        Intent activityIntent = new Intent(this, RtcActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notice_view_type_0, pendingIntent);


       // builder.setVisibility(-1);

        Notification notification = builder.build();


        if(android.os.Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
            notification.bigContentView = remoteViews;
        }

        notification.contentView = remoteViews;
        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
//        manager.notify(NOTICE_ID_TYPE_0, notification);


        startForeground(FOREGROUND_ID, notification);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);

        Log.i(TAG, "WhiteService->onDestroy");
        super.onDestroy();
    }




    @Override
    public void onLocalStream(MediaStream localStream) {

    }



    public static int getIconColor(){
        return Color.parseColor("#00000000");

    }


    private static String getTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.SIMPLIFIED_CHINESE);
        return format.format(new Date());
    }


    public static void clearNotification(Context context, int noticeId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(noticeId);
    }


    public static boolean isDarkNotificationTheme(Context context) {
        return !isSimilarColor(Color.BLACK, getNotificationColor(context));
    }


    public static int getNotificationColor(Context context) {
        NotificationCompat.Builder builder=new NotificationCompat.Builder(context);
        Notification notification=builder.build();
        int layoutId=notification.contentView.getLayoutId();
        ViewGroup viewGroup= (ViewGroup) LayoutInflater.from(context).inflate(layoutId, null, false);
        if (viewGroup.findViewById(android.R.id.title)!=null) {
            return ((TextView) viewGroup.findViewById(android.R.id.title)).getCurrentTextColor();
        }
        return findColor(viewGroup);
    }

    private static boolean isSimilarColor(int baseColor, int color) {
        int simpleBaseColor=baseColor|0xff000000;
        int simpleColor=color|0xff000000;
        int baseRed=Color.red(simpleBaseColor)-Color.red(simpleColor);
        int baseGreen=Color.green(simpleBaseColor)-Color.green(simpleColor);
        int baseBlue=Color.blue(simpleBaseColor)-Color.blue(simpleColor);
        double value=Math.sqrt(baseRed*baseRed+baseGreen*baseGreen+baseBlue*baseBlue);
        if (value<180.0) {
            return true;
        }
        return false;
    }


    private static int findColor(ViewGroup viewGroupSource) {
        int color=Color.TRANSPARENT;
        LinkedList<ViewGroup> viewGroups=new LinkedList<>();
        viewGroups.add(viewGroupSource);
        while (viewGroups.size()>0) {
            ViewGroup viewGroup1=viewGroups.getFirst();
            for (int i = 0; i < viewGroup1.getChildCount(); i++) {
                if (viewGroup1.getChildAt(i) instanceof ViewGroup) {
                    viewGroups.add((ViewGroup) viewGroup1.getChildAt(i));
                }
                else if (viewGroup1.getChildAt(i) instanceof TextView) {
                    if (((TextView) viewGroup1.getChildAt(i)).getCurrentTextColor()!=-1) {
                        color=((TextView) viewGroup1.getChildAt(i)).getCurrentTextColor();
                    }
                }
            }
            viewGroups.remove(viewGroup1);
        }
        return color;
    }




    public void onStateChanged( ServerlessRTCClient.State state) {

    }


}
