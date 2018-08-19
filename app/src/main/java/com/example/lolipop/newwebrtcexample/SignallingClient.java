package com.example.lolipop.newwebrtcexample;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SignallingClient {

    private static SignallingClient instance;
    private Socket socket;
    boolean isChannelReady = false;
    boolean isInitiator = false;
    boolean isStarted = false;
    private SignalingInterface callback;


    private Context context;
    Dialog dialog;

    // type
    private static final String kMessageType_Signal = "signal";
    private static final String kMessageType_Text = "text";
    private static final String kMessageType_Notification = "notification";


    // subtype
    private static final String kMessageSubtype_Req = "reqVoice";
    private static final String kMessageSubtype_Ack = "answerAckVoice";
    private static final String kMessageSubtype_Offer = "offer";
    private static final String kMessageSubtype_Answer = "answer";
    private static final String kMessageSubtype_Candidate = "candidate";
    private static final String kMessageSubtype_Close = "close";

    String to;
    String fromID;


    @SuppressLint("TrustAllX509TrustManager")
    private final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) {
        }
    }};


    public static SignallingClient getInstance() {
        if (instance == null) {
            instance = new SignallingClient();
        }
        return instance;
    }



    private Emitter.Listener onConnect = new Emitter.Listener() {

        JSONObject registerInfo = new JSONObject();


        @Override
        public void call(Object... args) {
            String android_id = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            try {
                registerInfo.put("name" , getDeviceName());
                registerInfo.put("id" , ""+android_id);
                registerInfo.put("specialty" , "special");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            socket.emit("register", registerInfo);
            getUserList();

        }
    };



    private void getUserList(){
        String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        socket.emit("list users", new String[0], args -> {
            ((MainActivity) context).runOnUiThread(new Runnable() {
                public void run() {
                    List<String> nameList = new ArrayList<String>();
                    List<String> idList = new ArrayList<String>();

                    dialog = new Dialog(context);
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Select user");

                    ListView modeList = new ListView(context);
                    //  String data =(JsonArray)args;
                    try {
                        JSONObject obj = new JSONObject(args[0].toString());
                        Iterator<String> keys = obj.keys();
                        //   Iterator<String> values = obj.vali();
                        int i = 0;

                        while (keys.hasNext()) {
                            String key = (String) keys.next(); // First key in your json object
                            String value = obj.getString(key);
                            JSONObject obj2 = new JSONObject(value);

                            nameList.add(obj2.getString("name"));
                            idList.add(obj2.getString("id"));

                            i++;
                        }
                        // obj.get(0)
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String[] stringArray = new String[nameList.size()];
                    nameList.toArray(stringArray);

                    ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
                    modeList.setAdapter(modeAdapter);

                    builder.setView(modeList);
                    dialog = builder.create();
                    dialog.show();

                    isInitiator = true;
                    isChannelReady = true;
                    isStarted = true;

                    modeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            ((MainActivity) context).start();
                            sendMessage(idList.get(i) , android_id);
                            dialog.dismiss();
                        }
                    });
                }
            });

        });

    }



    public void sendMessage(String userID , String fromID) {
        Log.d("JSON" , "fromID:"+fromID + " to:"+userID);
        JSONObject jsonObject3 = new JSONObject();
        try{
            jsonObject3.put("category" , 3);
            jsonObject3.put( "content","Incoming call answered.");
            jsonObject3.put("from" , fromID);
            jsonObject3.put("time", "");
            jsonObject3.put("to" , userID);
            jsonObject3.put("type" , kMessageType_Signal);
            jsonObject3.put("subtype", kMessageSubtype_Req);
        }catch (Exception e){
            e.printStackTrace();
        }

        Log.d("JSON_MESSAGE" , ""+jsonObject3.toString());
        socket.emit("chat message", jsonObject3);

    }



    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        return (model+manufacturer);
    }


    public void init(Context context , SignalingInterface signalingInterface){
        this.callback = signalingInterface;
        this.context = context;
        try{

            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, trustAllCerts, null);
            IO.setDefaultHostnameVerifier((hostname, session) -> true);
            IO.setDefaultSSLContext(sslcontext);

            //socket = IO.socket("https://139.59.248.179:1794/");
            socket = IO.socket("http://139.59.248.179:8000/");
            socket.connect();
            socket.on(Socket.EVENT_CONNECT, onConnect);

            isChannelReady = true;
            isInitiator = true;
            isStarted = true;

            socket.on("chat message" , args -> {
                JSONObject jsonObject = (JSONObject) args[0];
                try {

                    String type = jsonObject.getString("type");
                    String subType = jsonObject.getString("subtype");
                   to = jsonObject.getString("to");
                    fromID = jsonObject.getString("from");

                    if (type.equalsIgnoreCase(kMessageType_Signal)){

                        if (subType.equalsIgnoreCase(kMessageSubtype_Req)){
                            /**
                             *  If request call found answer the request call
                             */
                            onAnswerTheRequestCall(fromID , to);

                        }else if (subType.equalsIgnoreCase(kMessageSubtype_Ack)){

                            /**
                             *  If voice call accepted  send the offer
                             */

                            //callback.onTryToStart();

                            /// and send the offer
                            callback.onSendTheOffer(jsonObject);

                        }else if (subType.equalsIgnoreCase(kMessageSubtype_Offer)){

                            /**
                             *  If offer found answer the offer
                             */

                            callback.onOfferReceived(jsonObject);


                        }else if (subType.equalsIgnoreCase(kMessageSubtype_Answer)){

                            /**
                             *  if offer received initialized the local peer
                             *
                             */
                            callback.onAnswerReceived(jsonObject);

                        } else if (subType.equalsIgnoreCase(kMessageSubtype_Candidate)){
                            callback.onIceCandidateReceived(jsonObject);
                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });




        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void close() {
        //socket.emit("bye", roomName);
        socket.disconnect();
        socket.close();
    }


    public void emitIceCandidate(IceCandidate iceCandidate) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("candidate" , iceCandidate.sdp);
            jsonObject.put("sdpMLineIndex" , iceCandidate.sdpMLineIndex);
            jsonObject.put("sdpMid" , iceCandidate.sdpMid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(jsonObject);

        try {
            JSONObject object = new JSONObject();
            object.put("category" , 3);
            object.put("content" , jsonObject);
            object.put("type", kMessageType_Signal);
            object.put("subtype" ,kMessageSubtype_Candidate);
            object.put("time" , "");
            object.put("from" , to);
            object.put("to" , fromID);
            //object.put("sdpMLineIndex ", iceCandidate.sdpMLineIndex);
            //object.put("sdpMid", iceCandidate.sdpMid);
            //object.put("content ", iceCandidate.sdp);
            socket.emit("chat message", object);
            Log.d("JSON" , ""+object.toString());
            Log.d("JSON" , "From:"+fromID+"to:"+to);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void emitOffer(SessionDescription message  , String from , String to) {

        try {
            JSONObject obj = new JSONObject();
            obj.put("category" , 3);
            obj.put("from" , to);
            obj.put("to" , from);
            obj.put("time" , "");
            obj.put("type", kMessageType_Signal);
            obj.put("subtype" , kMessageSubtype_Offer);
            obj.put("content", message.description);
            socket.emit("chat message", obj);
            Log.d("JSON" , "OFFER_SEND:"+obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public void emitOfferAnswer(SessionDescription message  , String from , String to) {

        try {
            JSONObject obj = new JSONObject();
            obj.put("category" , 3);
            obj.put("from" , to);
            obj.put("to" , from);
            obj.put("time" , "");
            obj.put("type", kMessageType_Signal);
            obj.put("subtype" , kMessageSubtype_Answer);
            obj.put("content", message.description);
            socket.emit("chat message", obj);
            Log.d("JSON" , "OFFER_SEND:"+obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public void onAnswerTheRequestCall(String to  , String fromid){
        Log.d("JSON" , "send ack");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("category" , 3);
            jsonObject.put("content" , "Incoming call answered.");
            jsonObject.put("time"  , "");
            jsonObject.put("to" ,to );
            jsonObject.put("type", "signal");
            jsonObject.put("status" , "1");
            jsonObject.put("from" , fromid);
            jsonObject.put("subtype" , kMessageSubtype_Ack);
            socket.emit("chat message" , jsonObject);
            Log.d("JSON" , "SEND_ACK: "+jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }





    interface SignalingInterface {
        void onRemoteHangUp(String msg);

        void onOfferReceived(JSONObject data);

        void onAnswerReceived(JSONObject data);

        void onIceCandidateReceived(JSONObject data);

        //void onTryToStart();

        void onSendTheOffer(JSONObject jsonObject);
    }
}
