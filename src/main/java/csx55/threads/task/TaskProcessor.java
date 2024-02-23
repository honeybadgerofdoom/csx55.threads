package csx55.threads.task;

import csx55.threads.ComputeNode;
import csx55.threads.node.PartnerNodeRef;
import csx55.threads.wireformats.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

public class TaskProcessor implements Runnable {

    private final ComputeNode node;
    private final int numberOfRounds;
    private List<TaskManager> taskManagerList;

    private final Deque<AveragesCalculated> averagesCalculatedDeque;
    private boolean allAveragesCalculated = false;
    private boolean myAveragesAreAllCalculated = false;

    private final Deque<NodeAgreement> nodeAgreementDeque;
    private boolean allNodesAreReadyToProceed = false;
    private boolean iAmReadyToProceed = false;

    public TaskProcessor(ComputeNode node, int numberOfRounds) {
        this.node = node;
        this.numberOfRounds = numberOfRounds;
        this.averagesCalculatedDeque = new ArrayDeque<>();
        this.nodeAgreementDeque = new ArrayDeque<>();
    }

    public void run() {
        System.out.println("Starting " + this.numberOfRounds + " rounds");
        PartnerNodeRef partnerNodeRef = this.node.getPartnerNode();
        this.taskManagerList = new ArrayList<>();

        for (int i = 0; i < this.numberOfRounds; i++) {
            TaskManager taskManager = new TaskManager(this.node.getRng(), this.node.getThreadPool(), i, this.node.getTrafficStats());
            this.taskManagerList.add(taskManager);
        }

        // Wait for all TaskManagers
//        waitForNodeAgreement(Protocol.AGR_READY);

//        this.iAmReadyToProceed = false;
//        this.allNodesAreReadyToProceed = false;

        // FIXME We need to wait until these are all instantiated. Find a better way that sleep() - just use a message like AveragesCalculated...
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < this.numberOfRounds; i++) {
            getTaskAverage(partnerNodeRef, i);
        }

        for (TaskManager taskManager : this.taskManagerList) {
            while (!taskManager.averageIsSet()) {
//                Thread.onSpinWait();
            }
            taskManager.startInitialTasks();
        }

        // Wait for all averages
        this.myAveragesAreAllCalculated = true;
        while (!this.averagesCalculatedDeque.isEmpty()) {
            AveragesCalculated averagesCalculated = this.averagesCalculatedDeque.poll();
            handleAveragesCalculated(averagesCalculated);
        }
        AveragesCalculated averagesCalculated = new AveragesCalculated(this.node.getId());
        partnerNodeRef.writeToSocket(averagesCalculated);
        while (!this.allAveragesCalculated) {
//            Thread.onSpinWait();
        }

        // Load balancing
        for (int i = 0; i < this.numberOfRounds; i++) {
            balanceLoad(partnerNodeRef, i);
        }

    }

    private void waitForNodeAgreement(int agreementPolicy) {
        this.iAmReadyToProceed = true;
        while (!this.nodeAgreementDeque.isEmpty()) {
            NodeAgreement nodeAgreement = this.nodeAgreementDeque.poll();
            if (nodeAgreement.getAgreement() == agreementPolicy) {
                handleNodeAgreement(nodeAgreement);
            }
        }
        NodeAgreement nodeAgreement = new NodeAgreement(agreementPolicy, this.node.getId());
        this.node.getPartnerNode().writeToSocket(nodeAgreement);
        while (!this.allNodesAreReadyToProceed) {
//            Thread.onSpinWait();
        }
    }

    private void getTaskAverage(PartnerNodeRef partnerNodeRef, int iteration) {
        TaskManager taskManager = this.taskManagerList.get(iteration);
        RoundAverage roundAverage = new RoundAverage(taskManager.getCurrentNumberOfTasks(), this.node.getId(), iteration);
        partnerNodeRef.writeToSocket(roundAverage);
    }

    private void balanceLoad(PartnerNodeRef partnerNodeRef, int iteration) {

        // Get the correct TaskManager instance
        TaskManager taskManager = this.taskManagerList.get(iteration);

        // If we have too many tasks, send them around the ring in a TaskDelivery message
        if (taskManager.shouldGiveTasks()) {
            TaskDelivery taskDelivery = new TaskDelivery(taskManager.getTaskDiff(), this.node.getId(), iteration);
            taskManager.giveTasks();
            partnerNodeRef.writeToSocket(taskDelivery);
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

    public void handleAveragesCalculated(AveragesCalculated averagesCalculated) {
        if (averagesCalculated.nodeIsFirst(this.node.getId())) {
            this.allAveragesCalculated = true;
        }
        else if (myAveragesAreAllCalculated) {
            averagesCalculated.processRelay(this.node.getId());
            this.node.getPartnerNode().writeToSocket(averagesCalculated);
        }
        else {
            this.averagesCalculatedDeque.add(averagesCalculated);
        }
    }

    public void handleNodeAgreement(NodeAgreement nodeAgreement) {
        if (nodeAgreement.iSentThisMessage(this.node.getId())) {
            this.allNodesAreReadyToProceed = true;
        }
        else if (iAmReadyToProceed) {
            this.node.getPartnerNode().writeToSocket(nodeAgreement);
        }
        else {
            this.nodeAgreementDeque.add(nodeAgreement);
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
