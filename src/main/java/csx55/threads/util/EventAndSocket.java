package csx55.threads.util;

import csx55.threads.wireformats.Event;

import java.net.Socket;

public class EventAndSocket {

    private Event event;
    private Socket socket;

    public EventAndSocket(Event event, Socket socket) {
        this.event = event;
        this.socket = socket;
    }

    public Event getEventObject() {
        return event;
    }

    public Socket getSocket() {
        return socket;
    }

}
