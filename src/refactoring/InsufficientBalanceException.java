package refactoring;

/** Thông báo số dư khả dụng không đủ cho cả số tiền chuyển và phí. */
public final class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
