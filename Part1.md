# Phân tích và tái cấu trúc TransferService
## Dự án: Rikkei Digital Bank

---

# 1. Giới thiệu

Trong hệ thống Rikkei Digital Bank, nghiệp vụ chuyển tiền (Transfer) là một trong những nghiệp vụ cốt lõi của Core Banking.

Theo yêu cầu của khách hàng, hệ thống phải đáp ứng các tiêu chí sau:

- Hỗ trợ nhiều loại chuyển tiền.
- Có thể mở rộng thêm nhiều phương thức tính phí.
- Hệ thống cảnh báo gian lận theo thời gian thực.
- Dễ bảo trì khi chính sách ngân hàng thay đổi.
- Đảm bảo tính nhất quán dữ liệu (Consistency).

Tuy nhiên module TransferService hiện tại đang vi phạm nhiều nguyên lý thiết kế phần mềm khiến việc mở rộng về sau trở nên rất khó khăn.

---

# 2. Phân tích những điểm yếu của mã nguồn cũ

## 2.1 Vi phạm Single Responsibility Principle (SRP)

TransferService đang thực hiện quá nhiều nhiệm vụ:

- Kiểm tra số dư
- Tính phí giao dịch
- Trừ tiền
- Cộng tiền
- Gửi OTP
- Đồng bộ Core Banking
- Tạo Transaction

Một class chỉ nên có **một lý do để thay đổi**, nhưng hiện tại bất kỳ thay đổi nào về:

- biểu phí
- OTP
- Core Banking

đều phải sửa TransferService.

=> Vi phạm nghiêm trọng SRP.

---

## 2.2 Vi phạm Open/Closed Principle (OCP)

Đoạn code:

```java
if(type.equals("INTERNAL")){

}else if(type.equals("NAPAS_247")){

}else if(type.equals("SWIFT")){

}
```

Mỗi lần ngân hàng bổ sung:

- QR Transfer
- Visa Direct
- UnionPay
- Crypto Transfer
- Western Union

đều phải sửa class TransferService.

Điều này khiến:

- dễ phát sinh bug
- khó test regression
- khó bảo trì

=> Vi phạm Open/Closed Principle.

---

## 2.3 Business Rule bị hard-code

Ví dụ:

```java
fee = 2000;
fee = 5000;
fee = amount * 0.05 + 50000;
```

Các giá trị này đáng lẽ nên được lấy từ:

- Database
- Configuration Server
- Fee Policy Service

Nếu ngân hàng đổi biểu phí, lập trình viên phải build lại hệ thống.

---

## 2.4 Coupling quá cao

TransferService phụ thuộc trực tiếp vào:

```
OTP

Core Banking

Fee Calculation

Transfer Logic
```

Điều này làm module gần như không thể tái sử dụng.

---

## 2.5 Không hỗ trợ mở rộng

Ví dụ sau này ngân hàng bổ sung:

- miễn phí VIP
- miễn phí theo giờ vàng
- miễn phí cho doanh nghiệp

Toàn bộ TransferService sẽ phải sửa.

---

## 2.6 Khó Unit Test

Không thể test riêng:

- Fee
- OTP
- Sync

vì tất cả nằm chung trong một method.

---

## 2.7 Không hỗ trợ Event Driven

Sau khi chuyển tiền thành công hệ thống còn cần:

- Push Notification
- SMS
- Email
- Kafka
- Audit Log
- Fraud Detection

Nhưng code hiện tại chỉ:

```java
System.out.println(...)
```

Không thể mở rộng.

---

# 3. Phân tích Consistency và Real-time Risk

Theo yêu cầu của khách hàng:

> Giao dịch bất thường phải bị tạm giữ và thông báo ngay lập tức.

Điều này ảnh hưởng trực tiếp tới thiết kế hệ thống.

---

## 3.1 Rủi ro mất Consistency

Giả sử quy trình hiện tại:

```
Trừ tiền

↓

Cộng tiền

↓

OTP lỗi
```

