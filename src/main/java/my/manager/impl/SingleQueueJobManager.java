package my.manager.impl;

import lombok.Getter;
import my.job.Job;
import my.manager.JobManager;

import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static my.job.JobState.*;

public class SingleQueueJobManager implements JobManager {

    private final PriorityQueue<Job> queue;
    @Getter
    private volatile boolean isRunning;
    private Executor executor;

    public SingleQueueJobManager(PriorityQueue<Job> queue, boolean autostart) {
        this.queue = queue;
        isRunning = autostart;
        if (autostart)
            launch();
    }

    @Override
    public void add(Job... jobs) {
        synchronized (queue) {
            for (Job job : jobs) {
                queue.add(job);
                job.setState(QUEUED);
            }
            queue.notifyAll();
        }
    }

    @Override
    public void start() {
        isRunning = true;
        launch();
    }

    private void launch() {
        executor = Executors.newWorkStealingPool();
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (isRunning) {
            Job job;

            synchronized (queue) {
                job = queue.peek();
                if (job != null) {
                    long time = timeToFire(job);
                    if (time > 0) {
                        try {
                            queue.wait(time);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    job = queue.poll();
                }
            }
            if (job != null) {
                execute(job);
            }
        }
    }

    private long timeToFire(Job job) {
        return job.getScheduledTime() - System.currentTimeMillis();
    }

    private void execute(Job job) {
        while (!job.getStateRef().compareAndSet(QUEUED, RUNNING));

        CompletableFuture.supplyAsync(job::run, executor)
                .whenComplete((result, exception) -> {
                    if (exception != null || !result) {
                        job.setState(FAILED);
                    } else {
                        job.setState(SUCCESS);
                    }
                });
    }


    @Override
    public void shutdown() {
        isRunning = false;
        synchronized (queue) {
            queue.notifyAll();
        }
    }
}
