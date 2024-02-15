package csx55.overlay.util;

import csx55.overlay.node.MessagingNode;

public class TaskManager {

    private double numberOfTasks;
    private MessagingNode node;

    public TaskManager(MessagingNode node) {
        this.node = node;
    }

    public double getNumberOfTasks() {
        return numberOfTasks;
    }

}