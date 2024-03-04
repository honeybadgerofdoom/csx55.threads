package csx55.threads.wireformats;

import csx55.threads.hashing.Task;

import java.util.List;
import java.util.ArrayList;
import java.io.*;

public class TaskDelivery implements Event {

    private int messageType = Protocol.TASK_DELIVERY;
    private int numTasks;
    private int numberOfNodes;
    private List<String> nodeIds;
    private int iteration;
    private List<Task> taskList;

    public TaskDelivery(int numberOfTasks, String id, int iteration, List<Task> taskList) {
        this.numTasks = numberOfTasks;
        this.numberOfNodes = 1;
        this.nodeIds = new ArrayList<>();
        this.nodeIds.add(id);
        this.iteration = iteration;
        this.taskList = taskList;
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

        this.taskList = new ArrayList<>();
        for (int i = 0; i < this.numTasks; i++) {
            int currentTaskLength = din.readInt();
            byte[] currentTaskBytes = new byte[currentTaskLength];
            din.readFully(currentTaskBytes);
            this.taskList.add(new Task(currentTaskBytes));
        }

        bArrayInputStream.close();
        din.close();
    }

    public String getOriginNode() {
        return this.nodeIds.get(0);
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
            int tasksToGive = this.numTasks;
            this.numTasks = 0;
            return tasksToGive;
        }
        else {
            this.numTasks -= tasksToTake;
            return tasksToTake;
        }
    }

    public synchronized List<Task> receiveTasks(int tasksTaken) {
        List<Task> takenTasks = this.taskList.subList(0, tasksTaken);
        if (tasksTaken < this.taskList.size()) this.taskList = this.taskList.subList(tasksTaken, this.taskList.size());
        else this.taskList = new ArrayList<>();
        return takenTasks;
    }

    public synchronized List<Task> absorbAll() {
        this.numTasks = 0;
        List<Task> tasksToReturn = new ArrayList<>(this.taskList);
        this.taskList = new ArrayList<>();
        return tasksToReturn;
    }

    public boolean nodeIsFirst(String id) {
        return id.equals(this.nodeIds.get(0));
    }

    public synchronized int getNumTasks() {
        return this.numTasks;
    }

    public int getIteration() {
        return this.iteration;
    }

    public List<Task> getTaskList() {
        return taskList;
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
