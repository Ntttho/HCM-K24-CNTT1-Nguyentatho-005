# Hackathon De05 NguyenTaTho

## Nhiệm vụ 1

1. Viết prompt 1
```
Hãy đóng vai là một người chuyên viên phân ích hệ thống với hơn 10 năm kinh nghiệm trong lĩnh vực làm việc với ngân hàng  
Nhiệm vụ của bạn là Tái cấu trúc một module chuyển tiền TransferService để dể cho việc mở rộng. Tạo ra một module hóa cách tính toán chi phí và tách biệt hệ thống xử lý tác vụ phụ ra khỏi logic trừ tiền cốt lõi.  

Bối cảnh: Hình ảnh tôi gửi cho bạn Vấn đề ở đây: Module chuyển tiền dưới đây có quá nhiều logic ifelse lồng nhau để tính chi phí giao dịch và gửi mã OTP, việc này vi phạm nghiêm trọng nguyên lý Single Responsibility và Open/Closed   
[package refactoring; public class TransferService { public Transaction processTransfer(Account fromAcc, Account toAcc, double amount, String transferType) { if (fromAcc.getBalance() < amount) throw new RuntimeException("Insufficient balance"); double fee = 0; if (transferType.equals("INTERNAL")) { fee = 0; } else if (transferType.equals("NAPAS_247")) { if (amount < 2000000) fee = 2000; else fee = 5000; } else if (transferType.equals("SWIFT")) { fee = amount * 0.05 + 50000; // Phí quốc tế } fromAcc.deduct(amount + fee); toAcc.add(amount); System.out.println("Sending OTP to user email..."); System.out.println("Syncing transaction to core banking..."); return new Transaction(fromAcc, toAcc, amount, fee, "SUCCESS"); } }]   
Với những yêu cầu trên hãy thực hiện theo từng bước sau đây:  
1. Phân tích những điểm yêu do mã nguồn củ gây ra  
2. Phân tích và xem xét tính Consistency và rủi ro real-time theo như khác hàng nêu trên và đưa ra lý do tại sao bạn lại làm như vậy  
3. Refactor code, thực hiện tái cấu trúc mã nguồn cho module chuyển tiền TransferService này  

Kết quả cuối cùng: là một mã nguồn của file markdown bao gồm những phân tích về cách củ, cách mới, rủi ro, tính consistency và mã nguồn các file java bạn đã thực hiện.
```

2. Kết quả
---
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


---

## Nhiệm vụ 2
1Viết prompt 2
```markdown

```



# Nhiemej vu 2

1. Vieest prompt
```markdown
Hãy đóng vai một chuyên viên phân tích và thiết kế hệ thống với hơn 10 năm kinh nghiệm. Nhiệm vụ của bạn là cùng tôi điểu tra ra nguyên nhân gốc rễ trong luồng hoạt động của SecurityFilter.
Java
package com.rikkei.security;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

private final String SECRET_KEY = "rikkei_secret_key_super_secure_do_not_share";

    @Overrideprotected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)throws ServletException, IOException {
            
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            var claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
                  
            List<String> roles = (List<String>) claims.get("roles");
            if (!roles.contains("ROLE_ADMIN") && request.getRequestURI().startsWith("/api/admin")) {
                throw new AccessDeniedException("Access Denied"); 
            }
                    
            if (claims.getSubject() != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Logic set Authentication vào SecurityContext (đã rút gọn)
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
org.springframework.security.access.AccessDeniedException: Access Denied
at com.rikkei.security.JwtAuthenticationFilter.doFilterInternal(JwtAuthenticationFilter.java:46)
at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
...
Mô tả là hình ảnh như củ Với yêu cầu hãy thực hiện theo các bước: 1. Xác định luồng hoạt động của security filter 2. Dựa vào đó tìm ra giải pháp để bắt lỗi này một cách tập trung, tích hợp cơ chế xữ lý ngoại lệ một cách chuẩn mực trong spring security, trả về json đồng nhất vd forbidden message 3. dựa vào giải pháp và viết code tạo các thực thể cùng với giải pháp và giải thích tại sao không nên chỉ dùng trycatch đơn thuần bên trong ham filter Đầu ra: tiếp tục mà ghi file với đuôi markdown theo từng bước thực hiện.

```

