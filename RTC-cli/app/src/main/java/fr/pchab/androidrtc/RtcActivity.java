package fr.pchab.androidrtc;

import android.app.Activity;
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

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.List;

import fr.pchab.androidrtc.service.WhiteService;
import fr.pchab.webrtcclient.PeerConnectionParameters;
import fr.pchab.webrtcclient.WebRtcClient;

public class RtcActivity extends Activity  implements ServerlessRTCClient.RtcListener {
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String VIDEO_CODEC_VP8 = "VP8";

    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.

    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;

    private ServerlessRTCClient p2p_client;


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

//            Intent whiteIntent = new Intent(getApplicationContext(), WhiteService.class);
//            startService(whiteIntent);


                init();
            }
        });




//        Button button = (Button)findViewById(R.id.search_button) ;
//
//        final EditText input = (EditText)findViewById(R.id.edit_query);
//
//        Button button1= (Button)findViewById(R.id.search_button1) ;
//
//        Button button2= (Button)findViewById(R.id.search_button2) ;
//
//        button.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                p2p_client.makeOffer( );
//
//            }
//        });
//
//        button1.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                p2p_client.processAnswer(input.getText().toString());
//
//
//            }
//        });
//
//
//        button2.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                p2p_client.sendMessage(input.getText().toString());
//            }
//        });




        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);



    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);

        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, 1280, 720, 30, 1, VIDEO_CODEC_VP8, true, 1, AUDIO_CODEC_OPUS, true);


        p2p_client = new ServerlessRTCClient(this,params,VideoRendererGui.getEGLContext());

        p2p_client.init();



     }

    @Override
    public void onPause() {
        super.onPause();
        vsv.onPause();
        if(client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
        if(client != null) {
            client.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if(client != null) {
            client.onDestroy();
        }
        super.onDestroy();
    }







    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {
        Log.e("!!!",remoteStream.label());
        Log.e("!!!",remoteStream.videoTracks.toString());
        Log.e("!!!",remoteStream.toString());


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

    public void onStateChanged( ServerlessRTCClient.State state) {

    }



}