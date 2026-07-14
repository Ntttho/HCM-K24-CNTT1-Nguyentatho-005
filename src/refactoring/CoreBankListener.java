package refactoring;

/** Adapter mẫu cho việc đồng bộ sang Core Banking. */
public final class CoreBankListener implements TransactionEventListener {
    @Override
    public void handle(Transaction transaction) {
        System.out.println("Syncing transaction " + transaction.getTransactionId() + " to Core Banking");
    }
}
