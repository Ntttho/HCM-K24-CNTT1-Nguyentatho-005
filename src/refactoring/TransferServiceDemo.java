package refactoring;

import java.math.BigDecimal;
import java.util.Arrays;

/** Chạy thử độc lập: javac -d out refactoring/*.java; java -cp out refactoring.TransferServiceDemo */
public final class TransferServiceDemo {
    private TransferServiceDemo() {
    }

    public static void main(String[] args) {
        Account source = new Account("ACC-001", new BigDecimal("3000000"));
        Account destination = new Account("ACC-002", BigDecimal.ZERO);

        TransferService service = new TransferService(
                FeeStrategyFactory.withDefaultStrategies(),
                new TransactionEventPublisher(Arrays.asList(new OtpListener(), new CoreBankListener())));

        Transaction transaction = service.processTransfer(
                source, destination, new BigDecimal("1500000"), "NAPAS_247");

        System.out.println("Status: " + transaction.getStatus());
        System.out.println("Fee: " + transaction.getFee());
        System.out.println("Source balance: " + source.getBalance());
        System.out.println("Destination balance: " + destination.getBalance());
    }
}
