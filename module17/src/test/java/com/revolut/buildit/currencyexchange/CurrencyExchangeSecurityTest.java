package com.revolut.buildit.currencyexchange;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression: a caller must not be able to execute a forged quote (Quote is a public record). */
class CurrencyExchangeSecurityTest {

    @Test
    void forgedQuoteWithCurrentVersionIsRejectedAndMovesNoMoney() {
        CurrencyExchange fx = new CurrencyExchange(new BigDecimal("50"));
        fx.publishRates(Map.of("GBP", new BigDecimal("1.25"), "EUR", new BigDecimal("1.10")));
        fx.deposit("c1", "GBP", new BigDecimal("100"));
        CurrencyExchange.Quote honest = fx.quote("GBP", "EUR", new BigDecimal("100")).orElseThrow();

        var forged = new CurrencyExchange.Quote("GBP", "EUR", honest.amount(), honest.rate(),
                new BigDecimal("1000000.00"), honest.rateVersion());
        assertThat(fx.exchange("c1", forged)).isEqualTo(CurrencyExchange.Outcome.INVALID_QUOTE);
        var forgedRate = new CurrencyExchange.Quote("GBP", "EUR", honest.amount(), honest.rate().add(BigDecimal.ONE),
                honest.result(), honest.rateVersion());
        assertThat(fx.exchange("c1", forgedRate)).isEqualTo(CurrencyExchange.Outcome.INVALID_QUOTE);
        assertThat(fx.balance("c1", "GBP")).isEqualByComparingTo("100");
        assertThat(fx.balance("c1", "EUR")).isEqualByComparingTo("0");

        assertThat(fx.exchange("c1", honest)).as("honest quote still executes").isEqualTo(CurrencyExchange.Outcome.COMPLETED);
        assertThat(fx.balance("c1", "EUR")).isEqualByComparingTo(honest.result());
    }
}
