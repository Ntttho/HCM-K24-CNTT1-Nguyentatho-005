package refactoring;

import java.math.BigDecimal;

/** Biểu phí NAPAS 247 theo ngưỡng 2.000.000 VND. */
public final class Napas247Fee implements FeeStrategy {
    private static final BigDecimal THRESHOLD = new BigDecimal("2000000");
    private static final BigDecimal LOW_TIER_FEE = new BigDecimal("2000");
    private static final BigDecimal HIGH_TIER_FEE = new BigDecimal("5000");

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        Account.requirePositive(amount, "amount");
        return amount.compareTo(THRESHOLD) < 0 ? LOW_TIER_FEE : HIGH_TIER_FEE;
    }
}
