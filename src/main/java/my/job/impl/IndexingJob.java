package my.job.impl;

import lombok.experimental.SuperBuilder;

import java.util.Collection;

@SuperBuilder
public class IndexingJob extends AbstractJob {

    private Collection<String> payload;
    private Collection<String> indexes;

    @Override
    public boolean run() {
        index();
        System.out.printf("Indexing job %s is done\n", getId());
        return true;
    }

    private void index() {
        //TODO: index payload
    }
}
