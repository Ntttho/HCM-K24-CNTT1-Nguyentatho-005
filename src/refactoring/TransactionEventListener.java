package refactoring;

/** Tác vụ phụ được gọi sau khi số dư và giao dịch lõi đã được hoàn tất. */
public interface TransactionEventListener {
    void handle(Transaction transaction);
}
