package csx55.threads.wireformats;

import java.io.*;
import java.util.*;

public class MessagingNodesList implements Event {

    private final int messageType = Protocol.MESSAGING_NODES_LIST;
    private final int numberOfPeerMessagingNodes;
    private final List<String> info;
    private final int numberOfThreads;

    public MessagingNodesList(List<String> info, int numberOfThreads) {
        this.numberOfPeerMessagingNodes = info.size();
        this.info = info;
        this.numberOfThreads = numberOfThreads;
    }

    public MessagingNodesList(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        this.numberOfPeerMessagingNodes = din.readInt();

        this.info = new ArrayList<>();
        for (int i = 0; i < this.numberOfPeerMessagingNodes; i++) {
            int currentInfoLength = din.readInt();
            byte[] currentInfoBytes = new byte[currentInfoLength];
            din.readFully(currentInfoBytes);
            this.info.add(new String(currentInfoBytes));
        }

        this.numberOfThreads = din.readInt();

        bArrayInputStream.close();
        din.close();
    }

    public int getType() {
        return this.messageType;
    }

    public List<String> getInfo() {
        return this.info;
    }

    public int getNumberOfThreads() {
        return this.numberOfThreads;
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);
        dout.writeInt(this.numberOfPeerMessagingNodes);

        for (String partnerInfo : this.info) {
            byte[] infoBytes = partnerInfo.getBytes();
            int elementLength = infoBytes.length;
            dout.writeInt(elementLength);
            dout.write(infoBytes);
        }

        dout.writeInt(this.numberOfThreads);

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }
    
}