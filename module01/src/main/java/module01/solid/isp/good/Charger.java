package module01.solid.isp.good;

import java.math.BigDecimal;

public interface Charger {
    String charge(String card, BigDecimal amount);
}
