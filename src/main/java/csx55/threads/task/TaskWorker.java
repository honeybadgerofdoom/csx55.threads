package csx55.threads.task;

import csx55.threads.hashing.Miner;
import csx55.threads.hashing.Task;
import csx55.threads.threadPool.ThreadPool;
import csx55.threads.util.ComputeNodeTaskStats;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskWorker implements Runnable {

    private final ThreadPool threadPool;
    private final ComputeNodeTaskStats computeNodeTaskStats;

    public TaskWorker(ThreadPool threadPool, ComputeNodeTaskStats computeNodeTaskStats) {
        this.threadPool = threadPool;
        this.computeNodeTaskStats = computeNodeTaskStats;
    }

    @Override
    public void run() {
        Miner miner = new Miner();
        ConcurrentLinkedQueue<Task> taskQueue = threadPool.getTaskQueue();

        while (taskQueue.isEmpty()) {
//            Thread.onSpinWait();
        }
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            if (task == null) continue;
            miner.mine(task);
            this.computeNodeTaskStats.incrementCompleted();
            System.out.println(task);
        }

    }

}
