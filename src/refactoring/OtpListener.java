package refactoring;

/** Adapter mẫu cho dịch vụ gửi OTP/biên nhận giao dịch. */
public final class OtpListener implements TransactionEventListener {
    @Override
    public void handle(Transaction transaction) {
        System.out.println("Sending OTP for transaction " + transaction.getTransactionId());
    }
}
