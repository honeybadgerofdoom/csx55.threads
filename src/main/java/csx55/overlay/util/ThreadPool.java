package csx55.overlay.util;

import csx55.overlay.hashing.Task;
import csx55.overlay.node.MessagingNode;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadPool {

    private ConcurrentLinkedQueue<Task> taskQueue;
    private final MessagingNode node;

    public ThreadPool(MessagingNode node) {
        this.node = node;
    }

    public void startThreadPool(int numberOfThreads) {
        System.out.println("Starting thread pool with " + numberOfThreads + " threads.");
        this.taskQueue = new ConcurrentLinkedQueue<>();
        ThreadPoolManager threadPoolManager = new ThreadPoolManager(this.node.getTrafficStats(), this, numberOfThreads, this.node.getIpAddress(), this.node.getPortNumber(), this.node.getSocketToRegistry());
        Thread thread = new Thread(threadPoolManager);
        thread.start();
    }

    public void addTasksToQueue(int numTasks, int round) {
        for (int i = 0; i < numTasks; i++) {
            Task task = new Task(this.node.getIpAddress(), this.node.getPortNumber(), round, this.node.getRng().nextInt());
            this.taskQueue.add(task);
        }
    }

    public ConcurrentLinkedQueue<Task> getTaskQueue() {
        return taskQueue;
    }

}
