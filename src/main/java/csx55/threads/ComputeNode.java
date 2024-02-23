package csx55.threads;

import csx55.threads.cli.ComputeNodeCLIManager;
import csx55.threads.node.Node;
import csx55.threads.node.PartnerNodeRef;
import csx55.threads.task.TaskProcessor;
import csx55.threads.threadPool.ThreadPool;
import csx55.threads.util.ComputeNodeTaskStats;
import csx55.threads.wireformats.*;
import csx55.threads.transport.TCPReceiverThread;
import csx55.threads.transport.TCPSender;

import java.net.*;
import java.io.*;
import java.util.*;


public class ComputeNode implements Node {

    private ServerSocket serverSocket;

    private String ipAddress;
    private int portNumber;
    private String id;
    private final String registryIpAddress;
    private final int registryPortNumber;

    private ComputeNodeTaskStats computeNodeTaskStats;

    private PartnerNodeRef partnerNode;

    private Socket socketToRegistry;
    private Random rng;
    private ThreadPool threadPool;
    private TaskProcessor taskProcessor;
    private int numberOfThreads;

    public ComputeNode(String registryIpAddress, int registryPortNumber) {
        this.registryIpAddress = registryIpAddress;
        this.registryPortNumber = registryPortNumber;
    }

    public void doWork() {
        assignIpAddress();
        assignServerSocketAndPort();
        startTCPServerThread();
        connectToRegistry(this.registryIpAddress, this.registryPortNumber);
        registerSelf();
        manageCLI();
    }

    private void setupThreadPool() {
        this.threadPool = new ThreadPool(this);
    }

    private void initializeTrafficStats() {
        this.computeNodeTaskStats = new ComputeNodeTaskStats();
    }

    public int getNumberOfThreads() {
        return this.numberOfThreads;
    }

    private void assignIpAddress() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            this.ipAddress = addr.getHostAddress();
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
        ComputeNodeCLIManager cliManager = new ComputeNodeCLIManager(this);
        Thread thread = new Thread(cliManager);
        thread.start();
    }

    public Socket getSocketToRegistry() {
        return this.socketToRegistry;
    }

    public ComputeNodeTaskStats getTrafficStats() {
        return this.computeNodeTaskStats;
    }

    public Random getRng() {
        return this.rng;
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    public ThreadPool getThreadPool() {
        return this.threadPool;
    }

    public String getId() {
        return this.id;
    }

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
                    handlePartnerNodeInfo(event);
                    break;
                case (Protocol.TASK_INITIATE):
                    handleTaskInitiate(event);
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

    private void handlePartnerNodeInfo(Event event) {
        String partnerNode = ((PartnerNodeInfo) event).getPartnerNode();
        this.numberOfThreads = ((PartnerNodeInfo) event).getNumberOfThreads();
        String[] nodeInfoList = partnerNode.split(":");
        String partnerIpAddress = nodeInfoList[0];
        int partnerPortNumber = Integer.parseInt(nodeInfoList[1]);
        try {
            Socket socket = new Socket(partnerIpAddress, partnerPortNumber);
            this.partnerNode = new PartnerNodeRef(socket);
        } catch (IOException e) {
            System.out.println("Failed to create Socket to partner " + partnerNode);
        }
    }

    private void handleTaskInitiate(Event event) {
        initializeTrafficStats();
        setupThreadPool();
        this.threadPool.startThreadPool();
        int numberOfRounds = ((TaskInitiate) event).getRounds();
        this.taskProcessor = new TaskProcessor(this, numberOfRounds);
        Thread thread = new Thread(this.taskProcessor);
        thread.start();
    }

    public PartnerNodeRef getPartnerNode() {
        return this.partnerNode;
    }

    public void printTaskManagerSum() {
        this.taskProcessor.printTaskManagerStats();
    }

    private void handlePoke(Event event) {
        String message = ((PartnerPoke) event).getMessage();
        System.out.println("Received Poke: " + message);
    }

    private void handleTaskAverage(Event event) {
        RoundAverage roundAverage = (RoundAverage) event;
        this.taskProcessor.handleTaskAverage(roundAverage);
    }

    private void handleTaskDelivery(Event event) {
        TaskDelivery taskDelivery = (TaskDelivery) event;
        this.taskProcessor.handleTaskDelivery(taskDelivery);
    }

    private void handleAveragesCalculated(Event event) {
        AveragesCalculated averagesCalculated = (AveragesCalculated) event;
        this.taskProcessor.handleAveragesCalculated(averagesCalculated);
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

    public void pokePartner() {
        String message = "Hi from " + this.ipAddress + ":" + this.portNumber;
        PartnerPoke poke = new PartnerPoke(message);
        this.partnerNode.writeToSocket(poke);
    }

    @Override
    public String toString() {
        return "\nMessagingNode\n---------------------\n" + this.getIpAddress() + ":" + this.getPortNumber() + "\nPartner Node: " + this.partnerNode;
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            String registryIpAddress = args[0];
            int registryPortNumber = Integer.parseInt(args[1]);
            ComputeNode node = new ComputeNode(registryIpAddress, registryPortNumber);
            node.doWork();
        }
        else {
            System.out.println("Invalid Usage. Provide IP/Port of Registry");
        }
    }

}