package module01.solid.srp.bad;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * BAD: one class validates, persists, formats emails and logs. Four reasons to change.
 * A change to the email template forces a redeploy of the payment logic.
 */
public class PaymentService {
    private final List<String> database = new ArrayList<>();
    private final List<String> sentEmails = new ArrayList<>();
    private final List<String> log = new ArrayList<>();

    public void pay(String customerEmail, BigDecimal amount) {
        // validation
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (customerEmail == null || !customerEmail.contains("@")) {
            throw new IllegalArgumentException("bad email");
        }
        // persistence
        database.add("INSERT INTO payments VALUES ('" + customerEmail + "', " + amount + ")");
        // notification
        sentEmails.add("To: " + customerEmail + "\nSubject: Payment received\nAmount: " + amount);
        // logging
        log.add("payment of " + amount + " for " + customerEmail);
    }

    public List<String> database() { return database; }
    public List<String> sentEmails() { return sentEmails; }
    public List<String> log() { return log; }
}
