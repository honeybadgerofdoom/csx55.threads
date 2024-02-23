package csx55.threads.util;

import csx55.threads.hashing.Task;
import csx55.threads.ComputeNode;
import csx55.threads.transport.TCPSender;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadPool {

    private ConcurrentLinkedQueue<Task> taskQueue;
    private final ComputeNode node;
    private TCPSender sender;

    public ThreadPool(ComputeNode node) {
        this.node = node;
        try {
            this.sender = new TCPSender(node.getSocketToRegistry());
        } catch (IOException e) {
            System.out.println("Failed to build TCPSender to registry.");
        }
    }

    public void startThreadPool() {
        System.out.println("Starting thread pool with " + this.node.getNumberOfThreads() + " threads.");
        this.taskQueue = new ConcurrentLinkedQueue<>();
        ThreadPoolManager threadPoolManager = new ThreadPoolManager(this.node.getTrafficStats(), this, this.node.getNumberOfThreads(), this.node.getIpAddress(), this.node.getPortNumber(), this.node.getSocketToRegistry());
        Thread thread = new Thread(threadPoolManager);
        thread.start();
    }

    public void addTasksToQueue(int numTasks, int round, String nodeId) {
        String[] id = nodeId.split(":");
        int port = Integer.parseInt(id[1]);
        String ip = id[0];
        for (int i = 0; i < numTasks; i++) {
            Task task = new Task(ip, port, round, this.node.getRng().nextInt());
            this.taskQueue.add(task);
        }
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
