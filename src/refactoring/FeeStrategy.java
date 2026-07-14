package refactoring;

import java.math.BigDecimal;

/** Chính sách tính phí cho một loại chuyển tiền. */
public interface FeeStrategy {
    BigDecimal calculateFee(BigDecimal amount);
}
