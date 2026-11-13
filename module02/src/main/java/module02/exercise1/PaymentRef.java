package module02.exercise1;

/**
 * BUGGY key: equals() compares merchant+order, hashCode() is inherited from Object.
 * The ledger "loses" payments on lookup even though they were stored.
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
        if (!(o instanceof PaymentRef)) return false;
        PaymentRef p = (PaymentRef) o;
        return merchantId.equals(p.merchantId) && orderId.equals(p.orderId);
    }
    // BUG: no hashCode()
}
