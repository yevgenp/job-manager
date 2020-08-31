package my.job;

import java.util.concurrent.atomic.AtomicReference;

public interface Job {

    byte MAX_PRIORITY = Byte.MAX_VALUE;
    byte MIN_PRIORITY = Byte.MIN_VALUE;

    byte getPriority();

    /**
     * @return scheduled time in milliseconds
     */
    long getScheduledTime();

    JobState getState();

    /**
     * Thread safe backing implementation
     */
    AtomicReference<JobState> getStateRef();

    void setState(JobState state);

    /**
     * @return false (or throws an exception) if job fails
     */
    boolean run();
}
