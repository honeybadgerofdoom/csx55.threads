package csx55.threads.task;

import csx55.threads.threadPool.ThreadPool;
import csx55.threads.util.ComputeNodeTaskStats;
import csx55.threads.wireformats.TaskDelivery;
import java.util.Random;

public class TaskManager {

    private int currentNumberOfTasks;
    private final int initialNumberOfTasks;
    private double average;
    private int taskDiff;
    private boolean needsMoreTasks = false;
    private boolean averageUpdated = false;
    private final ThreadPool threadPool;
    private final int round;
    private final ComputeNodeTaskStats computeNodeTaskStats;

    public TaskManager(Random rng, ThreadPool threadPool, int round, ComputeNodeTaskStats computeNodeTaskStats) {
        this.threadPool = threadPool;
        int randomNumberOfTasks = rng.nextInt(1001);
        computeNodeTaskStats.updateGenerated(randomNumberOfTasks);
        this.currentNumberOfTasks = randomNumberOfTasks;
        this.initialNumberOfTasks = randomNumberOfTasks;
        this.round = round;
        this.computeNodeTaskStats = computeNodeTaskStats;
    }

    public synchronized void startInitialTasks() {
        int tasksToPush = this.initialNumberOfTasks;
        if (tasksToPush > average) {
            tasksToPush = (int) Math.floor(this.average);
        }
        pushTasksToThreadPool(tasksToPush);
    }

    // We still need more tasks. Take what we can, start them.
    public synchronized void handleTaskDelivery(TaskDelivery taskDelivery) {
        if (this.needsMoreTasks) {
            int tasksTaken = taskDelivery.takeTasks(this.taskDiff);
            this.computeNodeTaskStats.updatePulled(tasksTaken);
            this.currentNumberOfTasks += tasksTaken;
            String nodeId = taskDelivery.getOriginNode();
            pushTasksToThreadPool(tasksTaken, nodeId);
            updateTaskDiff();
        }
    }

    // This means we started with an excess. Update our current total, start that many of tasks.
    public synchronized void giveTasks() {
        this.currentNumberOfTasks -= this.taskDiff;
        this.computeNodeTaskStats.updatePushed(this.taskDiff);
        updateTaskDiff();
    }

    // We gave tasks but there are some left. Start those.
    public synchronized void absorbExcessTasks(int excessTasks) {
        this.currentNumberOfTasks += excessTasks;
        this.computeNodeTaskStats.absorb(excessTasks);
        pushTasksToThreadPool(excessTasks);
        updateTaskDiff();
    }

    private synchronized void updateTaskDiff() {
        int flooredAverage = (int) Math.floor(this.average);
        this.taskDiff = Math.abs(flooredAverage - this.currentNumberOfTasks);
        if (flooredAverage > this.currentNumberOfTasks) {
            this.needsMoreTasks = true;
        }
        else {
            this.needsMoreTasks = false;
        }
    }

    private synchronized void pushTasksToThreadPool(int numTasks) {
        this.threadPool.addTasksToQueue(numTasks, this.round);
    }

    private synchronized void pushTasksToThreadPool(int numTasks, String nodeId) {
        this.threadPool.addTasksToQueue(numTasks, this.round, nodeId);
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

    public synchronized int getInitialNumberOfTasks() {
        return this.initialNumberOfTasks;
    }

    @Override
    public String toString() {
        return "Tasks Held: " + this.currentNumberOfTasks + "\nAverage: " + (int) Math.floor(this.average);
    }

}