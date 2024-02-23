package csx55.threads.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.text.DecimalFormat;

public class TaskSummaryResponse implements Event {

    private final int messageType = Protocol.TRAFFIC_SUMMARY;
    private final String ipAddress;
    private final int portNumber;
    private final int generated, pushed, pulled, completed;
    private final DecimalFormat df = new DecimalFormat("#.##########");

    public TaskSummaryResponse(String ipAddress, int portNumber, int generated, int pushed, int pulled, int completed) {
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.generated = generated;
        this.pushed = pushed;
        this.pulled = pulled;
        this.completed = completed;
    }

    public TaskSummaryResponse(byte[] bytes) throws IOException {
        ByteArrayInputStream bArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream din = new DataInputStream(new BufferedInputStream(bArrayInputStream));

        din.readInt(); // take messageType out of stream

        int idAddressLength = din.readInt();
        byte[] ipAddressBytes = new byte[idAddressLength]; 
        din.readFully(ipAddressBytes);
        this.ipAddress = new String(ipAddressBytes);
        
        this.portNumber = din.readInt();
        this.generated = din.readInt();
        this.pushed = din.readInt();
        this.pulled = din.readInt();
        this.completed = din.readInt();

        bArrayInputStream.close();
        din.close();
    }

    @Override
    public String toString() {
        return String.format("| %10d | %10d | %10d | %10d |", generated, pulled, pushed, completed);
    }

    public String formatRow(String id, double percentCompleted) {
        double formatted = Double.parseDouble(df.format(percentCompleted));
        return String.format("| %-21s | %21d | %21d | %21d | %21d | %21f |", id, generated, pulled, pushed, completed, formatted);
    }

    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream(); 
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        dout.writeInt(this.messageType);

        byte[] ipAddressBytes = this.ipAddress.getBytes();
        int elementLength = ipAddressBytes.length;
        dout.writeInt(elementLength);
        dout.write(ipAddressBytes);

        dout.writeInt(this.portNumber);
        dout.writeInt(this.generated);
        dout.writeInt(this.pushed);
        dout.writeInt(this.pulled);
        dout.writeInt(this.completed);

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    public int getType() {
        return this.messageType;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    public int getGenerated() {
        return generated;
    }

    public int getPushed() {
        return pushed;
    }

    public int getPulled() {
        return pulled;
    }

    public int getCompleted() {
        return completed;
    }

}