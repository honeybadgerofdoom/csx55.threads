package csx55.overlay.util;

import csx55.overlay.wireformats.Event;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadPool {

    private ConcurrentLinkedQueue<Event> taskQueue;

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

    public ConcurrentLinkedQueue<Event> getTaskQueue() {
        return taskQueue;
    }

}
