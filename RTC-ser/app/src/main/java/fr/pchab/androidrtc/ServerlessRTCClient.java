package fr.pchab.androidrtc;


 import android.content.Context;
 import android.opengl.EGLContext;
 import android.util.Log;

 import org.greenrobot.eventbus.EventBus;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.webrtc.*;

 import java.net.Socket;
 import java.nio.ByteBuffer;
 import java.nio.charset.Charset;
 import java.util.LinkedList;
 import java.util.Locale;
 import java.util.Objects;

 import fr.pchab.androidrtc.dao.AnswerEvent;
 import fr.pchab.androidrtc.dao.OfferEvent;
 import fr.pchab.webrtcclient.PeerConnectionParameters;
 import fr.pchab.webrtcclient.WebRtcClient;


/**
 * This class handles all around WebRTC peer connections.
 */
public class ServerlessRTCClient {
    private VideoSource videoSource;
    private AudioSource audioSource;
    private PeerConnection pc;
    private Boolean pcInitialized = false;
    private DataChannel channel = null;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private PeerConnectionParameters pcParams;

    private PeerConnectionFactory pcf;
    private MediaConstraints pcConstraints = new MediaConstraints();
    private RtcListener mListener;
    private String TAG = "leviathan";
    private State state;
    private MediaStream localMS;
    private Charset UTF_8 = Charset.forName("UTF-8");
    MediaConstraints videoConstraints;

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
        setCamera("0");
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








    private  class DefaultObserver implements SdpObserver, PeerConnection.Observer {
//       private PeerConnection pc;


        @Override
        public void onCreateSuccess(final SessionDescription sdp) {
            // TODO: modify sdp to use pcParams prefered codecs
            try {
                pc.setLocalDescription(ServerlessRTCClient.DefaultObserver.this, sdp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSetSuccess() {}

        @Override
        public void onCreateFailure(String s) {}

        @Override
        public void onSetFailure(String s) {}

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
        public void onIceCandidate(final IceCandidate candidate) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("label", candidate.sdpMLineIndex);
                payload.put("id", candidate.sdpMid);
                payload.put("candidate", candidate.sdp);
//                            sendMessage(id, "candidate", payload);


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.e(TAG, "onAddStream " + mediaStream.label());

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



        public DefaultObserver() {
            pc = pcf.createPeerConnection(iceServers, pcConstraints, this);
            pc.addStream(localMS);

            Log.e("???1",localMS.label());
            Log.e("???1",localMS.videoTracks.toString());
            Log.e("???1",localMS.toString());

        }


    }






    public class DefaultSdpObserver implements SdpObserver {
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




    public class DefaultDataChannelObserver implements DataChannel.Observer {


        public void onMessage(DataChannel.Buffer DCB) {

            ByteBuffer buf = DCB.data;

            if (buf != null) {
                byte[] byteArray = new byte[buf.remaining()];
                buf.get(byteArray);
                String received = new String(byteArray, UTF_8);

                try {
                    String message = new JSONObject(received).getString(JSON_MESSAGE);
                    Log.e("1", message);

                    if (message.equals("stop")){

                        videoSource.stop();
                    }
                    if (message.equals("start")){
                        videoSource.restart();
                    }
                    if (message.equals("end")){
                        videoSource.dispose();
                    }



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
     * Process offer that was entered by user (this is called getOffer() in JavaScript example)
     */
    public void processOffer(String sdpJSON, final Socket socket) {
        try {
            JSONObject json = new JSONObject(sdpJSON);
            String type = json.getString(JSON_TYPE);
            String sdp = json.getString(JSON_SDP);
            state = State.CREATING_ANSWER;

            Log.e("sdp",sdp);

            if (type != null && sdp != null && type .equals("offer") ) {
                SessionDescription offer = new SessionDescription(SessionDescription.Type.OFFER, sdp);
                pcInitialized = true;
                pc = pcf.createPeerConnection(iceServers, pcConstraints, new DefaultObserver() {

                @Override
                public void onIceCandidate(final IceCandidate candidate){
//                Log.e("candidate",candidate.toString());
                }


                    @Override
                    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

                        super.onIceGatheringChange(iceGatheringState);
                        //ICE gathering complete, we should have answer now
                        if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE) {
                            doShowAnswer(pc.getLocalDescription(),socket);

                            state = State.WAITING_TO_CONNECT;
                        }
                    }


                    @Override
                    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {

                        }
                    }


                    @Override
                    public void onDataChannel(DataChannel dc) {
                        super.onDataChannel(dc);
                        channel = dc;
                        dc.registerObserver(new DefaultDataChannelObserver());
                    }


                });


                //we have remote offer, let's create answer for that
                pc.setRemoteDescription(new DefaultSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        super.onSetSuccess();
                        Log.e("1", "Remote description set.");
                        pc.addStream(localMS);

                        Log.e("???2",localMS.label());
                        Log.e("???2",localMS.videoTracks.toString());
                        Log.e("???2",localMS.toString());
                        pc.createAnswer(new DefaultSdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription p0) {
                                //answer is ready, set it
                                Log.e("1", "Local description set.");
                                pc.setLocalDescription(new DefaultSdpObserver(), p0);
                            }
                        }, pcConstraints);
                    }
                }, offer);



            } else {
                Log.e("1", "Invalid or unsupported offer.");
                state = State.WAITING_FOR_OFFER;
            }
        } catch (JSONException e) {
            Log.e("1", "bad json");
            state = State.WAITING_FOR_OFFER;
        }

    }



    public void doShowAnswer(SessionDescription sdp,Socket socket) {
        Log.e("1", "Here is your answer:");
        Log.e("1", sessionDescriptionToJSON(sdp).toString());
        String answer = sessionDescriptionToJSON(sdp).toString();
        EventBus.getDefault().post(new AnswerEvent(answer,socket));
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
//            setCamera();
            Log.e("aaa",sendJSON.toString());

             try {
                 ByteBuffer buf = ByteBuffer.wrap(sendJSON.toString().getBytes(UTF_8));
                 channel.send(new DataChannel.Buffer(buf, false));

             }
             catch (Exception e){
                 Log.e("",e.toString());

             }

        } else {
            Log.e("1", "Error. Chat is not established.");
        }
    }


    /**
     * Call this before using anything else from PeerConnection.
     */
    public void init() {

        state = State.INITIALIZING;


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




    public void setCamera(String flag){
        localMS = pcf.createLocalMediaStream("ARDAMS");
        if(pcParams.videoCallEnabled){
             videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(pcParams.videoHeight)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(pcParams.videoWidth)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(pcParams.videoFps)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(pcParams.videoFps)));

            videoSource = pcf.createVideoSource(getVideoCapturer(flag), videoConstraints);

            localMS.addTrack(pcf.createVideoTrack("ARDAMSv0", videoSource));
        }

                 audioSource = pcf.createAudioSource(new MediaConstraints());
                 localMS.addTrack(pcf.createAudioTrack("ARDAMSa0", audioSource));

    }


    private VideoCapturer getVideoCapturer(String flag) {
        if (flag.equals("1")) {
            String frontCameraDeviceName = VideoCapturerAndroid.getNameOfFrontFacingDevice();
            return VideoCapturerAndroid.create(frontCameraDeviceName);
        }
        else
        {
            String backCameraDeviceName = VideoCapturerAndroid.getNameOfBackFacingDevice();
            return VideoCapturerAndroid.create(backCameraDeviceName);
        }

    }


    public interface RtcListener{

        void onLocalStream(MediaStream localStream);

    }

}

