package refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Tách việc phát event khỏi nghiệp vụ chuyển tiền. Executor có thể là một
 * message broker adapter hoặc thread pool để các tác vụ như OTP không chặn
 * luồng chuyển tiền. Lỗi listener được cô lập, không rollback giao dịch đã
 * hoàn tất.
 */
public final class TransactionEventPublisher {
    private final List<TransactionEventListener> listeners;
    private final Executor executor;
    private final Consumer<RuntimeException> errorHandler;

    /** Publisher đồng bộ, thuận tiện cho unit test. */
    public TransactionEventPublisher(List<TransactionEventListener> listeners) {
        this(listeners, Runnable::run, error -> { });
    }

    public TransactionEventPublisher(
            List<TransactionEventListener> listeners,
            Executor executor,
            Consumer<RuntimeException> errorHandler) {
        Objects.requireNonNull(listeners, "listeners must not be null");
        this.listeners = Collections.unmodifiableList(
                new ArrayList<TransactionEventListener>(listeners));
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler must not be null");
    }

    public void publish(final Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        for (final TransactionEventListener listener : listeners) {
            if (listener == null) {
                throw new IllegalArgumentException("listeners must not contain null");
            }
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listener.handle(transaction);
                        } catch (RuntimeException ex) {
                            reportFailure(ex);
                        }
                    }
                });
            } catch (RuntimeException ex) {
                // Ví dụ: executor bị đầy hoặc đã shutdown. Số dư đã commit
                // vẫn là kết quả hợp lệ, vì vậy chỉ ghi nhận lỗi phát event.
                reportFailure(ex);
            }
        }
    }

    private void reportFailure(RuntimeException exception) {
        try {
            errorHandler.accept(exception);
        } catch (RuntimeException ignored) {
            // Reporting failure must not change the completed transfer outcome.
        }
    }
}
