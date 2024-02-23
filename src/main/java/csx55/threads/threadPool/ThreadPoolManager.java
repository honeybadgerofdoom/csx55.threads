package csx55.threads.threadPool;

import csx55.threads.task.TaskWorker;
import csx55.threads.transport.TCPSender;
import csx55.threads.util.ComputeNodeTaskStats;
import csx55.threads.wireformats.TaskSummaryResponse;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ThreadPoolManager implements Runnable {

    private final ComputeNodeTaskStats computeNodeTaskStats;
    private final ThreadPool threadPool;
    private final int numberOfThreads;
    private final String ipAddress;
    private final int portNumber;
    private final Socket socketToRegistry;

    public ThreadPoolManager(ComputeNodeTaskStats computeNodeTaskStats, ThreadPool threadPool, int numberOfThreads, String ipAddress, int portNumber, Socket socketToRegistry) {
        this.computeNodeTaskStats = computeNodeTaskStats;
        this.threadPool = threadPool;
        this.numberOfThreads = numberOfThreads;
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.socketToRegistry = socketToRegistry;
    }

    @Override
    public void run() {
        List<Thread> threads = new ArrayList<>();
        // Start threads
        for (int i = 0; i < numberOfThreads; i++) {
            TaskWorker taskWorker = new TaskWorker(this.threadPool, this.computeNodeTaskStats);
            Thread thread = new Thread(taskWorker);
            thread.start();
            threads.add(thread);
        }

        // Wait for threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Send TaskSummaryResponse
        System.out.println("All tasks complete, sending TaskSummaryResponse to registry.");
        TaskSummaryResponse taskSummaryResponse = new TaskSummaryResponse(this.ipAddress, this.portNumber, this.computeNodeTaskStats.getGenerated(), this.computeNodeTaskStats.getPushed(), this.computeNodeTaskStats.getPulled(), this.computeNodeTaskStats.getCompleted());
        try {
            TCPSender sender = new TCPSender(socketToRegistry);
            byte[] bytes = taskSummaryResponse.getBytes();
            sender.sendData(bytes);
            this.computeNodeTaskStats.reset();
        } catch (IOException e) {
            System.out.println("ERROR Sending traffic summary " + e);
        }
    }

}
