package fr.pchab.androidrtc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.List;

import fr.pchab.androidrtc.service.WhiteService;
import fr.pchab.androidrtc.service.WifiService;
import fr.pchab.webrtcclient.PeerConnectionParameters;
import fr.pchab.webrtcclient.WebRtcClient;

public class RtcActivity extends Activity  implements ServerlessRTCClient.RtcListener {
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String VIDEO_CODEC_VP8 = "VP8";

    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 0;     //72
    private static final int LOCAL_Y_CONNECTED = 0;
    private static final int LOCAL_WIDTH_CONNECTED = 25 ;  //25
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
     private String callerId;
    private ServerlessRTCClient p2p_client;

    private BroadcastReceiver receiver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
//                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                        | LayoutParams.FLAG_TURN_SCREEN_ON
        );
        setContentView(R.layout.main);

        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {

            Intent whiteIntent = new Intent(RtcActivity.this, WhiteService.class);
            startService(whiteIntent);


//            Intent intent = new Intent(RtcActivity.this, WifiService.class);
//            startService(intent);

//                init();
            }
        });


//        final EditText input = (EditText)findViewById(R.id.edit_query);
//        Button button = (Button)findViewById(R.id.search_button) ;
//
//        button.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                p2p_client.processOffer( input.getText().toString());
//
//            }
//        });


        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);


    }



//    private void init() {
//
//
//
//
//    }





//    private void init() {
//        Point displaySize = new Point();
//        getWindowManager().getDefaultDisplay().getSize(displaySize);
//
//        PeerConnectionParameters params = new PeerConnectionParameters(
//                true, false, 1280, 720, 30, 1, VIDEO_CODEC_VP8, true, 1, AUDIO_CODEC_OPUS, true);
//
//        p2p_client = new ServerlessRTCClient(this,params,VideoRendererGui.getEGLContext());
//
//        p2p_client.init();
//
//     }

    @Override
    public void onPause() {
        super.onPause();
     //   vsv.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();
//        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
//        registerReceiver(receiver, intentFilter);
       // vsv.onResume();

    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }



    @Override
    public void onLocalStream(MediaStream localStream) {

        Log.e("!!!",localStream.label());
        Log.e("!!!",localStream.videoTracks.toString());
        Log.e("!!!",localStream.toString());

        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType);
    }



    @Override
    public void onBackPressed() {

        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);

    }

    public void onStateChanged( ServerlessRTCClient.State state) {

    }



}