2. Ket qua

# Phân tích và thiết kế xử lý ngoại lệ tập trung cho Security Filter
## Dự án: Rikkei Digital Bank

---

# 1. Giới thiệu

Trong hệ thống Rikkei Digital Bank, mọi request từ Client đều phải đi qua Spring Security Filter Chain trước khi được chuyển tới Controller.

Khách hàng đặt ra các yêu cầu quan trọng:

- Quản lý nhiều loại người dùng (Cá nhân, Doanh nghiệp, Giao dịch viên).
- Kiểm soát quyền truy cập chặt chẽ.
- Có khả năng mở rộng nhiều chính sách bảo mật.
- Trả về lỗi theo chuẩn REST API.
- Dễ dàng tích hợp Mobile App và Internet Banking.

Tuy nhiên Security Filter hiện tại đang tự phát sinh ngoại lệ `AccessDeniedException` ngay bên trong Filter dẫn đến việc response trả về không đồng nhất và khó mở rộng.

---

# 2. Luồng hoạt động của Spring Security Filter

## 2.1 Luồng xử lý tổng quát

```text
                 Client Request
                        │
                        ▼
             Security Filter Chain
                        │
                        ▼
         JwtAuthenticationFilter
                        │
          ┌─────────────┴─────────────┐
          │                           │
   Token hợp lệ                 Token không hợp lệ
          │                           │
          ▼                           ▼
   Parse JWT                  Exception
          │
          ▼
   Kiểm tra quyền (Role)
          │
      ┌───┴────┐
      │        │
Có quyền    Không có quyền
      │        │
      ▼        ▼
 Set Authentication
      │
      ▼
Controller
      │
      ▼
Business Service
```

---

## 2.2 Luồng hoạt động của JwtAuthenticationFilter

```text
Request

↓

Lấy Authorization Header

↓

Bearer Token ?

↓

Parse JWT

↓

Đọc Claims

↓

Lấy Roles

↓

Kiểm tra quyền truy cập

↓

Nếu không đủ quyền

↓

throw AccessDeniedException

↓

Nếu đủ quyền

↓

Set Authentication

↓

FilterChain.doFilter()

↓

Controller
```

---

# 3. Phân tích nguyên nhân gốc rễ (Root Cause Analysis)

## Hiện trạng

Trong Filter có đoạn:

```java
if (!roles.contains("ROLE_ADMIN")
    && request.getRequestURI().startsWith("/api/admin")) {

    throw new AccessDeniedException("Access Denied");

}
```

Nhìn qua tưởng rằng việc throw Exception là đúng.

Tuy nhiên đây chỉ là triệu chứng.

Nguyên nhân gốc rễ nằm ở kiến trúc xử lý ngoại lệ.

---

## Root Cause 1

Filter đang tự xử lý Authorization.

Trong Spring Security, Authorization vốn được thực hiện bởi:

- FilterSecurityInterceptor (Spring Security 5)
- AuthorizationFilter (Spring Security 6)

Điều đó nghĩa là JwtAuthenticationFilter đang đảm nhận thêm trách nhiệm không thuộc về nó.

=> Vi phạm Single Responsibility Principle.

---

## Root Cause 2

Filter phát sinh Exception nhưng không có ExceptionTranslationFilter xử lý đúng cách.

Spring Security có cơ chế:

```text
AccessDeniedException

↓

ExceptionTranslationFilter

↓

AccessDeniedHandler

↓

JSON Response
```

Nhưng hiện tại Filter tự throw exception mà chưa cấu hình AccessDeniedHandler.

Kết quả:

```
500 Internal Server Error

hoặc

Stack Trace
```

thay vì

```json
{
    "success": false,
    "code": 403,
    "message": "Forbidden"
}
```

