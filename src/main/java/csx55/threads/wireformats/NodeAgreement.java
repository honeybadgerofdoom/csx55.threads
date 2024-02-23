package csx55.threads.wireformats;

import java.io.*;

public class NodeAgreement implements Event {

    private final int messageType = Protocol.NODE_AGREEMENT;
    private final int agreement;
    private final String sourceNode;

    public NodeAgreement(int agreement, String sourceNode) {
        this.sourceNode = sourceNode;
        this.agreement = agreement;
    }

    public NodeAgreement(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        this.agreement = din.readInt();

        int sourceNodeLength = din.readInt();
        byte[] sourceNodeBytes = new byte[sourceNodeLength];
        din.readFully(sourceNodeBytes);
        this.sourceNode = new String(sourceNodeBytes);

        bArrayInputStream.close();
        din.close();
    }

    public int getType() {
        return this.messageType;
    }

    public boolean iSentThisMessage(String id) {
        return id.equals(this.sourceNode);
    }

    public int getAgreement() {
        return this.agreement;
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);
        dout.writeInt(this.agreement);

        byte[] sourceNodeBytes = sourceNode.getBytes();
        int elementLength = sourceNodeBytes.length;
        dout.writeInt(elementLength);
        dout.write(sourceNodeBytes);

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    @Override
    public String toString() {
        return "NodeAgreement from source: " + sourceNode;
    }

}