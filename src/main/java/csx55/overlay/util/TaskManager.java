package csx55.overlay.util;

import csx55.overlay.node.MessagingNode;

public class TaskManager {

    private int currentNumberOfTasks;
    private int numberOfTasks;
    private MessagingNode node;
    private double average;

    public TaskManager(MessagingNode node) {
        this.node = node;
        this.currentNumberOfTasks = node.getRng().nextInt(1001);
    }

    public int getCurrentNumberOfTasks() {
        return currentNumberOfTasks;
    }

    public int getNumberOfTasks() {
        return numberOfTasks;
    }

    public void setAverage(double average) {
        this.average = average;
        int taskDiff = Math.abs((int) Math.floor(average) - currentNumberOfTasks);
        if (this.average > currentNumberOfTasks) {
            System.out.println("+" + taskDiff + " task excess");
        }
        else {
            System.out.println("-" + taskDiff + " task deficit");
        }
    }

}