package csx55.threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import csx55.threads.cli.RegistryCLIManager;
import csx55.threads.node.Node;
import csx55.threads.transport.TCPSender;
import csx55.threads.wireformats.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Registry implements Node {

    private final int portNumber;
    private ServerSocket serverSocket;
    private final ConcurrentHashMap<String, Socket> registryNodes;
    private ConcurrentHashMap<String, TaskSummaryResponse> taskResponseMap;
    private int numberOfDoneNodes = 0;
    private int taskSummariesCollected = 0;
    private final ReentrantLock doneNodesLock = new ReentrantLock();
    private final ReentrantLock taskSummariesLock = new ReentrantLock();
    private int numberOfThreads;

    public Registry(int portNumber) {
        this.portNumber = portNumber;
        this.registryNodes = new ConcurrentHashMap<>();
    }

    public void doWork() {
        assignServerSocket();
        startTCPServerThread();
        manageCLI();
        initializeOutputMaps();
    }

    private boolean checkDoneNodesNumber() {
        try {
            this.doneNodesLock.lock();
            this.numberOfDoneNodes++;
        } catch (Exception ignored) {
        } finally {
            this.doneNodesLock.unlock();
        }
        return this.numberOfDoneNodes == registryNodes.size();
    }

    private boolean checkTaskSummariesNumber() {
        try {
            this.taskSummariesLock.lock();
            this.taskSummariesCollected++;
        } catch (Exception ignored) {
        } finally {
            this.taskSummariesLock.unlock();
        }
        return this.taskSummariesCollected == registryNodes.size();
    }

    private void assignServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.portNumber);
        } catch (IOException e) {
            System.out.println("ERROR Failed to create ServerSocket...\n" + e);
        }
    }

    public void manageCLI() {
        RegistryCLIManager cliManager = new RegistryCLIManager(this);
        Thread thread = new Thread(cliManager);
        thread.start();
    }

    private void initializeOutputMaps() {
        this.taskResponseMap = new ConcurrentHashMap<>();
        for (String key : this.registryNodes.keySet()) {
            this.taskResponseMap.put(key, null);
        }
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    public ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    public void onEvent(Event event, Socket socket) {
        switch (event.getType()) {
            case (Protocol.REGISTER_REQUEST):
                handleRegisterRequest(event, socket);
                break;
            case (Protocol.DEREGISTER_REQUEST):
                handleDeregisterRequest(event, socket);
                break;
            case (Protocol.TRAFFIC_SUMMARY):
                handleTrafficSummary(event);
                break;
            default:
                System.out.println("onEvent trying to process invalid event type: " + event.getType());
        }
    }

    private synchronized void handleRegisterRequest(Event event, Socket socket) {
        String additionalInfo = "Register request unsuccessful. MessagingNode is already in the registry.";
        byte statusCode = Protocol.FAILURE;
        String mapKey = ((RegisterRequest) event).getIpAddress() + ":" + ((RegisterRequest) event).getPortNumber();
        if (!this.registryNodes.containsKey(mapKey)) {
            this.registryNodes.put(mapKey, socket);
            statusCode = Protocol.SUCCESS;
            additionalInfo = "Register request successful. The number of messaging nodes currently constituting the overlay is (" + this.registryNodes.size() + ")";
        }
        RegisterResponse registerResponse = new RegisterResponse(statusCode, additionalInfo);
        try {
            TCPSender sender = new TCPSender(socket);
            byte[] bytes = registerResponse.getBytes();
            sender.sendData(bytes);
        } catch (IOException e) {
            System.out.println("Failed to create TCPSender in handleRegisterRequest");
        }
    }

    private synchronized void handleDeregisterRequest(Event event, Socket socket) {
        byte statusCode = Protocol.FAILURE;
        String mapKey = ((DeregisterRequest) event).getIpAddress() + ":" + ((DeregisterRequest) event).getPortNumber();
        if (this.registryNodes.containsKey(mapKey)) {
            statusCode = Protocol.SUCCESS;
            this.registryNodes.remove(mapKey);
        }
        DeregisterResponse deregisterResponse = new DeregisterResponse(statusCode);
        try {
            TCPSender sender = new TCPSender(socket);
            byte[] bytes = deregisterResponse.getBytes();
            sender.sendData(bytes);
        } catch (IOException e) {
            System.out.println("Failed to create TCPSender in handleDeregisterRequest.");
        }
    }

    private synchronized void handleTrafficSummary(Event event) {
        String ipAddress = ((TaskSummaryResponse) event).getIpAddress();
        int port = ((TaskSummaryResponse) event).getPortNumber();

        String key = ipAddress + ":" + port;
        this.taskResponseMap.put(key, ((TaskSummaryResponse) event));

        if (checkTaskSummariesNumber()) {
            printFinalOutput();
            this.numberOfDoneNodes = 0;
            this.taskSummariesCollected = 0;
        }
    }

    private void printFinalOutput() {
        int totalGenerated = 0;
        int totalPulled = 0;
        int totalPushed = 0;
        int totalCompleted = 0;
        String horizontalTablePiece = "";
        int numDashes = 23;
        for (int i = 0; i < numDashes; i++) {
            horizontalTablePiece += "-";
        }
        String tableCorner = "+";
        String tableLine = tableCorner;
        int numCols = 6;
        for (int i = 0; i < numCols; i++) {
            tableLine += horizontalTablePiece + tableCorner;
        }
        System.out.println(tableLine);
        System.out.println(String.format("| %-21s | %21s | %21s | %21s | %21s | %21s |", "Node", "Generated", "Pulled", "Pushed", "Completed", "% of Tasks"));
        System.out.println(tableLine);

        for (String key : this.taskResponseMap.keySet()) {
            TaskSummaryResponse response = this.taskResponseMap.get(key);
            int completed = response.getCompleted();
            totalCompleted += completed;
        }

        double totalPercent = 0.0;

        for (String key : this.taskResponseMap.keySet()) {
            TaskSummaryResponse response = this.taskResponseMap.get(key);
            String ipAddress = response.getIpAddress();
            int portNumber = response.getPortNumber();
            int generated = response.getGenerated();
            int pushed = response.getPushed();
            int pulled = response.getPulled();
            int completed = response.getCompleted();

            String id = ipAddress + ":" + portNumber; // FIXME This should just be a member in the message `id` instead of ip & port

            double percent = ((double) completed / totalCompleted) * 100;
            totalPercent += percent;
            System.out.println(response.formatRow(id, percent));

            totalGenerated += generated;
            totalPushed += pushed;
            totalPulled += pulled;
        }

        System.out.println(tableLine);
        System.out.println(String.format("| %-21s | %21d | %21d | %21d | %21d | %21f |", "TOTAL", totalGenerated, totalPulled, totalPushed, totalCompleted, totalPercent));
        System.out.println(tableLine);
    }

    public void setupOverlay(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        createOverlay();
    }

    private void createOverlay() {
        List<String> nodes = new ArrayList<>(this.registryNodes.keySet());
        Map<String, MessagingNodesList> messagingNodeMap = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            String key = nodes.get(i);
            List<String> partnerNodes = new ArrayList<>();
            if (i < nodes.size() - 1) {
                partnerNodes.add(nodes.get(i+1));
            }
            else {
                partnerNodes.add(nodes.get(0));
            }
            MessagingNodesList messagingNodesList = new MessagingNodesList(partnerNodes, this.numberOfThreads);
            messagingNodeMap.put(key, messagingNodesList);
        }
        sendMessagingNodesListToNodes(messagingNodeMap);
    }

    private void sendMessagingNodesListToNodes(Map<String, MessagingNodesList> messagingNodesListMap) {
        for (String key : messagingNodesListMap.keySet()) {
            Socket socket = this.registryNodes.get(key);
            MessagingNodesList messagingNodesList = messagingNodesListMap.get(key);
            try {
                TCPSender sender = new TCPSender(socket);
                byte[] bytes = messagingNodesList.getBytes();
                sender.sendData(bytes);
            } catch (IOException e) {
                System.out.println("Failed to build TCPSender for MessagingNodesList");
            }
        }
    }

    public void listMessagingNodes() {
        for (String key : this.registryNodes.keySet()) {
            System.out.println(key);
        }
    }

    public void initiateMessagePassing(int numberOfRounds) {
        TaskInitiate taskInitiateMessage = new TaskInitiate(numberOfRounds);
        sendToAllNodes(taskInitiateMessage);
    }

    private void sendToAllNodes(Event event) {
        try {
            byte[] bytes = event.getBytes();
            for (Socket socket : this.registryNodes.values()) {
                TCPSender sender = new TCPSender(socket);
                sender.sendData(bytes);
            }
        } catch (IOException e) {
            System.out.println("Failed to send TaskInitial message: " + e);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid usage. Please provide a Port Number for the Registry.");
        }
        else {
            int registryPortNumber = Integer.parseInt(args[0]);
            Registry node = new Registry(registryPortNumber);
            node.doWork();
        }
    }
    
}