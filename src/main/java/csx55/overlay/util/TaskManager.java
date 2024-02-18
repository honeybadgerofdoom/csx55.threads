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

    public TaskManager(Random rng) {
        int randomNumberOfTasks = rng.nextInt(1001);
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
        this.balanced = true;
    }

    private void updateTaskDiff() {
        int flooredAverage = (int) Math.floor(this.average);
        this.taskDiff = Math.abs(flooredAverage - this.currentNumberOfTasks);
        if (flooredAverage - 1 > this.currentNumberOfTasks) {
            this.needsMoreTasks = true;
//            System.out.println("-" + this.taskDiff + " task deficit");
        }
        else {
            this.needsMoreTasks = false;
//            System.out.println("+" + this.taskDiff + " task excess");
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

    public boolean isBalanced() {
        return this.balanced;
    }

    @Override
    public String toString() {
        return "Tasks Held: " + this.currentNumberOfTasks + "\nAverage: " + (int) Math.floor(this.average);
    }

}