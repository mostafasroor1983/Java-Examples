package module01.solid;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SolidTest {

    @Nested
    class Srp {
        @Test
        void badServiceDoesEverything() {
            var bad = new module01.solid.srp.bad.PaymentService();
            bad.pay("a@b.com", BigDecimal.TEN);
            assertEquals(1, bad.database().size());
            assertEquals(1, bad.sentEmails().size());
            assertEquals(1, bad.log().size());
        }

        @Test
        void goodServiceDelegates() {
            var repo = new module01.solid.srp.good.InMemoryPaymentRepository();
            var notifier = new module01.solid.srp.good.RecordingNotifier();
            var svc = new module01.solid.srp.good.PaymentService(new module01.solid.srp.good.PaymentValidator(), repo, notifier);
            svc.pay("a@b.com", BigDecimal.TEN);
            assertEquals(List.of("a@b.com:10"), repo.rows());
            assertEquals(1, notifier.messages().size());
            assertThrows(IllegalArgumentException.class, () -> svc.pay("nope", BigDecimal.TEN));
            assertEquals(1, repo.rows().size(), "invalid payment must not be persisted");
        }
    }

    @Nested
    class Ocp {
        @Test
        void badCalculatorSilentlyReturnsZeroForTypo() {
            assertEquals(BigDecimal.ZERO, new module01.solid.ocp.bad.FeeCalculator().fee("CRAD", new BigDecimal("100")));
        }

        @Test
        void goodCalculatorFailsLoudlyAndIsExtensible() {
            var calc = new module01.solid.ocp.good.FeeCalculator(List.of(
                    new module01.solid.ocp.good.PercentageFeePolicy("CARD", new BigDecimal("0.015")),
                    new module01.solid.ocp.good.FlatFeePolicy("BANK", new BigDecimal("0.20"))));
            assertEquals(new BigDecimal("1.50"), calc.fee("CARD", new BigDecimal("100")));
            assertEquals(new BigDecimal("0.20"), calc.fee("BANK", new BigDecimal("100")));
            assertThrows(IllegalArgumentException.class, () -> calc.fee("CRAD", BigDecimal.TEN));
        }
    }

    @Nested
    class Lsp {
        @Test
        void badHierarchyBreaksGenericCode() {
            var accounts = List.<module01.solid.lsp.bad.Account>of(
                    new module01.solid.lsp.bad.Account(new BigDecimal("100")),
                    new module01.solid.lsp.bad.FixedDepositAccount(new BigDecimal("100")));
            assertThrows(UnsupportedOperationException.class,
                    () -> new module01.solid.lsp.bad.SweepService().sweep(accounts, BigDecimal.TEN));
        }

        @Test
        void goodHierarchyOnlyAcceptsWithdrawable() {
            var accounts = List.of(
                    new module01.solid.lsp.good.CurrentAccount(new BigDecimal("100")),
                    new module01.solid.lsp.good.CurrentAccount(new BigDecimal("50")));
            assertEquals(new BigDecimal("20"), new module01.solid.lsp.good.SweepService().sweep(accounts, BigDecimal.TEN));
            // FixedDepositAccount is not a WithdrawableAccount: the misuse does not compile.
            module01.solid.lsp.good.ReadableAccount fd = new module01.solid.lsp.good.FixedDepositAccount(BigDecimal.ONE);
            assertFalse(fd instanceof module01.solid.lsp.good.WithdrawableAccount);
        }
    }

    @Nested
    class Isp {
        @Test
        void badGatewayForcesStubs() {
            var g = new module01.solid.isp.bad.RefundOnlyGateway();
            g.refund("r1", BigDecimal.ONE);
            assertThrows(UnsupportedOperationException.class, g::settleBatch);
        }

        @Test
        void goodInterfacesAreSmall() {
            module01.solid.isp.good.Refunder r = new module01.solid.isp.good.RefundService();
            r.refund("r1", BigDecimal.ONE);
            assertThrows(IllegalArgumentException.class, () -> r.refund("r2", BigDecimal.ZERO));
            var full = new module01.solid.isp.good.FullGateway();
            full.settleBatch();
            assertTrue(full.settled());
            assertEquals("CH-1", full.charge("c", BigDecimal.ONE));
        }
    }

    @Nested
    class Dip {
        @Test
        void badServiceHardWiresDetail() {
            var svc = new module01.solid.dip.bad.TransferService();
            svc.transfer("A", "B", BigDecimal.TEN);
            assertTrue(svc.ledger().rows().get(0).startsWith("INSERT INTO"));
        }

        @Test
        void goodServiceAcceptsAnyLedger() {
            var ledger = new module01.solid.dip.good.InMemoryLedger();
            var svc = new module01.solid.dip.good.TransferService(ledger);
            svc.transfer("A", "B", BigDecimal.TEN);
            assertEquals(1, ledger.entries().size());
            assertThrows(IllegalArgumentException.class, () -> svc.transfer("A", "A", BigDecimal.TEN));
            assertThrows(IllegalArgumentException.class, () -> svc.transfer("A", "B", BigDecimal.ZERO));
        }
    }
}
