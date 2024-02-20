package csx55.overlay.transport;

import csx55.overlay.node.MessagingNode;
import csx55.overlay.node.PartnerNodeRef;
import csx55.overlay.util.TaskManager;
import csx55.overlay.wireformats.AveragesCalculated;
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.TaskAverage;
import csx55.overlay.wireformats.TaskDelivery;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

/*
* FIXME
*  - If the same node happens to always absorb the excess tasks, it might get way too many...
*  - I'm seeing that often times one node has ~20% too many and the rest are nicely balanced
* */

public class TaskProcessor implements Runnable {

    private MessagingNode node;
    private final int numberOfRounds;
    private List<TaskManager> taskManagerList;
    private boolean allAveragesCalculated = false;
    private Deque<AveragesCalculated> averagesCalculatedDeque;
    private boolean myAveragesAreAllCalculated = false;

    public TaskProcessor(MessagingNode node, int numberOfRounds) {
        this.node = node;
        this.numberOfRounds = numberOfRounds;
        this.averagesCalculatedDeque = new ArrayDeque<>();
    }

    public void run() {
        PartnerNodeRef partnerNodeRef = this.node.getOneNeighbor();
        this.taskManagerList = new ArrayList<>();

        for (int i = 0; i < this.numberOfRounds; i++) {
            TaskManager taskManager = new TaskManager(this.node.getRng(), this.node.getThreadPool(), i, this.node.getTrafficStats());
            this.taskManagerList.add(taskManager);
        }

        // FIXME We need to wait until these are all instantiated. Find a better way that sleep()
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < this.numberOfRounds; i++) {
            getTaskAverage(partnerNodeRef, i);
        }

        int totalAvg = 0;
        for (TaskManager taskManager : this.taskManagerList) {
            while (!taskManager.averageIsSet()) {
                Thread.onSpinWait();
            }
            taskManager.startInitialTasks();
            totalAvg += taskManager.getAverage();
        }
        System.out.println("Total average: " + totalAvg);

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

        System.out.println("TaskProcessor complete.");

    }

    private void getTaskAverage(PartnerNodeRef partnerNodeRef, int iteration) {
        TaskManager taskManager = this.taskManagerList.get(iteration);
        TaskAverage taskAverage = new TaskAverage(taskManager.getCurrentNumberOfTasks(), this.node.getId(), iteration);
        partnerNodeRef.writeToSocket(taskAverage);
    }

    private void balanceLoad(PartnerNodeRef partnerNodeRef, int iteration) {

        // Get the correct TaskManager instance
        TaskManager taskManager = this.taskManagerList.get(iteration);

        if (taskManager.shouldGiveTasks()) {
            TaskDelivery taskDelivery = new TaskDelivery(taskManager.getTaskDiff(), this.node.getId(), iteration);
            taskManager.giveTasks();
            partnerNodeRef.writeToSocket(taskDelivery);
        }

    }

    public void handleTaskAverage(TaskAverage taskAverage) {
        int iteration = taskAverage.getIteration();
        TaskManager taskManager = this.taskManagerList.get(iteration);
        if (taskAverage.nodeIsFirst(this.node.getId())) {
            double average = taskAverage.getSum() / taskAverage.getNumberOfNodes();
            taskManager.setAverage(average);
        }
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
            String lastNode = averagesCalculated.processRelay(this.node.getId());
            relay(lastNode, averagesCalculated);
        }
        else {
            this.averagesCalculatedDeque.add(averagesCalculated);
        }
    }

    private void relayTaskAverage(TaskManager taskManager, TaskAverage taskAverage) {
        String lastNode = taskAverage.processRelay(this.node.getId(), taskManager.getInitialNumberOfTasks());
        relay(lastNode, taskAverage);
    }

    private void relayTaskDelivery(TaskDelivery taskDelivery) {
        String lastNode = taskDelivery.processRelay(this.node.getId());
        relay(lastNode, taskDelivery);
    }

    private void relay(String lastNode, Event event) {
        for (String key : this.node.getPartnerNodes().keySet()) {
            if (key.equals(lastNode)) continue;
            PartnerNodeRef relayTarget = this.node.getPartnerNodes().get(key);
            relayTarget.writeToSocket(event);
        }
    }

}
