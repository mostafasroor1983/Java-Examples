package module01.interfaces;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public final class InMemoryFxRateProvider implements FxRateProvider {
    private final Map<String, BigDecimal> rates;

    public InMemoryFxRateProvider(Map<String, BigDecimal> rates) {
        this.rates = Map.copyOf(rates);
    }

    @Override
    public Optional<BigDecimal> rate(String base, String quote) {
        return Optional.ofNullable(rates.get(base + "/" + quote));
    }
}
