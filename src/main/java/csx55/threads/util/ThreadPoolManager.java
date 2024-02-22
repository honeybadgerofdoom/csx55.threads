package csx55.threads.util;

import csx55.threads.transport.TCPSender;
import csx55.threads.wireformats.TaskSummaryResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ThreadPoolManager implements Runnable {

    private final TrafficStats trafficStats;
    private final ThreadPool threadPool;
    private final int numberOfThreads;
    private final String ipAddress;
    private final int portNumber;
    private final Socket socketToRegistry;
    private String ipNumeric;

    public ThreadPoolManager(TrafficStats trafficStats, ThreadPool threadPool, int numberOfThreads, String ipAddress, int portNumber, Socket socketToRegistry) {
        this.trafficStats = trafficStats;
        this.threadPool = threadPool;
        this.numberOfThreads = numberOfThreads;
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.socketToRegistry = socketToRegistry;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            this.ipNumeric = addr.getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("Failed to get host " + e);
        }
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
        System.out.println("All tasks complete, sending TaskSummaryResponse to registry.");
        TaskSummaryResponse taskSummaryResponse = new TaskSummaryResponse(ipNumeric, portNumber, this.trafficStats.getGenerated(), this.trafficStats.getPushed(), this.trafficStats.getPulled(), this.trafficStats.getCompleted());
        try {
            TCPSender sender = new TCPSender(socketToRegistry);
            byte[] bytes = taskSummaryResponse.getBytes();
            sender.sendData(bytes);
            this.trafficStats.reset();
        } catch (IOException e) {
            System.out.println("ERROR Sending traffic summary " + e);
        }
    }

}
