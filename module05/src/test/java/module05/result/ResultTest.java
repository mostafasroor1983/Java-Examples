package module05.result;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void mapFlatMapAndRecovery() {
        Result<Integer, String> ok = Result.ok(2);
        Result<Integer, String> err = Result.err("boom");
        assertEquals(Result.ok(4), ok.map(x -> x * 2));
        assertEquals(Result.err("boom"), err.map(x -> x * 2));
        assertEquals(Result.ok("2"), ok.flatMap(x -> Result.ok(String.valueOf(x))));
        assertEquals(Result.err("BOOM"), err.mapErr(String::toUpperCase));
        assertEquals(2, ok.orElse(0));
        assertEquals(0, err.orElse(0));
        assertEquals(4, err.orElseGet(String::length));
        assertThrows(NoSuchElementException.class, err::orElseThrow);
        assertTrue(err.toOptional().isEmpty());
        assertThrows(NullPointerException.class, () -> Result.ok(null));
    }

    @Test
    void sideEffectsAndAttempt() {
        AtomicInteger okCount = new AtomicInteger(), errCount = new AtomicInteger();
        Result.<Integer, String>ok(1).ifOk(v -> okCount.incrementAndGet()).ifErr(e -> errCount.incrementAndGet());
        Result.<Integer, String>err("x").ifOk(v -> okCount.incrementAndGet()).ifErr(e -> errCount.incrementAndGet());
        assertEquals(1, okCount.get());
        assertEquals(1, errCount.get());
        Result<Integer, Exception> parsed = Result.attempt(() -> Integer.parseInt("12"));
        Result<Integer, Exception> failed = Result.attempt(() -> Integer.parseInt("x"));
        assertEquals(12, parsed.orElseThrow());
        assertTrue(failed.isErr());
    }

    @Test
    void instanceofPatternsOverSealedResult() {
        // Java 17: pattern matching for instanceof (switch patterns arrive in Java 21, see module07/13).
        Result<Integer, String> r = Result.err("nope");
        String s;
        if (r instanceof Result.Ok<Integer, String> ok) s = "ok " + ok.value();
        else if (r instanceof Result.Err<Integer, String> e) s = "err " + e.error();
        else throw new AssertionError("sealed: unreachable");
        assertEquals("err nope", s);
    }

    @Test
    void transferPipeline() {
        TransferService svc = new TransferService();
        svc.open("A", new BigDecimal("100"));
        svc.open("B", BigDecimal.ZERO);
        assertEquals(Result.err(TransferService.TransferError.INVALID_AMOUNT), svc.transfer("A", "B", BigDecimal.ZERO));
        assertEquals(Result.err(TransferService.TransferError.SAME_ACCOUNT), svc.transfer("A", "A", BigDecimal.ONE));
        assertEquals(Result.err(TransferService.TransferError.UNKNOWN_ACCOUNT), svc.transfer("A", "Z", BigDecimal.ONE));
        assertEquals(Result.err(TransferService.TransferError.INSUFFICIENT_FUNDS), svc.transfer("A", "B", new BigDecimal("500")));
        var ok = svc.transfer("A", "B", new BigDecimal("40"));
        assertTrue(ok.isOk());
        assertEquals(new BigDecimal("60"), svc.balance("A"));
        assertEquals(new BigDecimal("40"), svc.balance("B"));
    }
}
