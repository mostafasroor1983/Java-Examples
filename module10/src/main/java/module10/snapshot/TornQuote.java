package module10.snapshot;

/**
 * BAD on purpose: bid and ask are each volatile, so each individual read is fresh, but a reader can see the NEW bid
 * with the OLD ask (or vice versa). With a widening move that can yield bid > ask, a crossed market, and a customer
 * quoted a price that never existed. Volatile gives visibility per field, not atomicity across fields.
 */
public final class TornQuote {
    private volatile long bidMicros;
    private volatile long askMicros;

    public void update(long bid, long ask) {
        bidMicros = bid;  // a reader may run between these two writes
        askMicros = ask;
    }

    public long[] read() {
        return new long[]{bidMicros, askMicros};
    }
}