---

## Root Cause 3

Logic kiểm tra Role bị hard-code

```java
request.getRequestURI().startsWith("/api/admin")
```

Nếu sau này xuất hiện:

```
/api/staff

/api/teller

/api/auditor

/api/manager
```

Filter sẽ ngày càng chứa nhiều if-else.

---

## Root Cause 4

Filter đang làm quá nhiều nhiệm vụ

Hiện tại Filter chịu trách nhiệm:

- Parse JWT
- Validate JWT
- Kiểm tra quyền
- Throw Exception
- Set Authentication

Điều này khiến Filter khó bảo trì và khó mở rộng.

---

# 4. Kiến trúc xử lý ngoại lệ chuẩn của Spring Security

Spring Security đã cung cấp sẵn cơ chế xử lý tập trung.

```text
Request

↓

JwtAuthenticationFilter

↓

Authentication

↓

AuthorizationFilter

↓

AccessDeniedException

↓

ExceptionTranslationFilter

↓

AccessDeniedHandler

↓

JSON Response
```

Ưu điểm:

- Không cần try-catch trong Filter.
- Có một nơi duy nhất xử lý lỗi.
- Dễ mở rộng.
- Response thống nhất.

---

# 5. Giải pháp đề xuất

## Bước 1

JwtAuthenticationFilter chỉ nên làm:

- Parse JWT
- Validate Token
- Set Authentication

Không kiểm tra quyền.

Ví dụ:

```java
Authentication authentication =
        new UsernamePasswordAuthenticationToken(
                username,
                null,
                authorities
        );

SecurityContextHolder.getContext()
        .setAuthentication(authentication);
```

Sau đó:

```java
filterChain.doFilter(request,response);
```

Kết thúc.

---

## Bước 2

Phân quyền bằng Spring Security

```java
http
.authorizeHttpRequests(auth -> auth

.requestMatchers("/api/admin/**")
.hasRole("ADMIN")

.requestMatchers("/api/staff/**")
.hasRole("STAFF")

.anyRequest()
.authenticated()

);
```

Lúc này AuthorizationFilter sẽ tự kiểm tra quyền.

---

## Bước 3

Tạo AccessDeniedHandler

```java
@Component
public class CustomAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ObjectMapper mapper =
            new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        response.setContentType("application/json");

        ApiError error = new ApiError(
                false,
                403,
                "Forbidden"
        );

        mapper.writeValue(
                response.getOutputStream(),
                error
        );

    }

}
```

---

## Bước 4

Tạo AuthenticationEntryPoint

Dùng cho các trường hợp:

- Token sai
- Token hết hạn
- Không có Token

```java
@Component
public class JwtAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper mapper =
            new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException ex
    ) throws IOException {

        response.setStatus(401);

        response.setContentType("application/json");

        ApiError error = new ApiError(
                false,
                401,
                "Unauthorized"
        );

        mapper.writeValue(
                response.getOutputStream(),
                error
        );

    }

}
```

---

## Bước 5

ApiError

```java
public class ApiError {

    private boolean success;

    private int code;

    private String message;

    public ApiError(
            boolean success,
            int code,
            String message
    ){

        this.success = success;
        this.code = code;
        this.message = message;

    }

    public boolean isSuccess() {
        return success;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
```

---

## Bước 6

SecurityConfig

```java
@Bean
SecurityFilterChain securityFilterChain(
        HttpSecurity http
) throws Exception {

    http

    .exceptionHandling(exception -> exception

        .authenticationEntryPoint(jwtAuthenticationEntryPoint)

        .accessDeniedHandler(customAccessDeniedHandler)

    )

    .authorizeHttpRequests(auth -> auth

        .requestMatchers("/api/admin/**")
        .hasRole("ADMIN")

        .anyRequest()
        .authenticated()

    );

    return http.build();

}
```

---

# 6. Tại sao không nên dùng try-catch trong Filter?

Nhiều lập trình viên thường viết:

