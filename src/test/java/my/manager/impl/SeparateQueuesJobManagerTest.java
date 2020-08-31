package my.manager.impl;

import my.job.Job;
import my.job.impl.AwaitingJob;
import my.job.impl.FailureJob;
import my.job.impl.SuccessJob;
import my.manager.JobManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static my.job.JobState.*;
import static org.assertj.core.api.Assertions.assertThat;

class SeparateQueuesJobManagerTest {
    private JobManager manager;
    private ConcurrentLinkedQueue<Job> normalQueue;
    private ConcurrentLinkedQueue<Job> highQueue;
    private ConcurrentLinkedQueue<Job> lowQueue;

    @BeforeEach
    void setUp() {
        normalQueue = new ConcurrentLinkedQueue<>();
        highQueue = new ConcurrentLinkedQueue<>();
        lowQueue = new ConcurrentLinkedQueue<>();
        manager = new SeparateQueuesJobManager(normalQueue, highQueue, lowQueue, false);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    @Test
    void shouldChangeStateConsistently() throws Exception {
        //given
        CountDownLatch latch = new CountDownLatch(1);
        Job job = new AwaitingJob(latch);
        assertThat(job.getState()).isNull();
        //when
        manager.add(job);
        //then
        assertThat(job.getState()).isEqualTo(QUEUED);
        manager.start();
        Thread.sleep(50);
        //then
        assertThat(job.getState()).isEqualTo(RUNNING);
        //when
        latch.countDown();
        Thread.sleep(50);
        //then
        assertThat(job.getState()).isEqualTo(SUCCESS);
    }

    @Test
    void shouldProcessInFIFOOrder() throws Exception {
        //given
        Job job1 = new FailureJob();
        Job job2 = new SuccessJob();
        //when
        manager.add(job1, job2);
        manager.start();
        Thread.sleep(50);
        //then
        assertThat(job1.getState()).isEqualTo(FAILED);
        assertThat(job2.getState()).isEqualTo(SUCCESS);
        assertThat(manager.isRunning()).isTrue();
        //when
        manager.shutdown();
        //then
        assertThat(manager.isRunning()).isFalse();
    }

    @Timeout(2)
    @Test
    void shouldProcessInPriorityOrder() throws Exception {
        //given
        Job job1 = new SuccessJob();
        Job job2 = new SuccessJob((byte) 1, 0);
        //when
        manager.add(job1, job2);
        //then
        assertThat(normalQueue).containsExactly(job1);
        assertThat(highQueue).containsExactly(job2);
        //when
        manager.start();
        Thread.sleep(50);
        //then
        assertThat(job1.getState()).isEqualTo(SUCCESS);
        assertThat(job2.getState()).isEqualTo(SUCCESS);
        assertThat(normalQueue).isEmpty();
        assertThat(highQueue).isEmpty();

    }

    @Timeout(5)
    @Test
    void shouldScheduleJob() throws Exception {
        //given
        int delay = 500;
        Job job1 = new SuccessJob((byte) 0, System.currentTimeMillis() + delay);
        Job job2 = new SuccessJob((byte) 0, 0);
        //when
        manager.add(job1, job2);
        //then
        assertThat(normalQueue).containsExactly(job2);
        //when
        manager.start();
        Thread.sleep(50);
        //then
        assertThat(job1.getState()).isEqualTo(QUEUED);
        assertThat(job2.getState()).isEqualTo(SUCCESS);
        assertThat(normalQueue).isEmpty();
        Thread.sleep(delay);
        assertThat(job1.getState()).isEqualTo(SUCCESS);
    }

    @Timeout(10)
    @Test
    void shouldProcessConcurrently() throws Exception {
        //given
        int total = 10000;
        manager = new SeparateQueuesJobManager(normalQueue, highQueue, lowQueue, true);
        ConcurrentLinkedQueue<Job> results = new ConcurrentLinkedQueue<>();
        //when
        new Thread(() -> {
            for (int i = 0; i < total; i++) {
                Job job = new SuccessJob();
                manager.add(job);
                results.add(job);
            }
        }).start();
        //then
        while (results.size() < total) {
            Thread.sleep(100);
        }
        Job job;
        int it = 0;
        while ((job = results.peek()) != null) {
            if (job.getState() == SUCCESS) {
                results.poll();
                it++;
            }
        }
        assertThat(results).isEmpty();
        assertThat(it).isEqualTo(total);
    }

}