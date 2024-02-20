package csx55.overlay.util;

public class TrafficStats {

    private Integer generated, pushed, pulled, completed;
    private final TableHelper tableHelper = new TableHelper(12, 4);

    public TrafficStats() {
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

    public String table() {
        String header = String.format("| %10s | %10s | %10s | %10s |", "Generated", "Pushed", "Pulled", "Completed");
        return this.tableHelper.formatTable(header, this.toString());
    }

    @Override
    public String toString() {
        return String.format("| %10d | %10d | %10d | %10d |", generated, pushed, pulled, completed);
    }

}
