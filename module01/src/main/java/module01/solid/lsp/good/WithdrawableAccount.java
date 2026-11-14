package module01.solid.lsp.good;

import java.math.BigDecimal;

/** Only types that truly honour "withdraw reduces balance" implement this. */
/** لا تنفّذ ذلك إلا الأنواع التي تلتزم فعلاً بأن «السحب يُنقص الرصيد». */
public interface WithdrawableAccount extends ReadableAccount {
    void withdraw(BigDecimal amount);
}
