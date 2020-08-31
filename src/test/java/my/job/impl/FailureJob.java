package my.job.impl;

public class FailureJob extends AbstractJob {

    public FailureJob() {
    }
     public FailureJob(byte priority, long scheduledTime) {
        super(priority, scheduledTime);
    }

    @Override
    public boolean run() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException ignored) {}
        return false;
    }
}