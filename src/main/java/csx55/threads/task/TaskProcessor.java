package csx55.threads.task;

import csx55.threads.ComputeNode;
import csx55.threads.node.PartnerNodeRef;
import csx55.threads.util.AgreementSpace;
import csx55.threads.wireformats.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskProcessor implements Runnable {

    private final ComputeNode node;
    private int numberOfRounds;
    private List<TaskManager> taskManagerList;
    private final ConcurrentLinkedQueue<NodeAgreement> nodeAgreementDeque;
    private final AgreementSpace roundsAgreementSpace;
    private final AgreementSpace taskManagerAgreementSpace;
    private final AgreementSpace averagesAgreementSpace;

    public TaskProcessor(ComputeNode node) {
        System.out.println("TaskProcessor is instantiated.");
        this.node = node;
        this.nodeAgreementDeque = new ConcurrentLinkedQueue<>();
        this.roundsAgreementSpace = new AgreementSpace(Protocol.AGR_ROUNDS);
        this.taskManagerAgreementSpace = new AgreementSpace(Protocol.AGR_TASK_MANAGERS);
        this.averagesAgreementSpace = new AgreementSpace(Protocol.AGR_AVERAGE);
    }

    public void setNumberOfRounds(int numberOfRounds) {
        System.out.println("Setting number of rounds to " + numberOfRounds);
        this.numberOfRounds = numberOfRounds;
    }

    public void run() {
        System.out.println("Starting " + this.numberOfRounds + " rounds");
//        waitOnDistributedBarrier(roundsAgreementSpace);
        initializeTaskManagers();
        waitOnDistributedBarrier(taskManagerAgreementSpace);
        getAverages();
        waitOnDistributedBarrier(averagesAgreementSpace);
        sendLoadBalancingMessages();
        roundsAgreementSpace.reset();
        taskManagerAgreementSpace.reset();
        averagesAgreementSpace.reset();
    }

    private void sendLoadBalancingMessages() {
        for (int i = 0; i < this.numberOfRounds; i++) {
            balanceLoad(i);
        }
    }

    private void initializeTaskManagers() {
        this.taskManagerList = new ArrayList<>();
        for (int i = 0; i < this.numberOfRounds; i++) {
            TaskManager taskManager = new TaskManager(this.node.getRng(), this.node.getThreadPool(), i, this.node.getTrafficStats());
            this.taskManagerList.add(taskManager);
        }
    }

    private void getAverages() {
        for (int i = 0; i < this.numberOfRounds; i++) {
            getTaskAverage(i);
        }
        for (TaskManager taskManager : this.taskManagerList) {
            while (!taskManager.averageIsSet()) {
                Thread.onSpinWait();
            }
            taskManager.startInitialTasks();
        }
    }

    private void waitOnDistributedBarrier(AgreementSpace agreementSpace) {
        int agreementPolicy = agreementSpace.getAgreementPolicy();
        agreementSpace.setIAmReady(true);
        while (!this.nodeAgreementDeque.isEmpty()) {
            NodeAgreement nodeAgreement = this.nodeAgreementDeque.poll();
            if (nodeAgreement.getAgreement() == agreementPolicy) {
                handleNodeAgreement(nodeAgreement);
            }
        }
        NodeAgreement nodeAgreement = new NodeAgreement(agreementPolicy, this.node.getId());
        this.node.getPartnerNode().writeToSocket(nodeAgreement);
        while (!agreementSpace.areAllReady()) {
            Thread.onSpinWait();
        }
    }

    private void getTaskAverage(int iteration) {
        TaskManager taskManager = this.taskManagerList.get(iteration);
        RoundAverage roundAverage = new RoundAverage(taskManager.getCurrentNumberOfTasks(), this.node.getId(), iteration);
        this.node.getPartnerNode().writeToSocket(roundAverage);
    }

    private void balanceLoad(int iteration) {

        // Get the correct TaskManager instance
        TaskManager taskManager = this.taskManagerList.get(iteration);

        // If we have too many tasks, send them around the ring in a TaskDelivery message
        if (taskManager.shouldGiveTasks()) {
            TaskDelivery taskDelivery = new TaskDelivery(taskManager.getTaskDiff(), this.node.getId(), iteration);
            taskManager.giveTasks();
            this.node.getPartnerNode().writeToSocket(taskDelivery);
        }

    }

    public void handleTaskAverage(RoundAverage roundAverage) {

        // Get correct TaskManager instance
        int iteration = roundAverage.getIteration();
        TaskManager taskManager = this.taskManagerList.get(iteration);

        // We got our message back
        if (roundAverage.nodeIsFirst(this.node.getId())) {
            double average = roundAverage.getSum() / roundAverage.getNumberOfNodes();
            taskManager.setAverage(average);
        }

        // Not our message, relay it
        else {
            relayTaskAverage(taskManager, roundAverage);
        }

    }

    public void handleTaskDelivery(TaskDelivery taskDelivery) {

        // Get the correct TaskManager reference
        int iteration = taskDelivery.getIteration();
        TaskManager taskManager = this.taskManagerList.get(iteration);

        // If I originated this message. Absorb excess.
        if (taskDelivery.nodeIsFirst(this.node.getId())) {
            int tasksLeft = taskDelivery.getNumTasks();
            taskManager.absorbExcessTasks(tasksLeft);
        }

        // If I didn't originate this message, relay it
        else {
            taskManager.handleTaskDelivery(taskDelivery);
            if (taskDelivery.getNumTasks() > 0) relayTaskDelivery(taskDelivery);
        }

    }

    public void handleNodeAgreement(NodeAgreement nodeAgreement) {

        // Get the correct AgreementSpace instance
        int agreementPolicy = nodeAgreement.getAgreement();
        AgreementSpace agreementSpace;
        switch (agreementPolicy) {
            case Protocol.AGR_ROUNDS:
                agreementSpace = roundsAgreementSpace;
                break;
            case Protocol.AGR_TASK_MANAGERS:
                agreementSpace = taskManagerAgreementSpace;
                break;
            case Protocol.AGR_AVERAGE:
                agreementSpace = averagesAgreementSpace;
                break;
            default:
                agreementSpace = null;
        }

        if (agreementSpace != null) {
            // If I sent this, everyone has agreed
            if (nodeAgreement.iSentThisMessage(this.node.getId())) {
                agreementSpace.setAllAreReady(true);
            }

            // If I am ready, relay this message
            else if (agreementSpace.amIReady()) {
                this.node.getPartnerNode().writeToSocket(nodeAgreement);
            }

            // I am not ready yet, push this message into my nodeAgreementDeque
            else {
                this.nodeAgreementDeque.add(nodeAgreement);
            }
        }

    }

    private void relayTaskAverage(TaskManager taskManager, RoundAverage roundAverage) {
        roundAverage.processRelay(this.node.getId(), taskManager.getInitialNumberOfTasks());
        this.node.getPartnerNode().writeToSocket(roundAverage);
    }

    private void relayTaskDelivery(TaskDelivery taskDelivery) {
        taskDelivery.processRelay(this.node.getId());
        this.node.getPartnerNode().writeToSocket(taskDelivery);
    }

    public void printTaskManagerStats() {
        int sum = 0;
        for (TaskManager taskManager : this.taskManagerList) {
            sum += taskManager.getCurrentNumberOfTasks();
        }
        System.out.println("Total tasks: " + sum);
    }

}
