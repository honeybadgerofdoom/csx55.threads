package csx55.threads.util;

import csx55.threads.hashing.Miner;
import csx55.threads.hashing.Task;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskWorker implements Runnable {

    private final ThreadPool threadPool;
    private final TrafficStats trafficStats;

    public TaskWorker(ThreadPool threadPool, TrafficStats trafficStats) {
        this.threadPool = threadPool;
        this.trafficStats = trafficStats;
    }

    @Override
    public void run() {
        Miner miner = new Miner();
        ConcurrentLinkedQueue<Task> taskQueue = threadPool.getTaskQueue();

        while (taskQueue.isEmpty()) {
            Thread.onSpinWait();
        }
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            if (task == null) continue;
            miner.mine(task);
            this.trafficStats.incrementCompleted();
            System.out.println(task);
        }

    }

}
