package csx55.overlay.util;

import csx55.overlay.hashing.Miner;
import csx55.overlay.hashing.Task;
import csx55.overlay.wireformats.TaskSummaryResponse;

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

        // This assumes that the taskQueue never empties until it's done. Each task takes a while, so this should work.
        while (taskQueue.isEmpty()) {
            Thread.onSpinWait();
        }
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            miner.mine(task);
            this.trafficStats.incrementCompleted();
        }

    }

}
