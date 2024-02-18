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
    private int totalTasksProcessed = 0;
    ConcurrentLinkedQueue<Event> taskQueue;

    public TaskProcessor(MessagingNode node, int numberOfRounds) {
        this.node = node;
        this.numberOfRounds = numberOfRounds;
    }

    public void run() {
        this.taskQueue = this.node.getThreadPool().getTaskQueue();
        PartnerNodeRef partnerNodeRef = this.node.getOneNeighbor();
        this.taskManagerList = new ArrayList<>();

        for (int i = 0; i < this.numberOfRounds; i++) {
            TaskManager taskManager = new TaskManager(this.node.getRng());
            this.taskManagerList.add(taskManager);
        }

        for (int i = 0; i < this.numberOfRounds; i++) {
            getTaskAverage(partnerNodeRef, i);
            balanceLoad(partnerNodeRef, i);
        }

        // Wait for all taskManagers to get balanced
        for (TaskManager taskManager : this.taskManagerList) {
            while (!taskManager.isBalanced()) {}
        }
        System.out.println("Total processed: " + this.totalTasksProcessed);

    }

    private synchronized void updateTotal(int num) {
        this.totalTasksProcessed += num;
    }

    private void addToTaskQueue(int numberOfTasksToAdd) {
        updateTotal(numberOfTasksToAdd);
        /**
         * ToDo
         *  - Create `numberOfTasksToAdd` tasks
         *  - this.taskQueue.addAll(tasks)
         */
    }

    private void getTaskAverage(PartnerNodeRef partnerNodeRef, int iteration) {
        TaskManager taskManager = this.taskManagerList.get(iteration);
        TaskAverage taskAverage = new TaskAverage(taskManager.getCurrentNumberOfTasks(), this.node.getId(), iteration);
        partnerNodeRef.writeToSocket(taskAverage);
    }

    private void balanceLoad(PartnerNodeRef partnerNodeRef, int iteration) {
        TaskManager taskManager = this.taskManagerList.get(iteration);
        while (!taskManager.averageIsSet()) {
            //  Thread.onSpinWait();
        } // We can't hear anything from the registry during this time

        // ToDo refactor this trash
        if (taskManager.shouldGiveTasks()) {
            TaskDelivery taskDelivery = new TaskDelivery(taskManager.getTaskDiff(), this.node.getId(), iteration);
            taskManager.giveTasks(taskManager.getTaskDiff());
            partnerNodeRef.writeToSocket(taskDelivery);
        }
        else {
            TaskDelivery taskDelivery = new TaskDelivery(0, this.node.getId(), iteration);
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
            addToTaskQueue(taskManager.getCurrentNumberOfTasks());
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
            if (!key.equals(lastNode)) {
                PartnerNodeRef relayTarget = this.node.getPartnerNodes().get(key);
                relayTarget.writeToSocket(event);
            }
        }
    }

}
