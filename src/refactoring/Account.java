package refactoring;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Aggregate nhỏ đại diện cho số dư một tài khoản. Việc thay đổi số dư trong
 * một giao dịch phải đi qua {@link TransferService} để debit và credit là
 * một thao tác nguyên tử trong phạm vi tiến trình.
 */
public final class Account {
    private final String accountId;
    private final ReentrantLock balanceLock = new ReentrantLock();
    private BigDecimal balance;

    public Account(String accountId, BigDecimal openingBalance) {
        this.accountId = requireText(accountId, "accountId");
        this.balance = requireNonNegative(openingBalance, "openingBalance");
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        balanceLock.lock();
        try {
            return balance;
        } finally {
            balanceLock.unlock();
        }
    }

    void lockBalance() {
        balanceLock.lock();
    }

    void unlockBalance() {
        balanceLock.unlock();
    }

    BigDecimal balanceWhileLocked() {
        return balance;
    }

    void deductWhileLocked(BigDecimal value) {
        requirePositive(value, "value");
        if (balance.compareTo(value) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for account " + accountId);
        }
        balance = balance.subtract(value);
    }

    void addWhileLocked(BigDecimal value) {
        requirePositive(value, "value");
        balance = balance.add(value);
    }

    static BigDecimal requirePositive(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
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