Nếu OTP lỗi sau khi đã trừ tiền:

```
From Account giảm tiền

To Account chưa nhận

Transaction chưa lưu
```

Dữ liệu bị sai lệch.

Đây là lỗi rất nghiêm trọng trong ngân hàng.

---

## 3.2 Rủi ro Core Banking

Nếu:

```
Deduct thành công

↓

Sync Core Banking thất bại
```

Hệ thống nội bộ và Core Banking sẽ không đồng bộ.

Hậu quả:

- Sai số dư
- Sai sao kê
- Sai đối soát cuối ngày

---

## 3.3 Fraud Detection

Khách hàng yêu cầu:

```
Đăng nhập từ vị trí mới

+

Chuyển số tiền lớn

↓

Hold Transaction
```

Điều này nghĩa là:

Fraud Detection không nên nằm trong TransferService.

Nó phải là:

```
Fraud Service

↓

Risk Engine

↓

Decision Engine
```

để có thể mở rộng Machine Learning sau này.

---

## 3.4 OTP

OTP là tác vụ phụ.

Nếu gửi OTP chậm:

```
Không nên block toàn bộ giao dịch.
```

Có thể dùng:

- RabbitMQ
- Kafka
- Event

để xử lý bất đồng bộ.

---

## 3.5 Notification

Email

SMS

Push

Audit Log

không ảnh hưởng tới việc trừ tiền.

Do đó nên đưa thành Event Listener.

---

# 4. Kiến trúc đề xuất

```
                    TransferService
                           │
           ┌───────────────┴───────────────┐
           │                               │
     Fee Strategy                  Event Publisher
           │                               │
           │                     ┌─────────┴─────────┐
           │                     │                   │
      InternalFee          OTP Listener       CoreBank Listener
      NapasFee             Email Listener     Audit Listener
      SwiftFee             SMS Listener       Fraud Listener
```

TransferService chỉ còn chịu trách nhiệm:

- kiểm tra số dư
- tính phí
- trừ tiền
- tạo Transaction

Mọi tác vụ khác đều được tách riêng.

---

# 5. Thiết kế mới

## 5.1 Interface FeeStrategy

```java
public interface FeeStrategy {

    double calculateFee(double amount);

}
```

---

## 5.2 InternalTransferFee

```java
public class InternalTransferFee implements FeeStrategy{

    @Override
    public double calculateFee(double amount){
        return 0;
    }

}
```

---

## 5.3 Napas247Fee

```java
public class Napas247Fee implements FeeStrategy{

    @Override
    public double calculateFee(double amount){

        if(amount < 2_000_000){
            return 2000;
        }

        return 5000;
    }

}
```

---

## 5.4 SwiftFee

```java
public class SwiftFee implements FeeStrategy{

    @Override
    public double calculateFee(double amount){

        return amount * 0.05 + 50000;

    }

}
```

---

## 5.5 Factory

```java
import java.util.Map;

public class FeeStrategyFactory {

    private final Map<String, FeeStrategy> strategies;

    public FeeStrategyFactory() {

        strategies = Map.of(
                "INTERNAL", new InternalTransferFee(),
                "NAPAS_247", new Napas247Fee(),
                "SWIFT", new SwiftFee()
        );

    }

    public FeeStrategy getStrategy(String type) {

        return strategies.get(type);

    }

}
```

---

## 5.6 Event Interface

```java
public interface TransactionEventListener {

    void handle(Transaction transaction);

}
```

---

## 5.7 OTP Listener

```java
public class OtpListener implements TransactionEventListener {

    @Override
    public void handle(Transaction transaction){

        System.out.println("Sending OTP...");

    }

}
```

---

## 5.8 Core Banking Listener

```java
public class CoreBankListener implements TransactionEventListener {

    @Override
    public void handle(Transaction transaction){

        System.out.println("Syncing Core Banking...");

    }

}
```

---

## 5.9 Event Publisher

