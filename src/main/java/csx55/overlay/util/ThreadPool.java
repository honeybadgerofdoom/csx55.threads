package csx55.overlay.util;

import csx55.overlay.hashing.Miner;
import csx55.overlay.hashing.Task;
import csx55.overlay.wireformats.Event;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadPool {

    private ConcurrentLinkedQueue<Task> taskQueue;
    private Integer totalTasksReceived = 0;
    private String ipAddress;
    private int portNumber;
    private Random rng;
    private Integer tasksCompleted = 0;

    public ThreadPool(String ipAddress, int portNumber, Random rng) {
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.rng = rng;
    }

    public void startThreadPool(int numberOfThreads) {
        System.out.println("Starting thread pool with " + numberOfThreads + " threads.");
        this.taskQueue = new ConcurrentLinkedQueue<>();
        int numberOfWorkers = numberOfThreads;
        for (int i = 0; i < numberOfWorkers; i++) {
            TaskWorker taskWorker = new TaskWorker(this);
            Thread thread = new Thread(taskWorker);
            thread.start();
        }
    }

    public void incrementTasksCompleted() {
        synchronized (tasksCompleted) {
            this.tasksCompleted++;
        }
    }

    public int getTasksCompleted() {
        return this.tasksCompleted;
    }

    private void updateTotalTasksReceived(int numTasks) {
        synchronized (totalTasksReceived) {
            this.totalTasksReceived += numTasks;
        }
    }

    public void addTasksToQueue(int numTasks, int round) {
        updateTotalTasksReceived(numTasks);
        // FIXME What is the payload??
        Task task = new Task(this.ipAddress, this.portNumber, round, rng.nextInt());
        this.taskQueue.add(task);
    }

    public ConcurrentLinkedQueue<Task> getTaskQueue() {
        return taskQueue;
    }

}
