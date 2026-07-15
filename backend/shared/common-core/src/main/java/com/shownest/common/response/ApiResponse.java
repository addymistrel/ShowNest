package com.shownest.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;
    private final Instant timestamp;

    private ApiResponse(boolean success, T data, ErrorDetail error) {
        this.success   = success;
        this.data      = data;
        this.error     = error;
        this.timestamp = Instant.now();
    }

    // ── Success ───────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null);
    }

    // ── Error ─────────────────────────────────────────────────────

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message));
    }

    // ── Inner error detail ────────────────────────────────────────

    @Getter
    public static class ErrorDetail {
        private final String code;
        private final String message;

        public ErrorDetail(String code, String message) {
            this.code    = code;
            this.message = message;
        }
    }
}
