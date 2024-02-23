package csx55.threads.util;

public class ComputeNodeTaskStats {

    private Integer generated, pushed, pulled, completed;

    public ComputeNodeTaskStats() {
        generated = 0;
        pushed = 0;
        pulled = 0;
        completed = 0;
    }

    public void reset() {
        generated = 0;
        pushed = 0;
        pulled = 0;
        completed = 0;
    }

    public void updateGenerated(int numTasks) {
        synchronized (generated) {
            generated += numTasks;
        }
    }

    public void updatePushed(int numTasks) {
        synchronized (pushed) {
            pushed += numTasks;
        }
    }

    public void absorb(int numTasks) {
        synchronized (pushed) {
            pushed -= numTasks;
        }
    }

    public void updatePulled(int numTasks) {
        synchronized (pulled) {
            pulled += numTasks;
        }
    }

    public void incrementCompleted() {
        synchronized (completed) {
            completed++;
        }
    }

    public Integer getGenerated() {
        return generated;
    }

    public Integer getPushed() {
        return pushed;
    }

    public Integer getPulled() {
        return pulled;
    }

    public Integer getCompleted() {
        return completed;
    }

    @Override
    public String toString() {
        return String.format("| %10d | %10d | %10d | %10d |", generated, pushed, pulled, completed);
    }

}
