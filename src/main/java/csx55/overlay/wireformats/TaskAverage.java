package csx55.overlay.wireformats;

import csx55.overlay.node.MessagingNode;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

public class TaskAverage implements Event {

    private int messageType = Protocol.MESSAGING_NODES_LIST;
    private double average;
    private int numberOfNodes;
    private List<String> nodeIds;

    public TaskAverage(double average) {
        this.average = average;
        this.numberOfNodes = 1;
    }

    public TaskAverage(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        this.average = din.readDouble();
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

    public void processRelay(String id, int numberOfTasks) {
        double summed = this.average + numberOfTasks;
        this.average = summed / 2;
        this.nodeIds.add(id);
        this.numberOfNodes++;
    }

    public boolean nodeIsFirst(String id) {
        return id.equals(this.nodeIds.get(0));
    }

    public String getLastNode() {
        return this.nodeIds.get(this.numberOfNodes - 1);
    }

    public int getNumberOfNodes() {
        return this.numberOfNodes;
    }

    public List<String> getNodeIds() {
        return this.nodeIds;
    }

    public double getAverage() {
        return this.average;
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeDouble(this.average);
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