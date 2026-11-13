package module01.solid.isp.good;

import java.math.BigDecimal;

public interface Refunder {
    void refund(String reference, BigDecimal amount);
}
