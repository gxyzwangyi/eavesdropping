package fr.pchab.androidrtc.dao;

import java.net.Socket;

/**
 * Created by wangyi on 16/11/29.
 */

public class AnswerEvent {
        public final String name;
        public  Socket socket;
        public AnswerEvent(String name,Socket socket) {
                this.name = name;
                this.socket = socket;
            }

}
