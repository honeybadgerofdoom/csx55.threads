package csx55.threads.transport;

import csx55.threads.ComputeNode;
import csx55.threads.node.PartnerNodeRef;
import csx55.threads.util.TaskManager;
import csx55.threads.wireformats.AveragesCalculated;
import csx55.threads.wireformats.Event;
import csx55.threads.wireformats.TaskAverage;
import csx55.threads.wireformats.TaskDelivery;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

public class TaskProcessor implements Runnable {

    private final ComputeNode node;
    private final int numberOfRounds;
    private List<TaskManager> taskManagerList;
    private boolean allAveragesCalculated = false;
    private final Deque<AveragesCalculated> averagesCalculatedDeque;
    private boolean myAveragesAreAllCalculated = false;

    public TaskProcessor(ComputeNode node, int numberOfRounds) {
        this.node = node;
        this.numberOfRounds = numberOfRounds;
        this.averagesCalculatedDeque = new ArrayDeque<>();
    }

    public void run() {
        System.out.println("Starting " + this.numberOfRounds + " rounds");
        PartnerNodeRef partnerNodeRef = this.node.getPartnerNode();
        this.taskManagerList = new ArrayList<>();

        for (int i = 0; i < this.numberOfRounds; i++) {
            TaskManager taskManager = new TaskManager(this.node.getRng(), this.node.getThreadPool(), i, this.node.getTrafficStats());
            this.taskManagerList.add(taskManager);
        }

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
                Thread.onSpinWait();
            }
            taskManager.startInitialTasks();
        }

        // Wait until all nodes have calculated all their averages
        this.myAveragesAreAllCalculated = true;
        while (!this.averagesCalculatedDeque.isEmpty()) {
            AveragesCalculated averagesCalculated = this.averagesCalculatedDeque.poll();
            handleAveragesCalculated(averagesCalculated);
        }
        AveragesCalculated averagesCalculated = new AveragesCalculated(this.node.getId());
        partnerNodeRef.writeToSocket(averagesCalculated);

        while (!this.allAveragesCalculated) {
            Thread.onSpinWait();
        }

        // Load balancing
        for (int i = 0; i < this.numberOfRounds; i++) {
            balanceLoad(partnerNodeRef, i);
        }

    }

    private void getTaskAverage(PartnerNodeRef partnerNodeRef, int iteration) {
        TaskManager taskManager = this.taskManagerList.get(iteration);
        TaskAverage taskAverage = new TaskAverage(taskManager.getCurrentNumberOfTasks(), this.node.getId(), iteration);
        partnerNodeRef.writeToSocket(taskAverage);
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

    public void handleTaskAverage(TaskAverage taskAverage) {

        // Get correct TaskManager instance
        int iteration = taskAverage.getIteration();
        TaskManager taskManager = this.taskManagerList.get(iteration);

        // We got our message back
        if (taskAverage.nodeIsFirst(this.node.getId())) {
            double average = taskAverage.getSum() / taskAverage.getNumberOfNodes();
            taskManager.setAverage(average);
        }

        // Not our message, relay it
        else {
            relayTaskAverage(taskManager, taskAverage);
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

    private void relayTaskAverage(TaskManager taskManager, TaskAverage taskAverage) {
        taskAverage.processRelay(this.node.getId(), taskManager.getInitialNumberOfTasks());
        this.node.getPartnerNode().writeToSocket(taskAverage);
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
