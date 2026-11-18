package module18.ch02;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Item 5 - Prefer dependency injection to hardwiring resources.
 *
 * <p>A fee service that does {@code new ProdFeeSchedule()} and {@code LocalDate.now()} inside can only be tested
 * against production configuration and today's date. Passing the {@link FeeSchedule} and a {@link Clock} through the
 * constructor makes it testable, configurable per market, and honest about what it depends on. No framework needed.
 */
public final class Item05DependencyInjection {
    private Item05DependencyInjection() {
    }

    @FunctionalInterface
    public interface FeeSchedule {
        BigDecimal feeFor(BigDecimal amount, boolean weekend);
    }

    public static final class FeeService {
        private final FeeSchedule schedule;
        private final Clock clock;

        public FeeService(FeeSchedule schedule, Clock clock) {
            this.schedule = Objects.requireNonNull(schedule, "schedule");
            this.clock = Objects.requireNonNull(clock, "clock");
        }

        public BigDecimal fee(BigDecimal amount) {
            DayOfWeek day = LocalDate.now(clock).getDayOfWeek();
            return schedule.feeFor(amount, day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
        }
    }
}
