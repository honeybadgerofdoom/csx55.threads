package csx55.overlay.transport;

import csx55.overlay.ComputeNode;

import java.util.*;

public class MessagePassingThread implements Runnable {

    private final ComputeNode node;
    private final int numberOfRounds;

    public MessagePassingThread(ComputeNode node, int numberOfRounds) {
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