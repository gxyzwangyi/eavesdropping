package fr.pchab.androidrtc;


 import android.content.Context;
 import android.opengl.EGLContext;
 import android.util.Log;

 import org.greenrobot.eventbus.EventBus;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.webrtc.*;

 import java.io.Serializable;
 import java.nio.ByteBuffer;
 import java.nio.CharBuffer;
 import java.nio.charset.Charset;
 import java.nio.charset.CharsetDecoder;
 import java.util.LinkedList;
 import java.util.Objects;

 import fr.pchab.androidrtc.dao.MakeEvent;
 import fr.pchab.androidrtc.wifi.NewService;
 import fr.pchab.androidrtc.wifi.WiFiDirectActivity;
 import fr.pchab.webrtcclient.PeerConnectionParameters;
 import fr.pchab.webrtcclient.WebRtcClient;
 import rx.Observable;
 import rx.Subscriber;


/**
 * This class handles all around WebRTC peer connections.
 */
public class ServerlessRTCClient implements Serializable {

    private static final long serialVersionUID=1L;

    private PeerConnection pc;
    private Boolean pcInitialized = false;
    private DataChannel channel = null;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
//    private Byte<> aByte = new Byte<>();

     private MediaStream localMS;

    private PeerConnectionFactory pcf;
    private MediaConstraints pcConstraints = new MediaConstraints();
    private RtcListener mListener;

    private String TAG = "leviathan";
    private State state;

    private Charset UTF_8 = Charset.forName("UTF-8");

    private PeerConnectionParameters pcParams;


    public ServerlessRTCClient(RtcListener listener,PeerConnectionParameters params, EGLContext mEGLcontext) {

        mListener = listener;

        pcParams = params;

         PeerConnectionFactory.initializeAndroidGlobals(
                listener, true, true, params.videoCodecHwAcceleration,mEGLcontext);
         pcf = new PeerConnectionFactory();


        iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        state = State.INITIALIZING;

    }


    public enum State {
        /**
         * Initialization in progress.
         */
        INITIALIZING,
        /**
         * App is waiting for offer, fill in the offer into the edit text.
         */
        WAITING_FOR_OFFER,
        /**
         * App is creating the offer.
         */
        CREATING_OFFER,
        /**
         * App is creating answer to offer.
         */
        CREATING_ANSWER,
        /**
         * App created the offer and is now waiting for answer
         */
        WAITING_FOR_ANSWER,
        /**
         * Waiting for establishing the connection.
         */
        WAITING_TO_CONNECT,
        /**
         * Connection was established. You can chat now.
         */
        CHAT_ESTABLISHED,
        /**
         * Connection is terminated chat ended.
         */
        CHAT_ENDED
    }










