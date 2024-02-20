package csx55.overlay.node;

import csx55.overlay.cli.MessagingNodeCLIManager;
import csx55.overlay.transport.TaskProcessor;
import csx55.overlay.util.EventAndSocket;
import csx55.overlay.util.ThreadPool;
import csx55.overlay.util.TrafficStats;
import csx55.overlay.wireformats.*;
import csx55.overlay.transport.TCPReceiverThread;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.transport.MessagePassingThread;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MessagingNode implements Node {

    private ServerSocket serverSocket;

    private String ipAddress;
    private int portNumber;
    private String id;
    private final String registryIpAddress;
    private final int registryPortNumber;

    private TrafficStats trafficStats;

    private ConcurrentHashMap<String, PartnerNodeRef> partnerNodes; // FIXME Just 1 partner?
    private Socket socketToRegistry;
    private Random rng;
    private ThreadPool threadPool;
    private TaskProcessor taskProcessor;

    public MessagingNode(String registryIpAddress, int registryPortNumber) {
        this.registryIpAddress = registryIpAddress;
        this.registryPortNumber = registryPortNumber;
        this.partnerNodes = new ConcurrentHashMap<>();
    }

    public void doWork() {
        initializeTrafficStats();
        assignIpAddress();
        assignServerSocketAndPort();
        startTCPServerThread();
        connectToRegistry(this.registryIpAddress, this.registryPortNumber);
        registerSelf();
        setupThreadPool();
        manageCLI();
    }

    private void setupThreadPool() {
        this.threadPool = new ThreadPool(this);
    }

    private void initializeTrafficStats() {
        this.trafficStats = new TrafficStats();
    }

    private void assignIpAddress() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            this.ipAddress = addr.getHostName();
        } catch (UnknownHostException e) {
            System.out.println("ERROR Failed to get MessagingNode IP Address...\n" + e);
        }
    }

    private void assignServerSocketAndPort() {
        try {
            this.serverSocket = new ServerSocket(0);
            this.portNumber = this.serverSocket.getLocalPort();
            this.rng = new Random(this.portNumber);
            this.id = this.ipAddress + ":" + this.portNumber;
        } catch (IOException e) {
            System.out.println("ERROR Failed to create ServerSocket...\n" + e);
        }
    }

    private void connectToRegistry(String registryIpAddress, int registryPortNumber) {
        try {
            this.socketToRegistry = new Socket(registryIpAddress, registryPortNumber);
            TCPReceiverThread tcpReceiverThread = new TCPReceiverThread(this, this.socketToRegistry);
            Thread thread = new Thread(tcpReceiverThread);
            thread.start();
        } catch (IOException e) {
            System.out.println("ERROR Failed to connect to Registry " + e);
        }
    }

    private void registerSelf() {
        try {
            TCPSender tcpSender = new TCPSender(this.socketToRegistry);
            RegisterRequest registerRequest = new RegisterRequest(this.getIpAddress(), this.getPortNumber());
            byte[] bytes = registerRequest.getBytes();
            tcpSender.sendData(bytes);
        } catch (IOException e) {
            System.out.println("ERROR Trying to register self " + e);
        }
    }

    public void manageCLI() {
        MessagingNodeCLIManager cliManager = new MessagingNodeCLIManager(this);
        Thread thread = new Thread(cliManager);
        thread.start();
    }

    public Socket getSocketToRegistry() {
        return this.socketToRegistry;
    }

    public TrafficStats getTrafficStats() {
        return this.trafficStats;
    }

    public Map<String, PartnerNodeRef> getPartnerNodes () {
        return this.partnerNodes;
    }

    public Random getRng() {
        return this.rng;
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    public ConcurrentLinkedQueue<EventAndSocket> getEventQueue() {
//        return this.eventQueue;
        return null;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    public void addEventToThreadPool(Event event, Socket socket) {
//        this.eventQueue.add(new EventAndSocket(event, socket));
    }

    public ThreadPool getThreadPool() {
        return this.threadPool;
    }

    public String getId() {
        return this.id;
    }

//    public void setTaskManager(TaskManager taskManager) {
//        this.taskManager = taskManager;
//    }
//
//    public TaskManager getTaskManager() {
//        return this.taskManager;
//    }

    public void onEvent(Event event, Socket socket) {
        if (event != null) {
            int type = event.getType();
            switch (type) {
                case (Protocol.REGISTER_RESPONSE):
                    handleRegisterResponse(event);
                    break;
                case (Protocol.DEREGISTER_RESPONSE):
                    handleDeregisterResponse(event);
                    break;
                case (Protocol.MESSAGING_NODES_LIST):
                    handleMessagingNodesList(event);
                    break;
                case (Protocol.PARTNER_CONNECTION_REQUEST):
                    handlePartnerConnection(event, socket);
                    break;
                case (Protocol.TASK_INITIATE):
                    handleTaskInitiate(event);
                    break;
                case (Protocol.PULL_TRAFFIC_SUMMARY):
                    handleTrafficSummary();
                    break;
                case (Protocol.POKE):
                    handlePoke(event);
                    break;
                case (Protocol.TASK_AVERAGE):
                    handleTaskAverage(event);
                    break;
                case (Protocol.TASK_DELIVERY):
                    handleTaskDelivery(event);
                    break;
                case (Protocol.TASK_REPORT_REQUEST):
                    handleTaskReport(event);
                    break;
                case (Protocol.AVERAGES_CALCULATED):
                    handleAveragesCalculated(event);
                    break;
                default:
                    System.out.println("onEvent couldn't handle event type " + type);
            }
        }
    }

    private void handleRegisterResponse(Event event) {
        String registerResponseInfo = ((RegisterResponse) event).getAdditionalInfo();
        System.out.println(registerResponseInfo);
    }

    private void handleDeregisterResponse(Event event) {
        int statusCode = ((DeregisterResponse) event).getStatusCode();
        if (statusCode == Protocol.SUCCESS) {
            System.out.println("Exiting Overlay");
            System.exit(0);
        }
        else {
            System.out.println("Deregister Failed.");
        }
    }

    private void handleMessagingNodesList(Event event) {
        List<String> info = ((MessagingNodesList) event).getInfo();
        int numberOfThreads = ((MessagingNodesList) event).getNumberOfThreads();
        this.threadPool.startThreadPool(numberOfThreads);
        int numberOfConnections = 0;
        for (String nodeInfo : info) {
            String[] nodeInfoList = nodeInfo.split(":");
            String partnerIpAddress = nodeInfoList[0];
            int partnerPortNumber = Integer.parseInt(nodeInfoList[1]);
            try {
                Socket socket = new Socket(partnerIpAddress, partnerPortNumber);
                TCPReceiverThread receiver = new TCPReceiverThread(this, socket);
                Thread thread = new Thread(receiver);
                thread.start();
                PartnerNodeRef partnerNodeRef = new PartnerNodeRef(socket, 0);
                PartnerConnectionRequest partnerConnectionRequest = new PartnerConnectionRequest(this.ipAddress, this.portNumber, 0);
                partnerNodeRef.writeToSocket(partnerConnectionRequest);
                this.partnerNodes.put(nodeInfo, partnerNodeRef);
                numberOfConnections++;
            } catch (IOException e) {
                System.out.println("Failed to create Socket to partner " + nodeInfo);
            }
        }
        System.out.println("All connections established Number of connections: " + numberOfConnections);
    }

    private void handlePartnerConnection(Event event, Socket socket) {
        String ipAddress = ((PartnerConnectionRequest) event).getIpAddress();
        int port = ((PartnerConnectionRequest) event).getPortNumber();
        int linkWeight = ((PartnerConnectionRequest) event).getLinkWeight();
        String key = ipAddress + ":" + port;
        PartnerNodeRef partnerNodeRef = new PartnerNodeRef(socket, linkWeight);
        this.partnerNodes.put(key, partnerNodeRef);
    }

    private void handleTaskInitiate(Event event) {
        int numberOfRounds = ((TaskInitiate) event).getRounds();
        this.taskProcessor = new TaskProcessor(this, numberOfRounds);
        Thread thread = new Thread(this.taskProcessor);
        thread.start();
    }

    public PartnerNodeRef getOneNeighbor() {
        List<String> partnerIds = new ArrayList<>(this.partnerNodes.keySet());
        String id = partnerIds.get(0);
        return this.partnerNodes.get(id);
    }

    public void reportAllMessagesPassed() {
        TaskComplete taskComplete = new TaskComplete(this.getIpAddress(), this.getPortNumber());
        try {
            byte[] bytes = taskComplete.getBytes();
            TCPSender sender = new TCPSender(this.getSocketToRegistry());
            sender.sendData(bytes);
        } catch (IOException e) {
            System.out.println("Failed to send TaskComplete message to Registry." + e);
        }
    }

    public void printTaskManagerSum() {
        this.taskProcessor.printTaskManagerStats();
    }

    private void handleTrafficSummary() {
//        TaskSummaryResponse taskSummaryResponse = new TaskSummaryResponse(
//                this.ipAddress,
//                this.portNumber,
//                this.trafficStats.getSendTracker(),
//                this.trafficStats.getSendSummation(),
//                this.trafficStats.getReceiveTracker(),
//                this.trafficStats.getReceiveSummation(),
//                this.trafficStats.getRelayTracker());
//        try {
//            byte[] bytes = taskSummaryResponse.getBytes();
//            TCPSender sender = new TCPSender(this.socketToRegistry);
//            sender.sendData(bytes);
//            this.trafficStats.reset();
//        } catch (IOException e) {
//            System.out.println("Failed to send TaskSummaryResponse " + e);
//        }
    }

    private void handlePoke(Event event) {
        String message = ((PartnerPoke) event).getMessage();
        System.out.println("Received Poke: " + message);
    }

    private void handleTaskAverage(Event event) {
        TaskAverage taskAverage = (TaskAverage) event;
        this.taskProcessor.handleTaskAverage(taskAverage);
    }

    private void handleTaskDelivery(Event event) {
        TaskDelivery taskDelivery = (TaskDelivery) event;
        this.taskProcessor.handleTaskDelivery(taskDelivery);
    }

    private void handleTaskReport(Event event) {
//        TaskReportResponse taskReportResponse = new TaskReportResponse(this.id, this.taskManager.getCurrentNumberOfTasks(), this.taskManager.getInitialNumberOfTasks());
//        try {
//            TCPSender sender = new TCPSender(socketToRegistry);
//            byte[] bytes = taskReportResponse.getBytes();
//            sender.sendData(bytes);
//        } catch (IOException e) {
//            System.out.println("Failed to send TaskReportResponse " + e);
//        }
    }

    private void handleAveragesCalculated(Event event) {
        AveragesCalculated averagesCalculated = (AveragesCalculated) event;
        this.taskProcessor.handleAveragesCalculated(averagesCalculated);
    }

    private void sendMessages(int numberOfRounds) {
        MessagePassingThread messagePassingThread = new MessagePassingThread(this, numberOfRounds);
        Thread thread = new Thread(messagePassingThread);
        thread.start();
    }

    public void listPartners() {
        System.out.println(getPartnerNodesString());
    }

    private String getPartnerNodesString() {
        String str = "{\n";
        for (String key : this.partnerNodes.keySet()) {
            PartnerNodeRef partnerNodeRef = this.partnerNodes.get(key);
            str += "\t" + this.ipAddress + ":" + this.portNumber + " -- " + partnerNodeRef.getLinkWeight() + " --> " + key + ", " + partnerNodeRef.getSocket() + "\n";
        }
        str += "}";
        return str;
    }

    @Override
    public String toString() {
        return "\nMessagingNode\n---------------------\n" + this.getIpAddress() + ":" + this.getPortNumber() + "\nPartner Nods: " + getPartnerNodesString();
    }

    public void deregisterSelf() {
        try {
            TCPSender tcpSender = new TCPSender(this.socketToRegistry);
            DeregisterRequest deregisterRequest = new DeregisterRequest(this.getIpAddress(), this.getPortNumber());
            byte[] bytes = deregisterRequest.getBytes();
            tcpSender.sendData(bytes);
        } catch (IOException e) {
            System.out.println("ERROR Trying to register self " + e);
        }
    }

    public void pokePartner(String partner) {
        try {
            Socket socket = this.partnerNodes.get(partner).getSocket();
            TCPSender sender = new TCPSender(socket);
            String message = "Hi from " + this.ipAddress + ":" + this.portNumber;
            PartnerPoke poke = new PartnerPoke(message);
            byte[] bytes = poke.getBytes();
            sender.sendData(bytes);
        } catch (IOException e) {
            System.out.println("ERROR Trying to poke partner " + e);
        } catch (NullPointerException e) {
            System.out.println("No socket to " + partner);
        }
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            String registryIpAddress = args[0];
            int registryPortNumber = Integer.parseInt(args[1]);
            MessagingNode node = new MessagingNode(registryIpAddress, registryPortNumber);
            node.doWork();
        }
        else {
            System.out.println("Invalid Usage. Provide IP/Port of Registry");
        }
    }

}