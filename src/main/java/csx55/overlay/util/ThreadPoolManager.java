package csx55.overlay.util;

import csx55.overlay.transport.TCPSender;
import csx55.overlay.wireformats.TaskSummaryResponse;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ThreadPoolManager implements Runnable {

    private final TrafficStats trafficStats;
    private final ThreadPool threadPool;
    private final int numberOfThreads;
    private final String ipAddress;
    private final int portNumber;
    private final Socket socketToRegistry;

    public ThreadPoolManager(TrafficStats trafficStats, ThreadPool threadPool, int numberOfThreads, String ipAddress, int portNumber, Socket socketToRegistry) {
        this.trafficStats = trafficStats;
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
            TaskWorker taskWorker = new TaskWorker(this.threadPool, this.trafficStats);
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
        TaskSummaryResponse taskSummaryResponse = new TaskSummaryResponse(ipAddress, portNumber, this.trafficStats.getGenerated(), this.trafficStats.getPushed(), this.trafficStats.getPulled(), this.trafficStats.getCompleted());
        try {
            TCPSender sender = new TCPSender(socketToRegistry);
            byte[] bytes = taskSummaryResponse.getBytes();
            sender.sendData(bytes);
        } catch (IOException e) {
            System.out.println("ERROR Sending traffic summary " + e);
        }
    }

}
