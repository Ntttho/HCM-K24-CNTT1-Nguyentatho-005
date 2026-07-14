package refactoring;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Điều phối nghiệp vụ chuyển tiền lõi: chọn biểu phí, kiểm tra số dư, cập
 * nhật hai tài khoản và tạo transaction. Không chứa logic OTP/Core Banking.
 */
public final class TransferService {
    private final FeeStrategyFactory feeFactory;
    private final TransactionEventPublisher eventPublisher;

    public TransferService(FeeStrategyFactory feeFactory, TransactionEventPublisher eventPublisher) {
        this.feeFactory = Objects.requireNonNull(feeFactory, "feeFactory must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    public Transaction processTransfer(
            Account fromAccount,
            Account toAccount,
            BigDecimal amount,
            String transferType) {
        Objects.requireNonNull(fromAccount, "fromAccount must not be null");
        Objects.requireNonNull(toAccount, "toAccount must not be null");
        Account.requirePositive(amount, "amount");

        if (fromAccount.getAccountId().equals(toAccount.getAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        FeeStrategy feeStrategy = feeFactory.getStrategy(transferType);
        BigDecimal fee = requireNonNegative(feeStrategy.calculateFee(amount), "calculated fee");
        BigDecimal totalDebit = amount.add(fee);
        Transaction transaction = transferAtomically(fromAccount, toAccount, amount, fee, totalDebit);

        // Trong ứng dụng có database, bước này phải chạy sau transaction commit
        // (hoặc ghi Transactional Outbox) để không phát event cho giao dịch rollback.
        eventPublisher.publish(transaction);
        return transaction;
    }

    private Transaction transferAtomically(
            Account fromAccount,
            Account toAccount,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebit) {
        Account first = fromAccount.getAccountId().compareTo(toAccount.getAccountId()) < 0
                ? fromAccount : toAccount;
        Account second = first == fromAccount ? toAccount : fromAccount;

        first.lockBalance();
        second.lockBalance();
        try {
            if (fromAccount.balanceWhileLocked().compareTo(totalDebit) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient balance for amount and fee on account " + fromAccount.getAccountId());
            }
            fromAccount.deductWhileLocked(totalDebit);
            toAccount.addWhileLocked(amount);
            return new Transaction(fromAccount, toAccount, amount, fee);
        } finally {
            second.unlockBalance();
            first.unlockBalance();
        }
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(field + " must not be negative");
        }
        return value;
    }
}
