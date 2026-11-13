package module02.mutablekey;

import java.util.Objects;

/** BAD: mutable object used as a HashMap key. Mutating after insertion orphans the entry. */
public class MutablePaymentKey {
    private String merchantId;
    private String orderId;

    public MutablePaymentKey(String merchantId, String orderId) {
        this.merchantId = merchantId;
        this.orderId = orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MutablePaymentKey)) return false;
        MutablePaymentKey k = (MutablePaymentKey) o;
        return merchantId.equals(k.merchantId) && orderId.equals(k.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merchantId, orderId);
    }
}
