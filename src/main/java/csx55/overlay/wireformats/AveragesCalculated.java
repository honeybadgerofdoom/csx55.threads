package csx55.overlay.wireformats;

import java.util.List;
import java.util.ArrayList;
import java.io.*;

public class AveragesCalculated implements Event {

    private int messageType = Protocol.AVERAGES_CALCULATED;
    private int numberOfNodes;
    private List<String> nodeIds;

    public AveragesCalculated(String id) {
        this.numberOfNodes = 1;
        this.nodeIds = new ArrayList<>();
        this.nodeIds.add(id);
    }

    public AveragesCalculated(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        this.numberOfNodes = din.readInt();

        this.nodeIds = new ArrayList<>();
        for (int i = 0; i < this.numberOfNodes; i++) {
            int currentInfoLength = din.readInt();
            byte[] currentInfoBytes = new byte[currentInfoLength];
            din.readFully(currentInfoBytes);
            this.nodeIds.add(new String(currentInfoBytes));
        }

        bArrayInputStream.close();
        din.close();
    }

    public int getType() {
        return this.messageType;
    }

    public String processRelay(String id) {
        String lastNode = this.nodeIds.get(this.numberOfNodes - 1);
        this.nodeIds.add(id);
        this.numberOfNodes++;
        return lastNode;
    }

    public boolean nodeIsFirst(String id) {
        return id.equals(this.nodeIds.get(0));
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);
        dout.writeInt(this.numberOfNodes);

        for (String id : this.nodeIds) {
            byte[] idBytes = id.getBytes();
            int elementLength = idBytes.length;
            dout.writeInt(elementLength);
            dout.write(idBytes);
        }

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

}