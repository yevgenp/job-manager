package my.manager.impl;

import lombok.Getter;
import my.job.Job;
import my.manager.JobManager;

import java.util.PriorityQueue;
import java.util.concurrent.*;

import static my.job.JobState.*;

public class SingleQueueJobManager implements JobManager {

    private final PriorityQueue<Job> queue;
    @Getter
    private volatile boolean isRunning;
    private Executor executor;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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
                job = queue.poll();
                if (job != null) {
                    if (timeToFire(job) > 0) {
                        schedule(job);
                        continue;
                    }
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

    private void schedule(Job job) {
        scheduler.schedule(() -> {
            while (!job.getStateRef().compareAndSet(QUEUED, RUNNING)) ;
            try {
                if (job.run())
                    job.setState(SUCCESS);
                else
                    job.setState(FAILED);
            } catch (Exception ex) {
                job.setState(FAILED);
            }
        }, timeToFire(job), TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        isRunning = false;
        scheduler.shutdown();
        synchronized (queue) {
            queue.notifyAll();
        }
    }
}
