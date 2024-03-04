package csx55.threads.hashing;

import java.nio.charset.StandardCharsets;
import java.io.*;

public class Task {
    private final String ip;
    private final int port;
    private final int roundNumber;
    private final int payload;
    private long timestamp;
    private long threadId;
    private int nonce;

    public Task(String ip, int port, int roundNumber, int payload) {
        this.ip = ip;
        this.port = port;
        this.roundNumber = roundNumber;
        this.payload = payload;
        this.timestamp = 0L;
        this.threadId = 0L;
        this.nonce = 0;
    }

    public Task(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        int ipLength = din.readInt();
        byte[] ipBytes = new byte[ipLength];
        din.readFully(ipBytes);
        this.ip = new String(ipBytes);
        this.port = din.readInt();
        this.roundNumber = din.readInt();
        this.payload = din.readInt();
        this.timestamp = din.readLong();
        this.threadId = din.readLong();
        this.nonce = din.readInt();
    }

    public void setTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    public void setThreadId() {
        this.threadId = Thread.currentThread().getId();
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getPayload() {
        return payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getNonce() {
        return nonce;
    }

    public String toString() {
        return ip + ":" + port + ":" + roundNumber + ":" + payload + ":" + timestamp + ":" + threadId + ":" + nonce;
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        byte[] ipBytes = ip.getBytes();
        int elementLength = ipBytes.length;
        dout.writeInt(elementLength);
        dout.write(ipBytes);

        dout.writeInt(port);
        dout.writeInt(roundNumber);
        dout.writeInt(payload);
        dout.writeLong(timestamp);
        dout.writeLong(threadId);
        dout.writeInt(nonce);

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    public byte[] toBytes() {
        return toString().getBytes(StandardCharsets.UTF_8);
    }
}