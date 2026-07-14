package refactoring;

import java.math.BigDecimal;

/** Phí SWIFT: 5% giá trị giao dịch cộng 50.000 VND. */
public final class SwiftFee implements FeeStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.05");
    private static final BigDecimal FIXED_FEE = new BigDecimal("50000");

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        Account.requirePositive(amount, "amount");
        return amount.multiply(RATE).add(FIXED_FEE);
    }
}
