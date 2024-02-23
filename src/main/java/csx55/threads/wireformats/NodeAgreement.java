package csx55.threads.wireformats;

import java.io.*;

public class NodeAgreement implements Event {

    private final int messageType = Protocol.NODE_AGREEMENT;
    private final int agreementPolicy;
    private final String sourceNode;

    public NodeAgreement(int agreementPolicy, String sourceNode) {
        this.sourceNode = sourceNode;
        this.agreementPolicy = agreementPolicy;
    }

    public NodeAgreement(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        this.agreementPolicy = din.readInt();

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

    public int getAgreementPolicy() {
        return this.agreementPolicy;
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);
        dout.writeInt(this.agreementPolicy);

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