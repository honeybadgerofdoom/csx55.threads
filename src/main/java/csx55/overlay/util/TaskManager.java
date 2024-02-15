package csx55.overlay.util;

import csx55.overlay.node.MessagingNode;
import csx55.overlay.wireformats.TaskDelivery;

public class TaskManager {

    private int currentNumberOfTasks;
    private MessagingNode node;
    private double average;
    private int taskDiff;
    private boolean needsMoreTasks = false;

    public TaskManager(MessagingNode node) {
        this.node = node;
        this.currentNumberOfTasks = node.getRng().nextInt(1001);
    }

    public synchronized void handleTaskDelivery(TaskDelivery taskDelivery) {
        if (this.needsMoreTasks) {
            int tasksNeeded = this.taskDiff;
            int tasksTaken = taskDelivery.takeTasks(tasksNeeded);
            this.currentNumberOfTasks += tasksTaken;
            updateTaskDiff();
        }
    }

    private void updateTaskDiff() {
        int flooredAverage = (int) Math.floor(this.average);
        this.taskDiff = Math.abs(flooredAverage - this.currentNumberOfTasks);
        if (flooredAverage - 1 > this.currentNumberOfTasks) {
            System.out.println("-" + taskDiff + " task deficit");
            this.needsMoreTasks = true;
        }
        else {
            System.out.println("+" + taskDiff + " task excess");
            this.needsMoreTasks = false;
        }
    }

    public int getCurrentNumberOfTasks() {
        return currentNumberOfTasks;
    }

    public void setAverage(double average) {
        this.average = average;
        updateTaskDiff();
    }

    public int getTaskDiff() {
        return this.taskDiff;
    }

    public boolean needsMoreTasks() {
        return this.needsMoreTasks;
    }

}