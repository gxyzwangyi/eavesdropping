///*
// * Copyright (C) 2011 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package fr.pchab.androidrtc.wifi;
//
//import android.app.Activity;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.IntentFilter;
//import android.net.wifi.p2p.WifiP2pDevice;
//import android.net.wifi.p2p.WifiP2pDeviceList;
//import android.net.wifi.p2p.WifiP2pManager;
//import android.net.wifi.p2p.WifiP2pManager.Channel;
//import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.io.PrintWriter;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.ArrayList;
//import java.util.List;
//
//import fr.pchab.androidrtc.R;
//
////import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;
//
//
//public class WiFiDirectActivity extends Activity implements ChannelListener, WifiP2pManager.PeerListListener {
//
//    public static final String TAG = "wifidirectdemo";
//    private WifiP2pManager manager;
//    private boolean isWifiP2pEnabled = false;
//    private boolean retryChannel = false;
//    private WifiP2pDevice device;
//    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
//
//    private final IntentFilter intentFilter = new IntentFilter();
//    private Channel channel;
//    private BroadcastReceiver receiver = null;
//
//
//
//
//    /**
//     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
//     */
//    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
//        this.isWifiP2pEnabled = isWifiP2pEnabled;
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.main);
//
//
//        // add necessary intent values to be matched.
//
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
//        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
//
//        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//        channel = manager.initialize(this, getMainLooper(), null);
//
//
//        if (!isWifiP2pEnabled) {
//            Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
//                    Toast.LENGTH_SHORT).show();
//        }
//
//
//        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
//
//            @Override
//            public void onSuccess() {
//                Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
//                        Toast.LENGTH_SHORT).show();
//                new ServerAsyncTask(WiFiDirectActivity.this)
//                        .execute();
//            }
//
//            @Override
//            public void onFailure(int reasonCode) {
//                Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//
//
//    }
//
//
//
//    @Override
//    public void onPeersAvailable(WifiP2pDeviceList peerList) {
//
//        Log.e("onPeersAvailable","3");
//
//        peers.clear();
//        peers.addAll(peerList.getDeviceList());
//        if (peers.size() == 0) {
//            Log.d(WiFiDirectActivity.TAG, "No devices found");
//            return;
//        }
//
//    }
//
//    /** register the BroadcastReceiver with the intent values to be matched */
//    @Override
//    public void onResume() {
//        super.onResume();
//        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
//        registerReceiver(receiver, intentFilter);
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        unregisterReceiver(receiver);
//    }
//
//    /**
//     * Remove all peers and clear all fields. This is called on
//     * BroadcastReceiver receiving a state change event.
//     */
//    public void resetData() {
//
//    }
//
//
//
//    @Override
//    public void onChannelDisconnected() {
//        // we will try once more
//        if (manager != null && !retryChannel) {
//            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
//            resetData();
//            retryChannel = true;
//            manager.initialize(this, getMainLooper(), this);
//        } else {
//            Toast.makeText(this,
//                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
//                    Toast.LENGTH_LONG).show();
//        }
//    }
//
//
//
//
//    public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {
//
//        private Context context;
//        private TextView statusText;
//
//
//        ServerAsyncTask(Context context) {
//            this.context = context;
////            this.statusText = (TextView) statusText;
//        }
//
//        @Override
//        protected String doInBackground(Void... params) {
//            try {
//                String result="123";
//                ServerSocket serverSocket = new ServerSocket(8988);
//                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
//                Socket client = serverSocket.accept();
//                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
//
//
//
//                // 从Socket当中得到InputStream对象
//                InputStream inputStream = client.getInputStream();
//                byte buffer[] = new byte[1024 * 4];
//                int temp = 0;
//                // 从InputStream当中读取客户端所发送的数据
//                while ((temp = inputStream.read(buffer)) != -1) {
//                    System.out.println(new String(buffer, 0, temp));
//                    result=new String(buffer, 0, temp);
//                }
//
//
//
//                OutputStream os=client.getOutputStream();
//                PrintWriter pw=new PrintWriter(os);
//                String info=null;
//                String reply="welcome";
//                pw.write(reply);
//                pw.flush();
//                pw.close();
//                os.close();
//
//
//                serverSocket.close();
//                return result;
//            } catch (IOException e) {
//
//
//                Log.e(WiFiDirectActivity.TAG, e.getMessage());
//                return null;
//            }
//
//        }
//
//
//        @Override
//        protected void onPostExecute(String result) {
//            if (result != null) {
//
//
//                Toast.makeText(context,result, Toast.LENGTH_LONG).show();
//            }
//
//        }
//
//
//        @Override
//        protected void onPreExecute() {
////            statusText.setText("Opening a server socket");
//        }
//
//    }
//
//}
