package csx55.overlay.util;

import csx55.overlay.hashing.Task;
import csx55.overlay.node.MessagingNode;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.wireformats.TaskReport;

import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadPool {

    private ConcurrentLinkedQueue<Task> taskQueue;
    private final MessagingNode node;
    private TCPSender sender;

    public ThreadPool(MessagingNode node) {
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

    public void addTasksToQueue(int numTasks, int round) {
        for (int i = 0; i < numTasks; i++) {
            Task task = new Task(this.node.getIpAddress(), this.node.getPortNumber(), round, this.node.getRng().nextInt());
            this.taskQueue.add(task);
        }
    }

    public synchronized void sendTaskDataToRegistry(Task task) {
        String data = task.toString();
        TaskReport taskReport = new TaskReport(data);
        try {
            byte[] bytes = taskReport.getBytes();
            sender.sendData(bytes);
        } catch (IOException e) {
            System.out.println("Failed to send task data " + e);
        }
    }

    public ConcurrentLinkedQueue<Task> getTaskQueue() {
        return taskQueue;
    }

}
