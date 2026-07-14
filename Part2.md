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