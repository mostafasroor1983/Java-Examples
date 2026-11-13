package module01.solid.srp.good;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class RecordingNotifier implements Notifier {
    private final List<String> messages = new ArrayList<>();

    @Override
    public void paymentReceived(String customerEmail, BigDecimal amount) {
        messages.add(customerEmail + " <- " + amount);
    }

    public List<String> messages() {
        return List.copyOf(messages);
    }
}
