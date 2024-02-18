package csx55.overlay.util;

import csx55.overlay.wireformats.TaskDelivery;
import java.util.Random;

public class TaskManager {

    private int currentNumberOfTasks;
    private final int initialNumberOfTasks;
    private double average;
    private int taskDiff;
    private boolean needsMoreTasks = false;
    private boolean averageUpdated = false;
    private boolean balanced = false;
    private ThreadPool threadPool;

    public TaskManager(Random rng, ThreadPool threadPool) {
        this.threadPool = threadPool;
        int randomNumberOfTasks = rng.nextInt(1001);
        this.currentNumberOfTasks = randomNumberOfTasks;
        this.initialNumberOfTasks = randomNumberOfTasks;
    }

    // We started with a deficit. Go ahead & start the ones we have.
    public void startInitialTasks() {
        if (this.initialNumberOfTasks < average) {
            System.out.println("Began with a deficit. Starting all " + this.initialNumberOfTasks + " we have.");
            this.threadPool.addTasksToQueue(initialNumberOfTasks);
        }
    }

    // We still need more tasks. Take what we can, start them.
    public synchronized void handleTaskDelivery(TaskDelivery taskDelivery) {
        if (this.needsMoreTasks) {
            int tasksNeeded = this.taskDiff;
            int tasksTaken = taskDelivery.takeTasks(tasksNeeded);
            System.out.println("Received " + tasksTaken + " tasks from a TaskDelivery.");
            this.threadPool.addTasksToQueue(tasksTaken);
            this.currentNumberOfTasks += tasksTaken;
            updateTaskDiff();
        }
    }

    // This means we started with an excess. Update our current total, start that many of tasks.
    public synchronized void giveTasks() {
        this.currentNumberOfTasks -= this.taskDiff;
        System.out.println("Start with a excess. Put " + this.taskDiff + " into a TaskDelivery message, starting " + this.currentNumberOfTasks + " tasks.");
        this.threadPool.addTasksToQueue(this.currentNumberOfTasks);
        updateTaskDiff();
    }

    // We gave tasks but there are some left. Start those.
    public synchronized void absorbExcessTasks(int excessTasks) {
        System.out.println("Received " + excessTasks + " excess tasks from a TaskDelivery message. Starting them.");
        this.threadPool.addTasksToQueue(excessTasks);
        this.currentNumberOfTasks += excessTasks;
        updateTaskDiff();
        this.balanced = true;
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

    public synchronized int getCurrentNumberOfTasks() {
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

    public synchronized int getTaskDiff() {
        return this.taskDiff;
    }

    public int getInitialNumberOfTasks() {
        return this.initialNumberOfTasks;
    }

    public boolean isBalanced() {
        return this.balanced;
    }

    public double getAverage() {
        return this.average;
    }

    @Override
    public String toString() {
        return "Tasks Held: " + this.currentNumberOfTasks + "\nAverage: " + (int) Math.floor(this.average);
    }

}