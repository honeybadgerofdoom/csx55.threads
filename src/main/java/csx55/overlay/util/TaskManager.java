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
    private ThreadPool threadPool;
    private int round;
    private TrafficStats trafficStats;

    public TaskManager(Random rng, ThreadPool threadPool, int round, TrafficStats trafficStats) {
        this.threadPool = threadPool;
        int randomNumberOfTasks = rng.nextInt(1001);
        trafficStats.updateGenerated(randomNumberOfTasks);
        this.currentNumberOfTasks = randomNumberOfTasks;
        this.initialNumberOfTasks = randomNumberOfTasks;
        this.round = round;
        this.trafficStats = trafficStats;
    }

    // We started with a deficit. Go ahead & start the ones we have.
    public synchronized void startInitialTasks() {
        if (this.initialNumberOfTasks < average) {
            pushTasksToThreadPool(initialNumberOfTasks);
        }
    }

    // We still need more tasks. Take what we can, start them.
    public synchronized void handleTaskDelivery(TaskDelivery taskDelivery) {
        if (this.needsMoreTasks) {
            int tasksNeeded = this.taskDiff;
            int tasksTaken = taskDelivery.takeTasks(tasksNeeded);
            pushTasksToThreadPool(tasksTaken);
            this.trafficStats.updatePulled(tasksTaken);
            this.currentNumberOfTasks += tasksTaken;
            updateTaskDiff();
        }
    }

    // This means we started with an excess. Update our current total, start that many of tasks.
    public synchronized void giveTasks() {
        this.currentNumberOfTasks -= this.taskDiff;
        this.trafficStats.updatePushed(this.taskDiff);
        pushTasksToThreadPool(this.currentNumberOfTasks);
        updateTaskDiff();
    }

    // We gave tasks but there are some left. Start those.
    public synchronized void absorbExcessTasks(int excessTasks) {
        pushTasksToThreadPool(excessTasks);
        this.trafficStats.absorb(excessTasks);
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

    private void pushTasksToThreadPool(int numTasks) {
        this.threadPool.addTasksToQueue(numTasks, this.round);
    }

    public synchronized int getCurrentNumberOfTasks() {
        return currentNumberOfTasks;
    }

    public synchronized void setAverage(double average) {
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

    public double getAverage() {
        return this.average;
    }

    @Override
    public String toString() {
        return "Tasks Held: " + this.currentNumberOfTasks + "\nAverage: " + (int) Math.floor(this.average);
    }

}