package csx55.overlay.transport;

import csx55.overlay.node.MessagingNode;
import csx55.overlay.node.PartnerNodeRef;
import csx55.overlay.util.TaskManager;
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.TaskAverage;
import csx55.overlay.wireformats.TaskDelivery;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;

public class TaskProcessor implements Runnable {

    private MessagingNode node;
    private final int numberOfRounds;
    private List<TaskManager> taskManagerList;


    public TaskProcessor(MessagingNode node, int numberOfRounds) {
        this.node = node;
        this.numberOfRounds = numberOfRounds;
    }

    public void run() {
        ConcurrentLinkedQueue<Event> taskQueue = this.node.getThreadPool().getTaskQueue();
        PartnerNodeRef partnerNodeRef = this.node.getOneNeighbor();

        for (int i = 0; i < this.numberOfRounds; i++) {
            TaskManager taskManager = new TaskManager(this.node.getRng());
            this.taskManagerList.add(taskManager);
        }

        for (int i = 0; i < this.numberOfRounds; i++) {

            TaskManager taskManager = new TaskManager(this.node.getRng());
            
            getTaskAverage(partnerNodeRef, i);

            balanceLoad(partnerNodeRef, i);

            
            System.out.println(taskManager);
            
        }
    }

    private void getTaskAverage(PartnerNodeRef partnerNodeRef, int iteration) {
        TaskManager taskManager = this.taskManagerList.get(iteration);
        TaskAverage taskAverage = new TaskAverage(taskManager.getCurrentNumberOfTasks(), this.node.getId(), iteration);
        partnerNodeRef.writeToSocket(taskAverage);
    }

    private void balanceLoad(PartnerNodeRef partnerNodeRef, int iteration) {
        TaskManager taskManager = this.taskManagerList.get(iteration);
        while (!taskManager.averageIsSet()) {
            // Thread.onSpinWait();
        } // We can't hear anything from the registry during this time
        if (taskManager.shouldGiveTasks()) {
            TaskDelivery taskDelivery = new TaskDelivery(taskManager.getTaskDiff(), this.node.getId(), iteration);
            taskManager.giveTasks(taskManager.getTaskDiff());
            partnerNodeRef.writeToSocket(taskDelivery);
        }
    }

    public void handleTaskAverage(TaskAverage taskAverage) {
        int iteration = taskAverage.getIteration();
        TaskManager taskManager = this.taskManagerList.get(iteration);
        if (taskAverage.nodeIsFirst(this.node.getId())) {
            double average = (double) taskAverage.getSum() / taskAverage.getNumberOfNodes();
            synchronized (taskManager) {
                taskManager.setAverage(average);
            }
        } else {
            relayTaskAverage(taskManager, taskAverage);
        }
    }

    public void handleTaskDelivery(TaskDelivery taskDelivery) {
        int iteration = taskDelivery.getIteration();
        TaskManager taskManager = this.taskManagerList.get(iteration);
        while (!taskManager.averageIsSet()) {
            // Thread.onSpinWait();
        }
        if (!taskDelivery.nodeIsFirst(this.node.getId())) {
            taskManager.handleTaskDelivery(taskDelivery);
            if (taskDelivery.getNumTasks() > 0) {
                relayTaskDelivery(taskDelivery);
            }
        }
        else {
            int tasksLeft = taskDelivery.getNumTasks();
            taskManager.absorbExcessTasks(tasksLeft);
        }
    }

    private void relayTaskAverage(TaskManager taskManager, TaskAverage taskAverage) {
        String lastNode = taskAverage.processRelay(this.node.getId(), taskManager.getCurrentNumberOfTasks());
        relay(lastNode, taskAverage);
    }

    private void relayTaskDelivery(TaskDelivery taskDelivery) {
        String lastNode = taskDelivery.processRelay(this.node.getId());
        relay(lastNode, taskDelivery);
    }

    private void relay(String lastNode, Event event) {
        for (String key : this.node.getPartnerNodes().keySet()) {
            if (!key.equals(lastNode)) {
                PartnerNodeRef relayTarget = this.node.getPartnerNodes().get(key);
                relayTarget.writeToSocket(event);
            }
        }
    }

}
