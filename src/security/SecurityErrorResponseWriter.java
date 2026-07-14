package security;

import com.fasterxml.jackson.databind.ObjectMapper;
import entity.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Dùng chung việc ghi JSON lỗi, tránh mỗi security handler trả về một schema khác nhau. */
final class SecurityErrorResponseWriter {
    private SecurityErrorResponseWriter() {
    }

    static void write(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ApiError(false, status, message));
    }
}
