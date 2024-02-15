package csx55.overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;

public class TaskReportResponse implements Event {

    private final int messageType = Protocol.TASK_REPORT_RESPONSE;
    private String id;
    private int numTasks;
    private int initialNumberOfTasks;

    public TaskReportResponse(String id, int numTasks, int initialNumberOfTasks) {
        this.id = id;
        this.numTasks = numTasks;
        this.initialNumberOfTasks = initialNumberOfTasks;
    }

    public TaskReportResponse(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        int idLength = din.readInt();
        byte[] idBytes = new byte[idLength];
        din.readFully(idBytes);
        this.id = new String(idBytes);

        this.numTasks = din.readInt();
        this.initialNumberOfTasks = din.readInt();

        bArrayInputStream.close();
        din.close();
    }

    public int getType() {
        return this.messageType;
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);

        byte[] ipAddressBytes = this.id.getBytes();
        int elementLength = ipAddressBytes.length;
        dout.writeInt(elementLength);
        dout.write(ipAddressBytes);

        dout.writeInt(this.numTasks);
        dout.writeInt(this.initialNumberOfTasks);

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    @Override
    public String toString() {
        return "--------------------\n" + this.id + "\nBefore Load Balancing: " + this.initialNumberOfTasks + "\nAfter Load Balancing: " + this.numTasks + "\n--------------------";
    }

}
