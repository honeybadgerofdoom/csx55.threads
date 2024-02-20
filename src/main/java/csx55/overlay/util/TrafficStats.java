package csx55.overlay.util;

public class TrafficStats {

    private Integer generated, pushed, pulled, completed = 0;

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
        String header = String.format("| %17s | %17s | %17s | %17s |", "Generated", "Pushed", "Pulled", "Completed");
        return header + "\n" + this;
    }

    @Override
    public String toString() {
        return String.format("| %17d | %17d | %17d | %17d |", generated, pushed, pulled, completed);
    }

}
