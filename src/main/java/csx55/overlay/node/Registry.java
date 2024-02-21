package csx55.overlay.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import csx55.overlay.cli.RegistryCLIManager;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.util.EventAndSocket;
import csx55.overlay.util.OverlayCreator;
import csx55.overlay.wireformats.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Registry implements Node {

    private final int portNumber;
    private ServerSocket serverSocket;
    private final ConcurrentHashMap<String, Socket> registryNodes;
    private OverlayCreator overlayCreator;
    private ConcurrentLinkedQueue<EventAndSocket> eventQueue;
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

    public ConcurrentLinkedQueue<EventAndSocket> getEventQueue() {
        return this.eventQueue;
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    public ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    public void addEventToThreadPool(Event event, Socket socket) {
        this.eventQueue.add(new EventAndSocket(event, socket));
    };

    public void onEvent(Event event, Socket socket) {
        switch (event.getType()) {
            case (Protocol.REGISTER_REQUEST):
                handleRegisterRequest(event, socket);
                break;
            case (Protocol.DEREGISTER_REQUEST):
                handleDeregisterRequest(event, socket);
                break;
            case (Protocol.TASK_COMPLETE):
                handleTaskComplete();
                break;
            case (Protocol.TRAFFIC_SUMMARY):
                handleTrafficSummary(event);
                break;
            case (Protocol.TASK_REPORT):
                handleTaskReport(event);
                break;
            default:
                System.out.println("onEvent trying to process invalid evernt type: " + event.getType());
        }
    }

    private synchronized void handleRegisterRequest(Event event, Socket socket) {
        /*
         * TODO
         *  - 'When a registry receives a request, ... ensures the IP address in the message matches the address where the request originated.'
         *  - Throw an error if the above condition is not met
         * */
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
        /*
         * TODO
         *  - 'When a registry receives a request, ... ensures the IP address in the message matches the address where the request originated.'
         *  - Throw an error if the above condition is not met
         * */
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

    private synchronized void handleTaskComplete() {
        if (checkDoneNodesNumber()) {
            sendPullTrafficSummaryMessage();
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

    private void handleTaskReport(Event event) {
        TaskReport taskReport = (TaskReport) event;
        System.out.println(taskReport.getData());
    }

    private void printFinalOutput() {
        int totalGenerated = 0;
        int totalPulled = 0;
        int totalPushed = 0;
        int totalCompleted = 0;
        String horizontalTablePiece = "";
        int numDashes = 19;
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
        System.out.println(String.format("| %-17s | %17s | %17s | %17s | %17s | %17s |", "Node", "Generated", "Pushed", "Pulled", "Completed", "% of Tasks"));
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
        System.out.println(String.format("| %-17s | %17d | %17d | %17d | %17d | %17f |", "TOTAL", totalGenerated, totalPushed, totalPulled, totalCompleted, totalPercent));
        System.out.println(tableLine);
    }

    private void sendPullTrafficSummaryMessage() {
        int numSeconds = 15;
        System.out.println("All nodes done sending messages, waiting for " + numSeconds + "s to send TaskSummaryRequest...");
        try {
            Thread.sleep(numSeconds * 1000);
        } catch (InterruptedException e) {
            System.out.println("INTERRUPTED While waiting before sending PullTrafficSummary " + e);
        }
        TaskSummaryRequest taskSummaryRequest = new TaskSummaryRequest();
        System.out.println(numSeconds + "s elapsed, sending TaskSummaryRequest");
        try {
            byte[] bytes = taskSummaryRequest.getBytes();
            for (Socket socket : this.registryNodes.values()) {
                TCPSender sender = new TCPSender(socket);
                sender.sendData(bytes);
            }
        } catch (IOException e) {
            System.out.println("ERROR Trying to send PullTrafficSummary");
        }
    }

    public void setupOverlay(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        createOverlay();
    }

    private void createOverlay() {
        List<String> nodes = new ArrayList<>(this.registryNodes.keySet());
        this.overlayCreator = new OverlayCreator(nodes, this.numberOfThreads);
        this.overlayCreator.createOverlay();
        sendMessagingNodesListToNodes();
    }

    public void printOverlay() {
        this.overlayCreator.printOverlay();
    }

    public void printMatrix() {
        this.overlayCreator.printMatrix();
    }

    private void sendMessagingNodesListToNodes() {
        Map<String, MessagingNodesList> messagingNodesListMap = this.overlayCreator.overlayToMessagingNodesListMap();
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