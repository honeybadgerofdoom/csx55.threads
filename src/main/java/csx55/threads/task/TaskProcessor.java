package csx55.threads.task;

import csx55.threads.ComputeNode;
import csx55.threads.node.PartnerNodeRef;
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

    private boolean allTaskManagersReady = false;
    private boolean myTaskManagersReady = false;

    private boolean allNodesAreReadyToProceed = false;
    private boolean iAmReadyToProceed = false;

    public TaskProcessor(ComputeNode node) {
        System.out.println("TaskProcessor is instantiated.");
        this.node = node;
        this.nodeAgreementDeque = new ConcurrentLinkedQueue<>();
    }

    public void setNumberOfRounds(int numberOfRounds) {
        System.out.println("Setting number of rounds to " + numberOfRounds);
        this.numberOfRounds = numberOfRounds;
    }

    public void run() {
        System.out.println("Starting " + this.numberOfRounds + " rounds");
        initializeTaskManagers();
        waitForNodeAgreement(Protocol.AGR_TASK_MANAGERS);
        allNodesAreReadyToProceed = false;
        iAmReadyToProceed = false;
        getAverages();
        waitForNodeAgreement(Protocol.AGR_AVERAGE);
        sendLoadBalancingMessages();
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

    private void waitForNodeAgreement(int agreementPolicy) {

        if (agreementPolicy == Protocol.AGR_TASK_MANAGERS) {
            myTaskManagersReady = true;
            while (!this.nodeAgreementDeque.isEmpty()) {
                NodeAgreement nodeAgreement = this.nodeAgreementDeque.poll();
                if (nodeAgreement.getAgreement() == agreementPolicy) {
                    handleNodeAgreement(nodeAgreement);
                }
            }
            NodeAgreement nodeAgreement = new NodeAgreement(agreementPolicy, this.node.getId());
            this.node.getPartnerNode().writeToSocket(nodeAgreement);
            while (!allTaskManagersReady) {
                Thread.onSpinWait();
            }
        }

        else if (agreementPolicy == Protocol.AGR_AVERAGE) {
            iAmReadyToProceed = true;
            while (!this.nodeAgreementDeque.isEmpty()) {
                NodeAgreement nodeAgreement = this.nodeAgreementDeque.poll();
                if (nodeAgreement.getAgreement() == agreementPolicy) {
                    handleNodeAgreement(nodeAgreement);
                }
            }
            NodeAgreement nodeAgreement = new NodeAgreement(agreementPolicy, this.node.getId());
            this.node.getPartnerNode().writeToSocket(nodeAgreement);
            while (!allNodesAreReadyToProceed) {
                Thread.onSpinWait();
            }
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
        int agreementPolicy = nodeAgreement.getAgreement();
        if (agreementPolicy == Protocol.AGR_TASK_MANAGERS) {
            if (nodeAgreement.iSentThisMessage(this.node.getId())) {
                allTaskManagersReady = true;
            }
            else if (myTaskManagersReady) {
                this.node.getPartnerNode().writeToSocket(nodeAgreement);
            }
            else {
                this.nodeAgreementDeque.add(nodeAgreement);
            }
        }

        else if (agreementPolicy == Protocol.AGR_AVERAGE) {
            if (nodeAgreement.iSentThisMessage(this.node.getId())) {
                allNodesAreReadyToProceed = true;
            }
            else if (iAmReadyToProceed) {
                this.node.getPartnerNode().writeToSocket(nodeAgreement);
            }
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

    private class AgreementSpace {

        private boolean iAmReady = false;
        private boolean allAreReady = false;

        public boolean isiAmReady() {
            return iAmReady;
        }

        public boolean isAllAreReady() {
            return allAreReady;
        }

        public void setIAmReady(boolean iAmReady) {
            this.iAmReady = iAmReady;
        }

        public void setAllAreReady(boolean allAreReady) {
            this.allAreReady = allAreReady;
        }
        
    }

}
