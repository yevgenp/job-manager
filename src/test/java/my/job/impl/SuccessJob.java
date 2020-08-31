package my.job.impl;

public class SuccessJob extends AbstractJob {

    public SuccessJob() {
    }
     public SuccessJob(byte priority, long scheduledTime) {
        super(priority, scheduledTime);
    }

    @Override
    public boolean run() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException ignored) {}
        return true;
    }
}