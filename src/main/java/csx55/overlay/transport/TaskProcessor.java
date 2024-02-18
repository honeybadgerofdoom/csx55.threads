package csx55.overlay.transport;

import csx55.overlay.node.MessagingNode;
import csx55.overlay.node.PartnerNodeRef;
import csx55.overlay.util.TaskManager;
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.LoadBalanced;
import csx55.overlay.wireformats.TaskAverage;
import csx55.overlay.wireformats.TaskDelivery;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskProcessor implements Runnable {

    private MessagingNode node;
    private final int numberOfRounds;
    private TaskManager taskManager;
    private Queue<LoadBalanced> loadBalancedQueue = new ArrayDeque<>();
    private Queue<TaskAverage> taskAverageQueue = new ArrayDeque<>();
    private Queue<TaskDelivery> taskDeliveryQueue = new ArrayDeque<>();
    private volatile boolean loadBalancedReceived;
    private int iteration;

    public TaskProcessor(MessagingNode node, int numberOfRounds) {
        this.node = node;
        this.numberOfRounds = numberOfRounds;
    }

    public void run() {
        ConcurrentLinkedQueue<Event> taskQueue = this.node.getThreadPool().getTaskQueue();
        PartnerNodeRef partnerNodeRef = this.node.getOneNeighbor();
        for (this.iteration = 0; iteration < this.numberOfRounds; iteration++) {

            System.out.println("Beginning iteration " + iteration);
            loadBalancedReceived = false;

            this.taskManager = new TaskManager(this.node.getRng());
            
            getTaskAverage(partnerNodeRef);

            // FIXME Where do these 2 queue handlers go?
            System.out.println("Relaying all " + this.taskAverageQueue.size() + " TaskAverage messages.");
            while (!this.taskAverageQueue.isEmpty()) {
                TaskAverage taskAverage = this.taskAverageQueue.poll();
                handleTaskAverage(taskAverage);
            }
            
            System.out.println("Relaying all " + this.taskDeliveryQueue.size() + " TaskDelivery messages.");
            while (!this.taskDeliveryQueue.isEmpty()) {
                TaskDelivery taskDelivery = this.taskDeliveryQueue.poll();
                handleTaskDelivery(taskDelivery);
            }

            balanceLoad(partnerNodeRef);

            System.out.println("Waiting until I'm load balanced...");
            while (!taskManager.isLoadBalanced()) {
                // Thread.onSpinWait();
            }

            System.out.println("Load is balanced, sending LoadBalanced message...");
            LoadBalanced myLoadBalanced = new LoadBalanced(this.node.getId(), this.iteration);
            partnerNodeRef.writeToSocket(myLoadBalanced);

            // ToDo add correct # tasks to taskQueue

            // ToDo wait for taskQueue to be empty
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                System.out.println("Failed to sleep thread " + e);
//            }

            System.out.println("Relaying all " + this.loadBalancedQueue.size() + " LoadBalanced messages.");
            while (!this.loadBalancedQueue.isEmpty()) {
                LoadBalanced loadBalanced = this.loadBalancedQueue.poll();
                relayLoadBalanced(loadBalanced);
            }

            System.out.println("All LoadBalanced messages relayed. Waiting for my own LoadBalanced message...");
            while (!loadBalancedReceived) {
                // Thread.onSpinWait();
            }
            System.out.println("Received my own LoadBalanced message");

            System.out.println(taskManager);
            
        }
    }

    private void getTaskAverage(PartnerNodeRef partnerNodeRef) {
        TaskAverage taskAverage = new TaskAverage(this.taskManager.getCurrentNumberOfTasks(), this.node.getId(), this.iteration);
        partnerNodeRef.writeToSocket(taskAverage);
    }

    private void balanceLoad(PartnerNodeRef partnerNodeRef) {
        while (!this.taskManager.averageIsSet()) {
            // Thread.onSpinWait();
        } // We can't hear anything from the registry during this time
        if (this.taskManager.shouldGiveTasks()) {
            TaskDelivery taskDelivery = new TaskDelivery(this.taskManager.getTaskDiff(), this.node.getId(), this.iteration);
            this.taskManager.giveTasks(this.taskManager.getTaskDiff());
            partnerNodeRef.writeToSocket(taskDelivery);
        }
    }

    public void handleTaskAverage(TaskAverage taskAverage) {
        if (this.iteration <= taskAverage.getIteration()) {
            if (taskAverage.nodeIsFirst(this.node.getId())) {
                double average = (double) taskAverage.getSum() / taskAverage.getNumberOfNodes();
                synchronized (this.taskManager) {
                    this.taskManager.setAverage(average);
                }
            } else {
                relayTaskAverage(taskAverage);
            }
        }
        else {
            this.taskAverageQueue.add(taskAverage);
        }
    }

    public void handleTaskDelivery(TaskDelivery taskDelivery) {
        while (!this.taskManager.averageIsSet()) {
            // Thread.onSpinWait();
        }
        if (this.iteration <= taskDelivery.getIteration()) {
            if (!taskDelivery.nodeIsFirst(this.node.getId())) {
                this.taskManager.handleTaskDelivery(taskDelivery);
                if (taskDelivery.getNumTasks() > 0) {
                    relayTaskDelivery(taskDelivery);
                }
            }
            else {
                int tasksLeft = taskDelivery.getNumTasks();
                this.taskManager.absorbExcessTasks(tasksLeft);
            }
        }
        else {
            this.taskDeliveryQueue.add(taskDelivery);
        }
    }

    public synchronized void handleLoadBalanced(LoadBalanced loadBalanced) {
        /*
        * FIXME
        *  - This isn't working, check logic
        * */
        if (this.iteration != loadBalanced.getIteration()) {
            this.loadBalancedQueue.add(loadBalanced);
        }
        else if (loadBalanced.nodeIsFirst(this.node.getId())) {
            this.loadBalancedReceived = true;
        }
        else if (this.taskManager.isLoadBalanced()) {
            relayLoadBalanced(loadBalanced);
        }
        else {
            this.loadBalancedQueue.add(loadBalanced);
        }
    }

    private void relayLoadBalanced(LoadBalanced loadBalanced) {
        if (this.iteration != loadBalanced.getIteration()) {
            this.loadBalancedQueue.add(loadBalanced);
        }
        String lastNode = loadBalanced.processRelay(this.node.getId());
        for (String key : this.node.getPartnerNodes().keySet()) {
            if (!key.equals(lastNode)) {
                PartnerNodeRef relayTarget = this.node.getPartnerNodes().get(key);
                relayTarget.writeToSocket(loadBalanced);
            }
        }
    }

    private void relayTaskAverage(TaskAverage taskAverage) {
        String lastNode = taskAverage.processRelay(this.node.getId(), this.taskManager.getCurrentNumberOfTasks());
        for (String key : this.node.getPartnerNodes().keySet()) {
            if (!key.equals(lastNode)) {
                PartnerNodeRef relayTarget = this.node.getPartnerNodes().get(key);
                relayTarget.writeToSocket(taskAverage);
            }
        }
    }

    private void relayTaskDelivery(TaskDelivery taskDelivery) {
        String lastNode = taskDelivery.processRelay(this.node.getId());
        for (String key : this.node.getPartnerNodes().keySet()) {
            if (!key.equals(lastNode)) {
                PartnerNodeRef relayTarget = this.node.getPartnerNodes().get(key);
                relayTarget.writeToSocket(taskDelivery);
            }
        }
    }

}
