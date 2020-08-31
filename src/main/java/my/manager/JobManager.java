package my.manager;

import my.job.Job;

public interface JobManager extends Runnable
{
    void add(Job... jobs);

    boolean isRunning();

    void start();

    void shutdown();
}
