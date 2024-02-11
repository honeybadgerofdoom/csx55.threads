package csx55.overlay.transport;

import csx55.overlay.node.MessagingNode;
import csx55.overlay.node.PartnerNodeRef;
import csx55.overlay.util.TrafficStats;
import csx55.overlay.wireformats.Message;

import java.util.*;

public class MessagePassingThread implements Runnable {

    private final MessagingNode node;

    public MessagePassingThread(MessagingNode node) {
        this.node = node;
    }

    @Override
    public void run() {
        Random rng = this.node.getRng();
        int numTasks = rng.nextInt(1001);
        // Do stuff
    }
    
}