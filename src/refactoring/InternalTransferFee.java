package refactoring;

import java.math.BigDecimal;

/** Chuyển tiền nội bộ được miễn phí. */
public final class InternalTransferFee implements FeeStrategy {
    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        Account.requirePositive(amount, "amount");
        return BigDecimal.ZERO;
    }
}
