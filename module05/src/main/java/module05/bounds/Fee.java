package module05.bounds;

import java.math.BigDecimal;

public record Fee(BigDecimal value) implements Summable<Fee>, Comparable<Fee> {
    @Override
    public Fee plus(Fee other) {
        return new Fee(value.add(other.value));
    }

    @Override
    public int compareTo(Fee o) {
        return value.compareTo(o.value);
    }
}
