package module02.immutability.bad;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * BAD: public setters, exposes a mutable Date and the internal list.
 * A quote shared between threads can change under a reader's feet.
 */
public class MutableFxQuote {
    private String pair;
    private BigDecimal rate;
    private Date validUntil;
    private List<String> venues;

    public MutableFxQuote(String pair, BigDecimal rate, Date validUntil, List<String> venues) {
        this.pair = pair;
        this.rate = rate;
        this.validUntil = validUntil;
        this.venues = venues;
    }

    public String getPair() { return pair; }
    public void setPair(String pair) { this.pair = pair; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public Date getValidUntil() { return validUntil; }       // leaks mutable Date
    public List<String> getVenues() { return venues; }       // leaks internal list
}
