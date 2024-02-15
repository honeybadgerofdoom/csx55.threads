package csx55.overlay.util;

import csx55.overlay.node.MessagingNode;

public class TaskManager {

    private int initialNumberOfTasks;
    private int numberOfTasks;
    private MessagingNode node;

    public TaskManager(MessagingNode node) {
        this.node = node;
        this.initialNumberOfTasks = node.getRng().nextInt(1001);
    }

    public int getInitialNumberOfTasks() {
        return initialNumberOfTasks;
    }

    public int getNumberOfTasks() {
        return numberOfTasks;
    }

    public void setNumberOfTasks(int numberOfTasks) {
        this.numberOfTasks = numberOfTasks;
    }

}