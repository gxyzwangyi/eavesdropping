//package fr.pchab.androidrtc.service;
//
//import android.app.IntentService;
//import android.content.Intent;
//import android.util.Log;
//
//import org.greenrobot.eventbus.EventBus;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//import java.net.InetSocketAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//import fr.pchab.androidrtc.dao.AnswerEvent;
//import fr.pchab.androidrtc.dao.OfferEvent;
//import fr.pchab.androidrtc.wifi.WiFiDirectActivity;
//
///**
// * Created by wangyi on 16/11/29.
// */
//
//public class NewService extends IntentService {
//    public static final String EXTRAS = "extra";
//
//
//
//    public NewService() {
//        super("NewService");
//    }
//
//
//
//
//    @Override
//    public void onHandleIntent(Intent intent) {
//
//        try {
//
//            String result="123";
//            ServerSocket serverSocket = new ServerSocket(8988);
//            Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
//            Socket client = serverSocket.accept();
//            Log.d(WiFiDirectActivity.TAG, "Server: connection done");
//
//
//            // 从Socket当中得到InputStream对象
//            InputStream inputStream = client.getInputStream();
//            byte buffer[] = new byte[1024 * 4];
//            int temp = 0;
//            // 从InputStream当中读取客户端所发送的数据
//            while ((temp = inputStream.read(buffer)) != -1) {
//                System.out.println(new String(buffer, 0, temp));
//                result=new String(buffer, 0, temp);
//            }
//
//
//
//
//
//
//
//            EventBus.getDefault().post(new OfferEvent(result,client));
//
//
//
//
//            OutputStream os=client.getOutputStream();
//            PrintWriter pw=new PrintWriter(os);
//            pw.write(reply);
//            pw.flush();
//            pw.close();
//            os.close();
////            serverSocket.close();
//
//
//
//
//
//
//        } catch (IOException e) {
//            Log.e("wrong", e.getMessage());
//        } finally {
//
//        }
//
//
//    }
//}