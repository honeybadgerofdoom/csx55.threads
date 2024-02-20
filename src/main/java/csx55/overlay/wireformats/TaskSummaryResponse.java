package csx55.overlay.wireformats;

import csx55.overlay.util.TableHelper;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;

public class TaskSummaryResponse implements Event {

    private final int messageType = Protocol.TRAFFIC_SUMMARY;
    private final String ipAddress;
    private final int portNumber;
    private int generated, pushed, pulled, completed;
    private TableHelper tableHelper = new TableHelper(12, 4);


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

    public String formatTable() {
        String header = String.format("| %10s | %10s | %10s | %10s |", "Generated", "Pushed", "Pulled", "Completed");
        return this.tableHelper.formatTable(header, this.toString());
    }

    @Override
    public String toString() {
        return String.format("| %10d | %10d | %10d | %10d |", generated, pushed, pulled, completed);
    }

    public String formatRow(String id) {
        return String.format("| %-17s | %17d | %17d | %17d | %17d |", id, generated, pushed, pulled, completed);
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