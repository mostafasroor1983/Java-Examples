package module08.validation;

import module08.validation.PaymentRequestValidator.Request;
import module08.validation.PaymentRequestValidator.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRequestValidatorTest {
    private static final String DE = "DE89 3704 0044 0532 0130 00";  // public example IBAN
    private static final String GB = "GB82WEST12345698765432";
    private final PaymentRequestValidator v = new PaymentRequestValidator();

    @Test
    void validRequest() {
        assertInstanceOf(Result.Valid.class, v.validate(new Request(DE, GB, new BigDecimal("10.50"), "EUR", "Invoice 42")));
        assertInstanceOf(Result.Valid.class, v.validate(new Request(DE, GB, new BigDecimal("10.500"), "EUR", null)),
                "trailing zeros are not extra precision");
    }

    @Test
    void collectsAllErrorsAtOnceWithoutEchoingValues() {
        var r = (Result.Invalid) v.validate(new Request("DE00 1234", DE, new BigDecimal("-1.001"), "XXX", "<script>"));
        var codes = r.errors().stream().map(e -> e.field() + ":" + e.code()).toList();
        assertEquals(java.util.List.of("debtorIban:INVALID_IBAN", "currency:UNSUPPORTED", "amount:NOT_POSITIVE",
                "amount:PRECISION", "reference:INVALID"), codes);
        r.errors().forEach(e -> {
            assertFalse(e.hint().contains("<script>"));
            assertFalse(e.hint().contains("DE00"));
        });
    }

    @Test
    void sameAccountAndLimits() {
        var r = (Result.Invalid) v.validate(new Request(DE, "de89370400440532013000", new BigDecimal("2000000"), "EUR", ""));
        assertEquals(java.util.List.of("SAME_ACCOUNT", "LIMIT"), r.errors().stream().map(PaymentRequestValidator.FieldError::code).toList());
        assertInstanceOf(Result.Invalid.class, v.validate(null));
    }

    @Test
    void ibanChecksum() {
        assertTrue(PaymentRequestValidator.ibanChecksumValid("DE89370400440532013000"));
        assertFalse(PaymentRequestValidator.ibanChecksumValid("DE89370400440532013001"));
    }
}
