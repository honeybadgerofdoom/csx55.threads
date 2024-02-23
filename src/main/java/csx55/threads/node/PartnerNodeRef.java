package csx55.threads.node;

import csx55.threads.transport.TCPSender;
import csx55.threads.wireformats.Event;

import java.io.IOException;
import java.net.Socket;

public class PartnerNodeRef {

    private Socket socket;
    private TCPSender tcpSender;

    public PartnerNodeRef(Socket socket) {
        this.socket = socket;
        initializeTCPSender(socket);
    }

    private void initializeTCPSender(Socket socket) {
        try {
            this.tcpSender = new TCPSender(socket);
        } catch (IOException e) {
            System.out.println("Failed to initialize TCPSender " + e);
        }
    }

    public synchronized void writeToSocket(Event event) {
        try {
            byte[] bytes = event.getBytes();
            this.tcpSender.sendData(bytes);
        } catch (IOException e) {
            System.out.println("Failed to write to socket " + this.socket);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public String toString() {
        return socket.toString();
    }

}
