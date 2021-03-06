package com.example.lolipop.newwebrtcexample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class CallActivity extends AppCompatActivity implements SignallingClient.SignalingInterface , View.OnClickListener{

    PeerConnectionFactory peerConnectionFactory;
    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;

    SurfaceViewRenderer localVideoView;
    SurfaceViewRenderer remoteVideoView;
    VideoRenderer localRenderer;
    VideoRenderer remoteRenderer;

    PeerConnection localPeer;
    EglBase rootEglBase;
    boolean gotUserMedia;
    List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();
    private static final String TAG = "CallActivity";
    MediaStream stream;
    VideoCapturer videoCapturerAndroid;
    public Camera camera;
    ImageButton imageButton;
    ImageButton callEnd;
    ToggleButton micOnoff;
    ToggleButton cameraOnOff;
    ToggleButton loudSpeakerOnOff;

    public static boolean isBackCamera = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        callEnd = (ImageButton) findViewById(R.id.disconnectButton);
        micOnoff = (ToggleButton) findViewById(R.id.microphoneEnabledToggle);
        cameraOnOff = (ToggleButton) findViewById(R.id.cameraEnabledToggle);
        loudSpeakerOnOff = (ToggleButton) findViewById(R.id.loudSpeakerOnoff);

        setSupportActionBar(toolbar);
        camera = new Camera();

        Intent intent = getIntent();
        String to = intent.getExtras().getString("to");
        String from = intent.getExtras().getString("from");

        Log.d("ID" , "from:"+from+"to:"+to);

        initViews();
        initVideos();
        getIceServers();
        SignallingClient.getInstance().init(this ,  this);
        SignallingClient.getInstance().sendMessage(to , from);
        initPeerCoonectionGlobally();
        startWithFontCamera();
        imageButton = (ImageButton) findViewById(R.id.switchCameraButton);

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBackCamera==true){
                    streamBackCamera();
                    isBackCamera = false;
                }else {
                    streamFontCamera();
                    isBackCamera = true;
                }
            }
        });

        callEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignallingClient.getInstance().sendCallEndMessage();
                hangup();
            }
        });

        micOnoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleclick();
            }
        });

        cameraOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraOnOff();
            }
        });

        loudSpeakerOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLoudSpeakerOnOff();
            }
        });


    }


    private void initViews() {

        localVideoView = findViewById(R.id.local_gl_surface_view);
        remoteVideoView = findViewById(R.id.remote_gl_surface_view);
    }

    private void initPeerCoonectionGlobally(){
        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableVideoHwAcceleration(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        peerConnectionFactory = new PeerConnectionFactory(options, defaultVideoEncoderFactory, defaultVideoDecoderFactory);
    }


    private void initVideos() {
        rootEglBase = EglBase.create();
        localVideoView.init(rootEglBase.getEglBaseContext(), null);
        remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
        localVideoView.setZOrderMediaOverlay(true);
        remoteVideoView.setZOrderMediaOverlay(true);
    }

    private void getIceServers() {
        PeerConnection.IceServer peerIceServer = PeerConnection.IceServer.builder("turn.server")
                .setUsername("")
                .setPassword("")
                .createIceServer();
        peerIceServers.add(peerIceServer);
    }



    public void startWithFontCamera() {

        //Now create a VideoCapturer instance.

        videoCapturerAndroid = camera.openFontCamera(new Camera1Enumerator(false));

        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();

        //Create a VideoSource instance
        if (videoCapturerAndroid != null) {
            videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
        }
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);


        if (videoCapturerAndroid != null) {
            videoCapturerAndroid.startCapture(1024, 720, 30);
        }

        localVideoView.setVisibility(View.VISIBLE);
        //create a videoRenderer based on SurfaceViewRenderer instance
        localRenderer = new VideoRenderer(localVideoView);
        // And finally, with our VideoRenderer ready, we
        // can add our renderer to the VideoTrack.
        localVideoTrack.addRenderer(localRenderer);

        localVideoView.setMirror(true);
        remoteVideoView.setMirror(true);

        gotUserMedia = true;
        createPeerConnection();
    }


    public void streamFontCamera() {

        //Now create a VideoCapturer instance.
        if (videoCapturerAndroid!=null){
            try {
                videoCapturerAndroid.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (localVideoTrack!=null){
            stream.removeTrack(localVideoTrack);
        }

        localVideoTrack.removeRenderer(localRenderer);
        videoCapturerAndroid = camera.openFontCamera(new Camera1Enumerator(false));

        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();

        //Create a VideoSource instance
        if (videoCapturerAndroid != null) {
            videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
        }
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);


        if (videoCapturerAndroid != null) {
            videoCapturerAndroid.startCapture(1024, 720, 30);
        }
        localVideoView.setVisibility(View.VISIBLE);
        //create a videoRenderer based on SurfaceViewRenderer instance
        localRenderer = new VideoRenderer(localVideoView);
        // And finally, with our VideoRenderer ready, we
        // can add our renderer to the VideoTrack.
        localVideoTrack.addRenderer(localRenderer);

        localVideoView.setMirror(true);
        remoteVideoView.setMirror(true);

        gotUserMedia = true;
        stream.addTrack(localVideoTrack);
    }

    public void streamBackCamera() {

        //Now create a VideoCapturer instance.
        if (videoCapturerAndroid!=null){
            try {
                videoCapturerAndroid.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (localVideoTrack!=null){
            stream.removeTrack(localVideoTrack);
        }

        localVideoTrack.removeRenderer(localRenderer);
        videoCapturerAndroid = camera.openBackCamera(new Camera1Enumerator(false));

        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();

        //Create a VideoSource instance
        if (videoCapturerAndroid != null) {
            videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
        }
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);


        if (videoCapturerAndroid != null) {
            videoCapturerAndroid.startCapture(1024, 720, 30);
        }
        localVideoView.setVisibility(View.VISIBLE);
        //create a videoRenderer based on SurfaceViewRenderer instance
        localRenderer = new VideoRenderer(localVideoView);
        // And finally, with our VideoRenderer ready, we
        // can add our renderer to the VideoTrack.
        localVideoTrack.addRenderer(localRenderer);

        localVideoView.setMirror(true);
        remoteVideoView.setMirror(true);

        gotUserMedia = true;
        stream.addTrack(localVideoTrack);
    }



    public void toggleclick(){

        if(micOnoff.isChecked()){
            stream.removeTrack(localAudioTrack);
            Toast.makeText(this, "Mic off", Toast.LENGTH_SHORT).show();
        }else {
            stream.addTrack(localAudioTrack);
            Toast.makeText(this, "Mic on", Toast.LENGTH_SHORT).show();
        }
    }


    public void cameraOnOff(){

        if(cameraOnOff.isChecked()){
            stream.removeTrack(localVideoTrack);
            Toast.makeText(this, "Video off", Toast.LENGTH_SHORT).show();
        }else {
            stream.addTrack(localVideoTrack);
            Toast.makeText(this, "Video on", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoudSpeakerOnOff(){

        @SuppressLint({"NewApi", "LocalSuppress"}) AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        //audioManager.setMode(AudioManager.MODE_IN_CALL);
        //audioManager.setSpeakerphoneOn(false);
        if (loudSpeakerOnOff.isChecked()){
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(true);
        }else {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(false);
        }
    }

    @Override
    public void onRemoteHangUp(String msg) {

    }

    @Override
    public void onAnswerReceived(JSONObject data) {
        showToast("Received Answer");
        Log.d("JSON" , ""+data.toString());
        try {
            localPeer.setRemoteDescription(new CustomSdpObserver(), new SessionDescription(SessionDescription.Type.fromCanonicalForm(data.getString("subtype").toLowerCase()), data.getString("content")));
            updateVideoViews(true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onIceCandidateReceived(IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        SignallingClient.getInstance().emitIceCandidate(iceCandidate);
    }


    @Override
    public void onIceCandidateReceived(JSONObject data) {
        String sdpMid = null;
        int sdpMLineIndex = 0;
        String candidate = null;
        JSONObject jsonObject = null;

        try {
            jsonObject = data.getJSONObject("content");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            sdpMid = jsonObject.getString("sdpMid");
            sdpMLineIndex = jsonObject.getInt("sdpMLineIndex");
            candidate = jsonObject.getString("candidate");
            Log.d("JSON_SDP" , "sdpMid:"+sdpMid +"sdpMLineIndex:"+sdpMLineIndex +"candidate:"+candidate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        localPeer.addIceCandidate(new IceCandidate(sdpMid, sdpMLineIndex,candidate));
    }

    @Override
    public void callEnd() {
        showToast("Call End");
        hangup();
    }


    @Override
    public void onSendTheOffer(JSONObject jsonObject) {
        //createPeerConnection();
        showToast("Sending the offer");
        try {
            String fromid = jsonObject.getString("from");
            String to = jsonObject.getString("to");

            runOnUiThread(() -> {
                if (!SignallingClient.getInstance().isInitiator && !SignallingClient.getInstance().isStarted) {
                    createPeerConnection();
                }
                updateVideoViews(true);
            });

            //create sdpConstraints
            sdpConstraints = new MediaConstraints();
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

            localPeer.createOffer(new CustomSdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    super.onCreateSuccess(sessionDescription);
                    localPeer.setLocalDescription(new CustomSdpObserver(), sessionDescription);
                    Log.d("onCreateSuccess", "SignallingClient emit ");
                    SignallingClient.getInstance().emitOffer(sessionDescription , fromid , to);
                }
            }, sdpConstraints);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onOfferReceived(JSONObject data) {
        showToast("Received Offer");
        String content = null;
        try {
            content = data.getString("content");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("JSON_CONTENT" , ""+content);
        runOnUiThread(() -> {
            if (!SignallingClient.getInstance().isInitiator && !SignallingClient.getInstance().isStarted) {
                createPeerConnection();
                updateVideoViews(true);
            }

            try {
                localPeer.setRemoteDescription(new CustomSdpObserver(), new SessionDescription(SessionDescription.Type.OFFER, data.getString("content")));
                doAnswer(data.getString("from") , data.getString("to"));
                updateVideoViews(true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void doAnswer(String from , String to) {
        localPeer.createAnswer(new CustomSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver(), sessionDescription);
                SignallingClient.getInstance().emitOfferAnswer(sessionDescription , from , to);
            }
        }, new MediaConstraints());
    }


    @Override
    protected void onDestroy() {
        SignallingClient.getInstance().close();
        super.onDestroy();
    }

    /**
     * Util Methods
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(CallActivity.this, msg, Toast.LENGTH_SHORT).show());
    }





    @Override
    public void onClick(View v) {
    }

    private void hangup() {

        try {
            localPeer.close();
            localPeer = null;
            //SignallingClient.getInstance().close();
            updateVideoViews(false);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void updateVideoViews(final boolean remoteVisible) {
        runOnUiThread(() -> {
            ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
            if (remoteVisible) {
                params.height = dpToPx(100);
                params.width = dpToPx(100);
            } else {
                params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            localVideoView.setLayoutParams(params);
        });

    }


    private void createPeerConnection() {

        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(peerIceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;


        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                showToast("Received Remote stream");
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }
        });

        addStreamToLocalPeer();
    }



    private void addStreamToLocalPeer() {
        //creating local mediastream
        stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        localPeer.addStream(stream);
    }



    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        runOnUiThread(() -> {
            try {
                remoteRenderer = new VideoRenderer(remoteVideoView);
                remoteVideoView.setVisibility(View.VISIBLE);
                videoTrack.addRenderer(remoteRenderer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

}
