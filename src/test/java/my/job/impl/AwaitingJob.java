package my.job.impl;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.CountDownLatch;

@RequiredArgsConstructor
public class AwaitingJob extends AbstractJob {
    private final CountDownLatch latch;

    @Override
    public boolean run() {
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
        return true;
    }
}