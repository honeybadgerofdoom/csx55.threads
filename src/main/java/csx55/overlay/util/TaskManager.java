package csx55.overlay.util;

import csx55.overlay.node.MessagingNode;
import csx55.overlay.wireformats.TaskDelivery;

public class TaskManager {

    private int currentNumberOfTasks;
    private final int initialNumberOfTasks;
    private MessagingNode node;
    private double average;
    private int taskDiff;
    private boolean needsMoreTasks = false;
    private boolean averageUpdated = false;

    public TaskManager(MessagingNode node) {
        this.node = node;
        int randomNumberOfTasks = node.getRng().nextInt(1001);
        this.currentNumberOfTasks = randomNumberOfTasks;
        this.initialNumberOfTasks = randomNumberOfTasks;
    }

    public synchronized void handleTaskDelivery(TaskDelivery taskDelivery) {
        if (this.needsMoreTasks) {
            int tasksNeeded = this.taskDiff;
            int tasksTaken = taskDelivery.takeTasks(tasksNeeded);
            this.currentNumberOfTasks += tasksTaken;
            updateTaskDiff();
        }
    }

    public synchronized void giveTasks(int tasksToGive) {
        this.currentNumberOfTasks -= tasksToGive;
        updateTaskDiff();
    }

    public synchronized void absorbExcessTasks(int excessTasks) {
        this.currentNumberOfTasks += excessTasks;
        updateTaskDiff();
    }

    private void updateTaskDiff() {
        int flooredAverage = (int) Math.floor(this.average);
        this.taskDiff = Math.abs(flooredAverage - this.currentNumberOfTasks);
        if (flooredAverage - 1 > this.currentNumberOfTasks) {
            this.needsMoreTasks = true;
        }
        else {
            this.needsMoreTasks = false;
        }
    }

    public int getCurrentNumberOfTasks() {
        return currentNumberOfTasks;
    }

    public void setAverage(double average) {
        this.average = average;
        updateTaskDiff();
        averageUpdated = true;
    }

    public synchronized boolean averageIsSet() {
        return this.averageUpdated;
    }

    public synchronized boolean shouldGiveTasks() {
        return !this.needsMoreTasks;
    }

    public int getTaskDiff() {
        return this.taskDiff;
    }

    public int getInitialNumberOfTasks() {
        return this.initialNumberOfTasks;
    }

    @Override
    public String toString() {
        return "Tasks Held: " + this.currentNumberOfTasks + "\nAverage: " + (int) Math.floor(this.average);
    }

}