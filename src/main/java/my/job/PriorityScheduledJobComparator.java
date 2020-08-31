package my.job;

import java.util.Comparator;

public class PriorityScheduledJobComparator implements Comparator<Job> {
    @Override
    public int compare(Job j1, Job j2) {
        if (j1.getPriority() != j2.getPriority())
            return Byte.compare(j2.getPriority(), j1.getPriority());

        return Long.compare(j1.getScheduledTime(), j2.getScheduledTime());
    }
}
