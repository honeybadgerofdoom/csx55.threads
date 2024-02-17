package csx55.overlay.transport;

import csx55.overlay.node.MessagingNode;
import csx55.overlay.node.PartnerNodeRef;
import csx55.overlay.util.TaskManager;
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.LoadBalanced;
import csx55.overlay.wireformats.TaskAverage;
import csx55.overlay.wireformats.TaskDelivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskProcessor implements Runnable {

    private MessagingNode node;
    private final int numberOfRounds;
    private TaskManager taskManager;
    private List<LoadBalanced> loadBalancedList = new ArrayList<>();
    private boolean loadBalancedReceived;

    public TaskProcessor(MessagingNode node, int numberOfRounds) {
        this.node = node;
        this.numberOfRounds = numberOfRounds;
    }

    public void run() {
        ConcurrentLinkedQueue<Event> taskQueue = this.node.getThreadPool().getTaskQueue();
        PartnerNodeRef partnerNodeRef = this.node.getOneNeighbor();
        for (int i = 0; i < this.numberOfRounds; i++) {
            System.out.println("Beginning iteration " + i);
            loadBalancedReceived = false;
            this.taskManager = new TaskManager(this.node.getRng());
            getTaskAverage(partnerNodeRef);
            balanceLoad(partnerNodeRef);

            // Wait until I'm load balanced, then send LoadBalanced message
            System.out.println("Waiting until I'm load balanced...");
            while (!taskManager.isLoadBalanced()) {}

            // ToDo add correct # tasks to taskQueue

            System.out.println("Load is balanced. Relaying all LoadBalanced messages.");

            // Forward all LoadBalanced messages in my loadBalancedList
            for (LoadBalanced loadBalanced : this.loadBalancedList) {
                relayLoadBalanced(loadBalanced);
            }

            System.out.println("All LoadBalanced messages relayed. Waiting for my own LoadBalanced message");

            // Wait until I receive my own LoadBalanced message
            while (!loadBalancedReceived) {}

            System.out.println(taskManager);

        }
    }

    private void getTaskAverage(PartnerNodeRef partnerNodeRef) {
        TaskAverage taskAverage = new TaskAverage(this.taskManager.getCurrentNumberOfTasks(), this.node.getId());
        partnerNodeRef.writeToSocket(taskAverage);
    }

    private void balanceLoad(PartnerNodeRef partnerNodeRef) {
        while (!this.taskManager.averageIsSet()) {} // We can't hear anything from the registry during this time
        if (this.taskManager.shouldGiveTasks()) {
            TaskDelivery taskDelivery = new TaskDelivery(this.taskManager.getTaskDiff(), this.node.getId());
            this.taskManager.giveTasks(this.taskManager.getTaskDiff());
            partnerNodeRef.writeToSocket(taskDelivery);
        }
    }

    public void handleTaskAverage(TaskAverage taskAverage) {
        if (taskAverage.nodeIsFirst(this.node.getId())) {
            double average = (double) taskAverage.getSum() / taskAverage.getNumberOfNodes();
            synchronized (this.taskManager) {
                this.taskManager.setAverage(average);
            }
        }
        else {
            String lastNode = taskAverage.processRelay(this.node.getId(), this.taskManager.getCurrentNumberOfTasks());
            for (String key : this.node.getPartnerNodes().keySet()) {
                if (!key.equals(lastNode)) {
                    PartnerNodeRef partnerNodeRef = this.node.getPartnerNodes().get(key);
                    partnerNodeRef.writeToSocket(taskAverage);
                }
            }
        }
    }

    public void handleTaskDelivery(TaskDelivery taskDelivery) {
        while (!this.taskManager.averageIsSet()) {}
        if (!taskDelivery.nodeIsFirst(this.node.getId())) {
            this.taskManager.handleTaskDelivery(taskDelivery);
            if (taskDelivery.getNumTasks() > 0) {
                String lastNode = taskDelivery.processRelay(this.node.getId());
                for (String key : this.node.getPartnerNodes().keySet()) {
                    if (!key.equals(lastNode)) {
                        PartnerNodeRef partnerNodeRef = this.node.getPartnerNodes().get(key);
                        partnerNodeRef.writeToSocket(taskDelivery);
                    }
                }
            }
        }
        else {
            int tasksLeft = taskDelivery.getNumTasks();
            this.taskManager.absorbExcessTasks(tasksLeft);
        }

    }

    public void handleLoadBalanced(LoadBalanced loadBalanced) {
        /*
        * TODO
        *  - If this message originated with me, I can iterate
        *  - If I'm load balanced, relay the message
        *  - Else, push the message into my loadBalancedList
        * */
        if (loadBalanced.nodeIsFirst(this.node.getId())) {
            this.loadBalancedReceived = true;
        }
        else if (this.taskManager.isLoadBalanced()) {
            relayLoadBalanced(loadBalanced);
        }
        else {
            this.loadBalancedList.add(loadBalanced);
        }
    }

    private void relayLoadBalanced(LoadBalanced loadBalanced) {
        String lastNode = loadBalanced.processRelay(this.node.getId());
        for (String key : this.node.getPartnerNodes().keySet()) {
            if (!key.equals(lastNode)) {
                PartnerNodeRef relayTarget = this.node.getPartnerNodes().get(key);
                relayTarget.writeToSocket(loadBalanced);
            }
        }
    }

}
