package csx55.overlay.transport;

import csx55.overlay.node.MessagingNode;
import csx55.overlay.node.PartnerNodeRef;
import csx55.overlay.util.TaskManager;
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.TaskAverage;
import csx55.overlay.wireformats.TaskDelivery;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskProcessor implements Runnable {

    private MessagingNode node;
    private final int numberOfRounds;

    public TaskProcessor(MessagingNode node, int numberOfRounds) {
        this.node = node;
        this.numberOfRounds = numberOfRounds;
    }

    public void run() {
        ConcurrentLinkedQueue<Event> taskQueue = this.node.getThreadPool().getTaskQueue();
        PartnerNodeRef partnerNodeRef = this.node.getOneNeighbor();
        for (int i = 0; i < this.numberOfRounds; i++) {
            TaskManager taskManager = new TaskManager(this.node.getRng());
            this.node.setTaskManager(taskManager);
            getTaskAverage(partnerNodeRef);
            balanceLoad(partnerNodeRef);
            /*
            * TODO
            *  - Load up tasks into taskQueue
            *  - Figure out how to manage synchronization of load balances per round
            *  - Code was initially written to only balance the load once
            *  - Now we must calculate new averages and balance new loads every iteration, in a thread-safe way
            *    between `n` MessagingNode instances
            *       -> This is where it gets tricky
            *       - handleTaskAverage() & handleTaskDelivery() won't be able to rely on an instance of TaskManager
            *       - Maybe we toss the TaskManager and figure out a new way to manage that data
            *       - Basically, these iterations will be changing the TaskManager reference out from under the
            *         MessagingNode instances that created this TaskProcessor. This won't work.
            *       - We could maintain a data structure where each index is an iteration and the value within
            *         is a TaskManager reference?
            * */
        }
    }

    private void getTaskAverage(PartnerNodeRef partnerNodeRef) {
        TaskAverage taskAverage = new TaskAverage(this.node.getTaskManager().getCurrentNumberOfTasks(), this.node.getId());
        partnerNodeRef.writeToSocket(taskAverage);
    }

    private void balanceLoad(PartnerNodeRef partnerNodeRef) {
        while (!this.node.getTaskManager().averageIsSet()) {} // We can't hear anything from the registry during this time
        if (this.node.getTaskManager().shouldGiveTasks()) {
            TaskDelivery taskDelivery = new TaskDelivery(this.node.getTaskManager().getTaskDiff(), this.node.getId());
            this.node.getTaskManager().giveTasks(this.node.getTaskManager().getTaskDiff());
            partnerNodeRef.writeToSocket(taskDelivery);
        }
    }

}
