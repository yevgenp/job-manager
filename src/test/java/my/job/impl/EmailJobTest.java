package my.job.impl;

import my.email.EmailSender;
import my.job.Job;
import my.job.PriorityScheduledJobComparator;
import my.manager.JobManager;
import my.manager.impl.SingleQueueJobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.PriorityQueue;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailJobTest {
    final PriorityScheduledJobComparator defaultComparator = new PriorityScheduledJobComparator();

    private JobManager manager;
    private PriorityQueue<Job> queue;
    private @Mock EmailSender emailSender;

    @BeforeEach
    void setUp() {
        queue = new PriorityQueue<>(defaultComparator);
        manager = new SingleQueueJobManager(queue, true);
    }

    @Test
    void shouldRun() throws Exception {
        //given
        Job job = EmailJob.builder()
                .sender(emailSender)
                .address("to").body("body")
                .from("from").subject("subj")
                .build();
        //when
        manager.add(job);
        Thread.sleep(50);
        //then
        verify(emailSender).send("to", "body", "from", "subj");

    }
}