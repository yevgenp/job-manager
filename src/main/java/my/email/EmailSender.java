package my.email;

public interface EmailSender {
    void send(String to, String body, String from, String subject);
}