```java
import java.util.List;

public class TransactionEventPublisher {

    private final List<TransactionEventListener> listeners;

    public TransactionEventPublisher(List<TransactionEventListener> listeners){

        this.listeners = listeners;

    }

    public void publish(Transaction transaction){

        listeners.forEach(listener -> listener.handle(transaction));

    }

}
```

---

## 5.10 TransferService sau khi Refactor

```java
public class TransferService {

    private final FeeStrategyFactory feeFactory;

    private final TransactionEventPublisher publisher;

    public TransferService(
            FeeStrategyFactory feeFactory,
            TransactionEventPublisher publisher
    ){

        this.feeFactory = feeFactory;
        this.publisher = publisher;

    }

    public Transaction processTransfer(
            Account fromAcc,
            Account toAcc,
            double amount,
            String transferType
    ){

        if(fromAcc.getBalance() < amount){

            throw new RuntimeException("Insufficient balance");

        }

        FeeStrategy strategy = feeFactory.getStrategy(transferType);

        double fee = strategy.calculateFee(amount);

        fromAcc.deduct(amount + fee);

        toAcc.add(amount);

        Transaction transaction =
                new Transaction(
                        fromAcc,
                        toAcc,
                        amount,
                        fee,
                        "SUCCESS"
                );

        publisher.publish(transaction);

        return transaction;

    }

}
```

---

# 6. Lợi ích sau khi Refactor

| Tiêu chí | Code cũ | Code mới |
|-----------|----------|-----------|
| SRP | ❌ | ✅ |
| OCP | ❌ | ✅ |
| Dễ mở rộng loại phí | ❌ | ✅ |
| Unit Test | Khó | Dễ |
| Event Driven | Không | Có |
| Notification độc lập | Không | Có |
| OTP độc lập | Không | Có |
| Core Banking độc lập | Không | Có |
| Thêm phương thức chuyển tiền | Phải sửa TransferService | Chỉ cần thêm Strategy |
| Khả năng bảo trì | Thấp | Cao |

---

# 7. Đề xuất nâng cấp trong môi trường Production

Để đáp ứng yêu cầu của Rikkei Digital Bank trong môi trường thực tế, nên triển khai thêm:

- Spring Transaction (`@Transactional`) để đảm bảo tính nhất quán dữ liệu khi ghi nhận giao dịch.
- Saga Pattern hoặc Transaction Outbox khi đồng bộ với Core Banking nhằm tránh mất đồng bộ giữa các hệ thống.
- Message Broker (Kafka hoặc RabbitMQ) để xử lý OTP, Email, SMS và Audit Log theo cơ chế bất đồng bộ.
- Rule Engine hoặc Fraud Detection Service riêng để đánh giá rủi ro giao dịch theo thời gian thực trước khi thực hiện chuyển tiền.
- Cấu hình biểu phí từ Database hoặc Configuration Server thay vì hard-code để dễ thay đổi theo chính sách ngân hàng.
- Áp dụng Dependency Injection (Spring IoC) để quản lý các Strategy và Listener, giúp dễ mở rộng và kiểm thử.

---

# 8. Kết luận

Việc tái cấu trúc TransferService bằng cách áp dụng **Strategy Pattern** cho tính phí và **Observer/Event Pattern** cho các tác vụ phụ giúp hệ thống:

- Tuân thủ các nguyên lý SOLID (đặc biệt là SRP và OCP).
- Dễ dàng mở rộng khi bổ sung loại giao dịch hoặc chính sách phí mới.
- Tách biệt hoàn toàn logic nghiệp vụ cốt lõi với các tác vụ phụ như OTP, đồng bộ Core Banking và thông báo.
- Nâng cao khả năng kiểm thử, bảo trì và triển khai trong môi trường ngân hàng có yêu cầu cao về tính ổn định, tính nhất quán và xử lý thời gian thực.

Kiến trúc mới phù hợp hơn với định hướng phát triển lâu dài của một hệ thống Digital Banking hiện đại.



## Phan 2

## Phan 3

## Phan 4
