package my.job.impl;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class LogJob extends AbstractJob {
    @Override
    public boolean run() {
        System.out.println("Job is done");
        return true;
    }
}
