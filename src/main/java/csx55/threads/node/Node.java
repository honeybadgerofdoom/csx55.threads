package csx55.threads.node;

import csx55.threads.transport.TCPServerThread;
import csx55.threads.util.EventAndSocket;
import csx55.threads.wireformats.Event;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface Node {

    public int getPortNumber();
    public ServerSocket getServerSocket();
    public void onEvent(Event event, Socket socket);

    default void startTCPServerThread() {
        TCPServerThread server = new TCPServerThread(this);
        Thread thread = new Thread(server);
        thread.start();
    }

}