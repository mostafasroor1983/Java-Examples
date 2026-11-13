package module01.composition;

import java.math.BigDecimal;
import java.util.Set;

/** Reusable rule factories; each is small, testable and combinable. */
public final class StandardChecks {
    private StandardChecks() {
    }

    public static FraudCheck amountAbove(BigDecimal limit) {
        return (customer, amount, country) ->
                amount.compareTo(limit) > 0 ? "amount " + amount + " exceeds limit " + limit : null;
    }

    public static FraudCheck blockedCountries(Set<String> blocked) {
        Set<String> copy = Set.copyOf(blocked);
        return (customer, amount, country) ->
                copy.contains(country) ? "country " + country + " is blocked" : null;
    }

    public static FraudCheck blockedCustomers(Set<String> blocked) {
        Set<String> copy = Set.copyOf(blocked);
        return (customer, amount, country) ->
                copy.contains(customer) ? "customer " + customer + " is blocked" : null;
    }
}