```java
try{

    ...

}catch(Exception ex){

    response.getWriter()
            .write("Forbidden");

}
```

Đây không phải là cách làm được khuyến nghị trong Spring Security.

### Thứ nhất

Mỗi Filter sẽ phải tự viết lại logic xử lý lỗi.

Nếu có:

- JWT Filter
- Audit Filter
- Rate Limit Filter
- Logging Filter

thì cả bốn Filter đều phải có try-catch.

Dẫn tới trùng lặp mã nguồn.

---

### Thứ hai

Không thống nhất Response.

Ví dụ:

JWT Filter

```json
{
    "message":"Forbidden"
}
```

Audit Filter

```json
{
    "error":"Access denied"
}
```

Rate Limit

```json
{
    "status":"403"
}
```

Mobile App rất khó xử lý.

---

### Thứ ba

Khó mở rộng.

Nếu khách hàng yêu cầu:

```
code

message

timestamp

traceId

path
```

sẽ phải sửa tất cả các Filter.

---

### Thứ tư

Vi phạm Separation of Concerns.

Filter chỉ nên xác thực (Authentication) và chuyển tiếp request.

Việc chuyển đổi ngoại lệ thành HTTP Response thuộc trách nhiệm của `ExceptionTranslationFilter`, `AuthenticationEntryPoint` và `AccessDeniedHandler`.

---

# 7. Kiến trúc sau khi cải tiến

```text
                 Client
                    │
                    ▼
        JwtAuthenticationFilter
                    │
          Parse JWT
                    │
          Set Authentication
                    │
                    ▼
        AuthorizationFilter
                    │
       Kiểm tra quyền truy cập
          ┌─────────┴─────────┐
          │                   │
      Được phép         Không được phép
          │                   │
          ▼                   ▼
    Controller       AccessDeniedException
                                │
                                ▼
                  ExceptionTranslationFilter
                                │
                                ▼
                  CustomAccessDeniedHandler
                                │
                                ▼
                     JSON Response (403)
```

---

# 8. Kết quả trả về chuẩn REST

## Trường hợp chưa đăng nhập

```json
{
    "success": false,
    "code": 401,
    "message": "Unauthorized"
}
```

## Trường hợp không đủ quyền

```json
{
    "success": false,
    "code": 403,
    "message": "Forbidden"
}
```

---

# 9. Kết luận

Việc để `JwtAuthenticationFilter` trực tiếp kiểm tra quyền và ném `AccessDeniedException` làm cho Filter gánh quá nhiều trách nhiệm, vi phạm nguyên lý **Single Responsibility** và không tận dụng được cơ chế xử lý ngoại lệ chuẩn của Spring Security.

Giải pháp phù hợp là:

- Giữ `JwtAuthenticationFilter` chỉ thực hiện xác thực (Authentication): đọc JWT, kiểm tra tính hợp lệ và đưa `Authentication` vào `SecurityContext`.
- Để Spring Security thực hiện phân quyền (Authorization) thông qua `AuthorizationFilter` và cấu hình `authorizeHttpRequests`.
- Tập trung xử lý lỗi bằng `AuthenticationEntryPoint` (401) và `AccessDeniedHandler` (403) nhằm đảm bảo toàn bộ API trả về định dạng JSON thống nhất.
- Tận dụng `ExceptionTranslationFilter` để chuyển đổi ngoại lệ thành HTTP Response thay vì sử dụng `try-catch` trong từng Filter.

Thiết kế này tuân thủ các nguyên lý SOLID, giảm sự phụ thuộc giữa các thành phần, dễ mở rộng khi có thêm vai trò hoặc chính sách bảo mật mới, đồng thời phù hợp với kiến trúc của các hệ thống ngân hàng và Digital Banking trong môi trường Production.


3. Nhệm vụ 03
```markdown
Dựa vào những phân tích ở phân security và mã nguồn sinh ra hãy thực hiện việc tạo nên hình ảnh các bảng entity nhầm báo cáo khác hàng
```