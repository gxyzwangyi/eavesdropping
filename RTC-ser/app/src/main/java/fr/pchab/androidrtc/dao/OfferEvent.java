package fr.pchab.androidrtc.dao;

import java.net.Socket;

/**
 * Created by wangyi on 16/11/29.
 */

public class OfferEvent {

        public final String name;
        public Socket socket;
        public OfferEvent(String name,Socket socket) {
            this.name = name;
            this.socket = socket;

        }

}
