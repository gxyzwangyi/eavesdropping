package fr.pchab.androidrtc.wifi;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import fr.pchab.androidrtc.dao.AnswerEvent;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by wangyi on 16/10/16.
 */
public class NewService extends IntentService {
    public static final String EXTRAS = "extra";



    public NewService() {
        super("NewService");
    }





    @Override
    public void onHandleIntent(Intent intent) {




        String host = intent.getExtras().getString(EXTRAS);
        String p2p = intent.getExtras().getString("p2p");
        Log.e("sendserviceend",host);
//        String host =  info.groupOwnerAddress.getHostAddress();
       final Socket socket = new Socket();
        int port = 8988;

        try {
            Log.d("open", "Opening client socket - ");

            socket.bind(null);
            socket.connect((new InetSocketAddress(host, port)), 5000);


            Log.e("connect", "Client socket - " + socket.isConnected());






            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                            socket.getOutputStream()));
            writer.write(p2p);
            writer.flush();





            InputStream is=socket.getInputStream();
            BufferedReader br=new BufferedReader(new InputStreamReader(is));
            socket.shutdownOutput();
            //接收服务器的相应
            String reply=null;
            while(!((reply=br.readLine())==null)) {
                System.out.println("接收服务器的信息：" + reply);
                Log.e("客户端",reply);

                EventBus.getDefault().post(new AnswerEvent(reply));

            }



            br.close();
            is.close();





            Log.e("data", "Client: Data written");


        } catch (IOException e) {
            Log.e("wrong", e.getMessage());
        } finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }
        }


    }
}
