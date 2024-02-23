package csx55.threads.node;

import csx55.threads.transport.TCPServerThread;
import csx55.threads.wireformats.Event;

import java.net.ServerSocket;
import java.net.Socket;

public interface Node {

    int getPortNumber();
    ServerSocket getServerSocket();
    void onEvent(Event event, Socket socket);

    default void startTCPServerThread() {
        TCPServerThread server = new TCPServerThread(this);
        Thread thread = new Thread(server);
        thread.start();
    }

}