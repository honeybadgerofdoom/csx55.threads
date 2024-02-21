package csx55.threads.wireformats;

import java.io.IOException;

public interface Event {
    
    public int getType();
    public byte[] getBytes() throws IOException;

}