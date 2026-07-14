package refactoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Registry các chính sách phí. Muốn thêm loại chuyển tiền mới chỉ cần đưa
 * strategy mới vào factory khi khởi tạo, không cần sửa TransferService.
 */
public final class FeeStrategyFactory {
    private final Map<String, FeeStrategy> strategies;

    public FeeStrategyFactory(Map<String, FeeStrategy> strategies) {
        Objects.requireNonNull(strategies, "strategies must not be null");

        Map<String, FeeStrategy> normalized = new HashMap<String, FeeStrategy>();
        for (Map.Entry<String, FeeStrategy> entry : strategies.entrySet()) {
            normalized.put(normalizeType(entry.getKey()),
                    Objects.requireNonNull(entry.getValue(), "fee strategy must not be null"));
        }
        this.strategies = Collections.unmodifiableMap(normalized);
    }

    public static FeeStrategyFactory withDefaultStrategies() {
        Map<String, FeeStrategy> defaults = new HashMap<String, FeeStrategy>();
        defaults.put("INTERNAL", new InternalTransferFee());
        defaults.put("NAPAS_247", new Napas247Fee());
        defaults.put("SWIFT", new SwiftFee());
        return new FeeStrategyFactory(defaults);
    }

    public FeeStrategy getStrategy(String transferType) {
        String normalizedType = normalizeType(transferType);
        FeeStrategy strategy = strategies.get(normalizedType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported transfer type: " + transferType);
        }
        return strategy;
    }

    private static String normalizeType(String transferType) {
        Objects.requireNonNull(transferType, "transferType must not be null");
        String normalized = transferType.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("transferType must not be blank");
        }
        return normalized;
    }
}
