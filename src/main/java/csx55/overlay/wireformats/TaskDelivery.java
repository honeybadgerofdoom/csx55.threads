package csx55.overlay.wireformats;

import java.util.List;
import java.util.ArrayList;
import java.io.*;

public class TaskDelivery implements Event {

    private int messageType = Protocol.TASK_DELIVERY;
    private int numTasks;
    private int numberOfNodes;
    private List<String> nodeIds;
    private int iteration;

    public TaskDelivery(int numberOfTasks, String id, int iteration) {
        this.numTasks = numberOfTasks;
        this.numberOfNodes = 1;
        this.nodeIds = new ArrayList<>();
        this.nodeIds.add(id);
        this.iteration = iteration;
    }

    public TaskDelivery(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        this.numTasks = din.readInt();
        this.numberOfNodes = din.readInt();

        this.nodeIds = new ArrayList<>();
        for (int i = 0; i < this.numberOfNodes; i++) {
            int currentInfoLength = din.readInt();
            byte[] currentInfoBytes = new byte[currentInfoLength];
            din.readFully(currentInfoBytes);
            this.nodeIds.add(new String(currentInfoBytes));
        }

        this.iteration = din.readInt();

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

    public synchronized int takeTasks(int tasksToTake) {
        if (tasksToTake > this.numTasks) {
            int tasksToGive =  this.numTasks;
            this.numTasks = 0;
            return tasksToGive;
        }
        else {
            this.numTasks -= tasksToTake;
            return tasksToTake;
        }
    }

    public boolean nodeIsFirst(String id) {
        return id.equals(this.nodeIds.get(0));
    }

    public int getNumberOfNodes() {
        return this.numberOfNodes;
    }

    public int getNumTasks() {
        return this.numTasks;
    }

    public int getIteration() {
        return this.iteration;
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);
        dout.writeInt(this.numTasks);
        dout.writeInt(this.numberOfNodes);

        for (String id : this.nodeIds) {
            byte[] idBytes = id.getBytes();
            int elementLength = idBytes.length;
            dout.writeInt(elementLength);
            dout.write(idBytes);
        }

        dout.writeInt(this.iteration);

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

}
