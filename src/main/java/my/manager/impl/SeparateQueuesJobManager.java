package my.manager.impl;

import lombok.Getter;
import my.job.Job;
import my.manager.JobManager;

import java.util.concurrent.*;

import static my.job.JobState.*;

public class SeparateQueuesJobManager implements JobManager {

    private final ConcurrentLinkedQueue<Job> normalPriorityQueue;
    private final ConcurrentLinkedQueue<Job> highPriorityQueue;
    private final ConcurrentLinkedQueue<Job> lowPriorityQueue;
    @Getter
    private volatile boolean isRunning;
    private Executor executor;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public SeparateQueuesJobManager(ConcurrentLinkedQueue<Job> normalPriorityQueue,
                                    ConcurrentLinkedQueue<Job> highPriorityQueue,
                                    ConcurrentLinkedQueue<Job> lowPriorityQueue,
                                    boolean autostart) {
        this.normalPriorityQueue = normalPriorityQueue;
        this.highPriorityQueue = highPriorityQueue;
        this.lowPriorityQueue = lowPriorityQueue;
        isRunning = autostart;
        if (autostart)
            launch();
    }

    @Override
    public void add(Job... jobs) {
        for (Job job : jobs) {
            if (job.getScheduledTime() != 0) {
                job.setState(QUEUED);
                schedule(job);
                continue;
            }
            if (job.getPriority() > 0) {
                highPriorityQueue.add(job);
                job.setState(QUEUED);
                continue;
            }
            if (job.getPriority() < 0) {
                lowPriorityQueue.add(job);
                job.setState(QUEUED);
                continue;
            }
            normalPriorityQueue.add(job);
            job.setState(QUEUED);
        }
    }

    @Override
    public void start() {
        isRunning = true;
        launch();
    }

    @Override
    public void run() {
        while (isRunning) {
            Job job;

            if ((job = highPriorityQueue.poll()) != null) {
                execute(job);
                continue;
            }
            if ((job = normalPriorityQueue.poll()) != null) {
                execute(job);
                continue;
            }
            if ((job = lowPriorityQueue.poll()) != null) {
                execute(job);
            }
        }
    }

    private void launch() {
        executor = Executors.newWorkStealingPool(computeExecutorsNumber());
        new Thread(this).start();
    }

    private int computeExecutorsNumber() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        return availableProcessors > 2 ? availableProcessors - 2 : 1;
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

    private long timeToFire(Job job) {
        return job.getScheduledTime() - System.currentTimeMillis();
    }

    private void execute(Job job) {
        while (!job.getStateRef().compareAndSet(QUEUED, RUNNING)) ;

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
        scheduler.shutdown();
    }
}
