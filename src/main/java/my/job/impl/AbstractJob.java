package my.job.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import my.job.Job;
import my.job.JobState;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@SuperBuilder
@RequiredArgsConstructor
public abstract class AbstractJob implements Job {

    private final UUID id = UUID.randomUUID();
    private final byte priority;
    private final long scheduledTime;

    private final AtomicReference<JobState> stateRef = new AtomicReference<>();

    protected AbstractJob() {
        scheduledTime = 0;
        priority = 0;
    }

    @Override
    public JobState getState() {
        return this.stateRef.get();
    }

    @Override
    public void setState(JobState state) {
        this.stateRef.set(state);
    }
}