    public abstract class DefaultObserver implements PeerConnection.Observer, Serializable  {

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.e("1", "data channel ${p0?.label()} estabilished");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                Log.e("1", "closing channel");
                channel.close();
            }
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.e(TAG, "onAddStream " + mediaStream.label());
            mListener.onAddRemoteStream(mediaStream);

        }
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "onRemoveStream " + mediaStream.label());
        }



        @Override
        public void onRenegotiationNeeded() {
        }
    }





    public class DefaultSdpObserver implements SdpObserver,Serializable {
        @Override
        public void onCreateSuccess(final SessionDescription sdp) {
        }

        @Override
        public void onSetSuccess() {
            Log.e("1", "set success");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.e("1", "failed to create offer:");
        }

        @Override
        public void onSetFailure(String s) {
            Log.e("1", s);
        }
    }




    public class DefaultDataChannelObserver implements DataChannel.Observer,Serializable {


        public void onMessage(DataChannel.Buffer DCB) {

            ByteBuffer buf = DCB.data;



            if (buf != null) {
                byte[] byteArray = new byte[buf.remaining()];
                buf.get(byteArray);
                String received = new String(byteArray,UTF_8);


                Log.e("aaaa",received);
                try {
                    String message = new JSONObject(received).getString(JSON_MESSAGE);
                    Log.e("1", message);



                } catch (JSONException e) {
                    Log.e("1", "Malformed message received");
                }
            }
        }






        public void onStateChange() {
            Log.e("1", "Channel state changed:${channel.state()?.name}}");
            if (channel.state() == DataChannel.State.OPEN) {
                state = State.CHAT_ESTABLISHED;
                Log.e("1", "Chat established.");
            } else {
                state = State.CHAT_ENDED;
                Log.e("1", "Chat ended.");
            }
        }
    }



    private String JSON_TYPE = "type";
    private String JSON_MESSAGE = "message";
    private String JSON_SDP = "sdp";

    /**
     * Converts session desription object to JSON object that can be used in other applications.
     * This is what is passed between parties to maintain connection. We need to pass the session description to the other side.
     * In normal use case we should use some kind of signalling server, but for this demo you can use some other method to pass it there (like e-mail).
     */
    private JSONObject sessionDescriptionToJSON(SessionDescription sessDesc) {
        JSONObject json = new JSONObject();
        try {
            json.put(JSON_TYPE, sessDesc.type.canonicalForm());
            json.put(JSON_SDP, sessDesc.description);
        }
        catch (JSONException e) {
        }
        return json;

    }


    /**
     * Wait for an offer to be entered by user.
     */
    public void waitForOffer() {
        state = State.WAITING_FOR_OFFER;
    }





    /**
     * Process answer that was entered by user (this is called getAnswer() in JavaScript example)
     */
    public void processAnswer(String sdpJSON) {
        try {
            JSONObject json = new JSONObject(sdpJSON);
            String type = json.getString(JSON_TYPE);
            String sdp = json.getString(JSON_SDP);
            state = State.WAITING_TO_CONNECT;
            if (type != null && sdp != null && type.equals("answer")) {
                SessionDescription answer = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
                pc.setRemoteDescription(new DefaultSdpObserver(), answer);
            } else {
                Log.e("1", "Invalid or unsupported answer.");
                state = State.WAITING_FOR_ANSWER;
            }
        } catch (JSONException e) {
            Log.e("1", "bad json");
            state = State.WAITING_FOR_ANSWER;
        }
    }

    public void doShowAnswer(SessionDescription sdp) {
        Log.e("1", "Here is your answer:");
        Log.e("1", sessionDescriptionToJSON(sdp).toString());
    }

    /**
     * App creates the offer.
     */
    public void  makeOffer()  {
        state = State.CREATING_OFFER;
        pcInitialized = true;
        Observable<String> myObservable;

         DefaultObserver defaultObserver = new DefaultObserver() {
            public   String sdp;
                @Override
                public void onIceCandidate(IceCandidate p0) {
//                Log.e("make", p0.toString());
                }

            public void onIceGatheringChange(PeerConnection.IceGatheringState p0) {
                super.onIceGatheringChange(p0);
                if (p0 == PeerConnection.IceGatheringState.COMPLETE) {
                    Log.e("make", "Your offer is:");
                    state = State.WAITING_FOR_ANSWER;
//                    Log.e("hhhhh", pc.getLocalDescription().description);

                    sdp = sessionDescriptionToJSON(pc.getLocalDescription()).toString();
//                    Log.e("hhhhh", sdp);
//                    Log.e("hhhhh", sdp.substring(sdp.length()-5,sdp.length()));



                    EventBus.getDefault().post(new MakeEvent(sdp));




                }
            }

        };


        pc = pcf.createPeerConnection(iceServers, pcConstraints, defaultObserver);

        makeDataChannel();
        pc.createOffer(new DefaultSdpObserver() {
            public void onCreateSuccess(SessionDescription p0) {
                if (p0 != null) {
                    Log.e("make", "offer updated");
                    pc.setLocalDescription(new DefaultSdpObserver() {
                        public void onCreateSuccess(SessionDescription p0) {
                        }
                    }, p0);
                }
            }
        }, pcConstraints);





    }

    /**
     * Sends message to other party.
     */
    public void sendMessage(String message) {
        if (channel == null || state == State.CHAT_ESTABLISHED) {

            JSONObject sendJSON = new JSONObject();
            try {
                sendJSON.put(JSON_MESSAGE, message);
            }
            catch (JSONException e) {
                Log.e("",e.toString());
            }

            try {
                ByteBuffer buf = ByteBuffer.wrap(sendJSON.toString().getBytes(UTF_8));
                channel.send(new DataChannel.Buffer(buf, false));

            }
            catch (Exception E){
              Log.e("",E.toString());
            }


        } else {
            Log.e("1", "Error. Chat is not established.");
        }
    }

    /**
     * Creates data channel for use when offer is created on this machine.
     */
    public void makeDataChannel() {
        DataChannel.Init init = new DataChannel.Init();
        channel = pc.createDataChannel("test", init);
        channel.registerObserver(new DefaultDataChannelObserver());
    }

    /**
     * Call this before using anything else from PeerConnection.
     */
    public void init() {
//        PeerConnectionParameters params, EGLContext mEGLcontext
//        PeerConnectionFactory.initializeAndroidGlobals(
//                this, false, true, params.videoCodecHwAcceleration, mEGLcontext);
        state = State.INITIALIZING;

    }




public void AddIceCandidateCommand(JSONObject payload) throws JSONException{

    Log.d(TAG,"AddIceCandidateCommand");
//    PeerConnection pc = peers.get(peerId).pc;
    if (pc.getRemoteDescription() != null) {
        IceCandidate candidate = new IceCandidate(
                payload.getString("id"),
                payload.getInt("label"),
                payload.getString("candidate")
        );
        pc.addIceCandidate(candidate);
    }
}













    /**
     * Clean up some resources.
     */
    public void destroy() {
        channel.close();
        if (pcInitialized) {
            pc.close();
        }

    }



    public interface IStateChangeListener {
        /**
         * Called when status of client is changed.
         */
        void onStateChanged(State state);
    }

    public interface RtcListener{


        void onAddRemoteStream(MediaStream remoteStream );

        void onRemoveRemoteStream(int endPoint);
    }
}

