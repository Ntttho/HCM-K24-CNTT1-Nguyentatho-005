package entity;

/**
 * Định dạng lỗi thống nhất cho REST API. Các handler bảo mật dùng cùng entity
 * này để Mobile App và Internet Banking chỉ cần xử lý một schema lỗi.
 */
public final class ApiError {
    private final boolean success;
    private final int code;
    private final String message;

    public ApiError(boolean success, int code, String message) {
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
