package module01.solid.ocp.good;

import java.math.BigDecimal;

public interface FeePolicy {
    boolean supports(String type);

    BigDecimal fee(BigDecimal amount);
}
