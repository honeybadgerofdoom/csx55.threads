package csx55.overlay.transport;

import csx55.overlay.node.MessagingNode;
import csx55.overlay.node.PartnerNodeRef;
import csx55.overlay.util.TrafficStats;
import csx55.overlay.wireformats.Message;

import java.util.*;

public class MessagePassingThread implements Runnable {

    private final MessagingNode node;
    private final int numberOfRounds;

    public MessagePassingThread(MessagingNode node, int numberOfRounds) {
        this.node = node;
        this.numberOfRounds = numberOfRounds;
    }

    @Override
    public void run() {
        Random rng = this.node.getRng();
        int numTasks = rng.nextInt(1001);
        for (int j = 0; j < this.numberOfRounds; j++) {
            // do stuff
        }
    }
    
}