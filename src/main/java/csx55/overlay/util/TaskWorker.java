package csx55.overlay.util;

import csx55.overlay.hashing.Miner;
import csx55.overlay.hashing.Task;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.wireformats.TaskReport;
import csx55.overlay.wireformats.TaskSummaryResponse;

import java.io.IOException;
import java.net.Socket;
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
//            this.threadPool.sendTaskDataToRegistry(task);
        }

    }

}
