package csx55.overlay.util;

import csx55.overlay.wireformats.Event;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadPool {

    private ConcurrentLinkedQueue<Event> taskQueue;
    private int totalTasks = 0;

    public ThreadPool() {
    }

    public void startThreadPool(int numberOfThreads) {
        System.out.println("Starting thread pool with " + numberOfThreads + " threads.");
        this.taskQueue = new ConcurrentLinkedQueue<>();
        int numberOfWorkers = numberOfThreads;
        /*
        * TODO
        *  - Make the class that actually does the work, pass it to the ctor of new Thread();
        * */
        for (int i = 0; i < numberOfWorkers; i++) {
            Thread thread = new Thread();
            thread.start();
        }
    }

    private synchronized void updateTotalTasks(int numTasks) {
        this.totalTasks += numTasks;
        System.out.println("Starting " + numTasks + " tasks. Current total is " + this.totalTasks);
    }

    public void addTasksToQueue(int numTasks) {
        updateTotalTasks(numTasks);
        // ToDo Add the tasks to the clq
    }

    public ConcurrentLinkedQueue<Event> getTaskQueue() {
        return taskQueue;
    }

}
