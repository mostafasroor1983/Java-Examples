package module02.exercise1;

/**
 * BUGGY key: equals() compares merchant+order, hashCode() is inherited from Object.
 * The ledger "loses" payments on lookup even though they were stored.
 * 2 equals objects will have different hashCode => inconsistency => loses payment on lookup
 */
public final class PaymentRef {
    private final String merchantId;
    private final String orderId;

    public PaymentRef(String merchantId, String orderId) {
        this.merchantId = merchantId;
        this.orderId = orderId;
    }

    public String merchantId() {
        return merchantId;
    }

    public String orderId() {
        return orderId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PaymentRef p)) return false;
        return merchantId.equals(p.merchantId) && orderId.equals(p.orderId);
    }
    // BUG: no hashCode()
}
