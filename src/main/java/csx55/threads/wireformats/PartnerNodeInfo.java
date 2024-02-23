package csx55.threads.wireformats;

import java.io.*;

public class PartnerNodeInfo implements Event {

    private final int messageType = Protocol.MESSAGING_NODES_LIST;
    private final String partnerNode;
    private final int numberOfThreads;

    public PartnerNodeInfo(String partnerNode, int numberOfThreads) {
        this.partnerNode = partnerNode;
        this.numberOfThreads = numberOfThreads;
    }

    public PartnerNodeInfo(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        int partnerNodeLength = din.readInt();
        byte[] partnerNodeBytes = new byte[partnerNodeLength];
        din.readFully(partnerNodeBytes);
        this.partnerNode = new String(partnerNodeBytes);

        this.numberOfThreads = din.readInt();

        bArrayInputStream.close();
        din.close();
    }

    public int getType() {
        return this.messageType;
    }

    public String getPartnerNode() {
        return partnerNode;
    }

    public int getNumberOfThreads() {
        return this.numberOfThreads;
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);

        byte[] partnerNodeBytes = this.partnerNode.getBytes();
        int elementLength = partnerNodeBytes.length;
        dout.writeInt(elementLength);
        dout.write(partnerNodeBytes);

        dout.writeInt(this.numberOfThreads);

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }
    
}