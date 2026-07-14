package refactoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Bản ghi bất biến của một giao dịch đã hoàn tất trong module này. */
public final class Transaction {
    private final String transactionId;
    private final String fromAccountId;
    private final String toAccountId;
    private final BigDecimal amount;
    private final BigDecimal fee;
    private final TransactionStatus status;
    private final Instant createdAt;

    public Transaction(Account fromAccount, Account toAccount, BigDecimal amount, BigDecimal fee) {
        this(UUID.randomUUID().toString(), fromAccount, toAccount, amount, fee,
                TransactionStatus.SUCCESS, Instant.now());
    }

    Transaction(
            String transactionId,
            Account fromAccount,
            Account toAccount,
            BigDecimal amount,
            BigDecimal fee,
            TransactionStatus status,
            Instant createdAt) {
        this.transactionId = requireText(transactionId, "transactionId");
        this.fromAccountId = Objects.requireNonNull(fromAccount, "fromAccount must not be null").getAccountId();
        this.toAccountId = Objects.requireNonNull(toAccount, "toAccount must not be null").getAccountId();
        this.amount = Account.requirePositive(amount, "amount");
        this.fee = requireNonNegative(fee, "fee");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
