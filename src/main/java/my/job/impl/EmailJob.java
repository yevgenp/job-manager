package my.job.impl;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import my.email.EmailSender;

@Getter
@SuperBuilder
public class EmailJob extends AbstractJob {

    private EmailSender sender;
    private String address;
    private String body;
    private String from;
    private String subject;

    @Override
    public boolean run() {
        validate();
        System.out.printf("Sending email from %s to %s\n", from, address);
        sender.send(address, body, from, subject);
        return true;
    }

    private void validate() {
        //validate all necessary fields are set
    }
}
