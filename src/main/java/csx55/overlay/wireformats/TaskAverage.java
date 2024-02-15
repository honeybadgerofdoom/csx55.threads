package csx55.overlay.wireformats;

import csx55.overlay.node.MessagingNode;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

public class TaskAverage implements Event {

    private int messageType = Protocol.TASK_AVERAGE;
    private int sum;
    private int numberOfNodes;
    private List<String> nodeIds;

    public TaskAverage(int numberOfTasks, String id) {
        this.sum = numberOfTasks;
        this.numberOfNodes = 1;
        this.nodeIds = new ArrayList<>();
        this.nodeIds.add(id);
    }

    public TaskAverage(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        this.sum = din.readInt();
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

    public String processRelay(String id, int numberOfTasks) {
        String lastNode = this.nodeIds.get(this.numberOfNodes - 1);
        this.sum += numberOfTasks;
        this.nodeIds.add(id);
        this.numberOfNodes++;
        return lastNode;
    }

    public boolean nodeIsFirst(String id) {
        return id.equals(this.nodeIds.get(0));
    }

    public int getNumberOfNodes() {
        return this.numberOfNodes;
    }

    public double getSum() {
        return this.sum;
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);
        dout.writeInt(this.sum);
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