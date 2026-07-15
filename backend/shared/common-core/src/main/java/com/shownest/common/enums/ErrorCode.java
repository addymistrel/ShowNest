package com.shownest.common.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Generic
    INTERNAL_SERVER_ERROR    ("SN-000", "An unexpected error occurred",           HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR         ("SN-001", "Validation failed",                      HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND       ("SN-002", "Resource not found",                     HttpStatus.NOT_FOUND),
    UNAUTHORIZED             ("SN-003", "Authentication required",                HttpStatus.UNAUTHORIZED),
    FORBIDDEN                ("SN-004", "Access denied",                          HttpStatus.FORBIDDEN),
    CONFLICT                 ("SN-005", "Resource already exists",                HttpStatus.CONFLICT),

    // Auth Service
    INVALID_CREDENTIALS      ("AUTH-001", "Invalid email or password",            HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED            ("AUTH-002", "Token has expired",                    HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID            ("AUTH-003", "Token is invalid",                     HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS     ("AUTH-004", "Email is already registered",          HttpStatus.CONFLICT),
    ACCOUNT_DISABLED         ("AUTH-005", "Account is disabled",                  HttpStatus.FORBIDDEN),

    // User Service
    USER_NOT_FOUND           ("USR-001", "User not found",                        HttpStatus.NOT_FOUND),

    // Event Service
    EVENT_NOT_FOUND          ("EVT-001", "Event not found",                       HttpStatus.NOT_FOUND),
    EVENT_CANCELLED          ("EVT-002", "Event has been cancelled",              HttpStatus.GONE),

    // Venue Service
    VENUE_NOT_FOUND          ("VEN-001", "Venue not found",                       HttpStatus.NOT_FOUND),

    // Seat Service
    SEAT_NOT_FOUND           ("SEAT-001", "Seat not found",                       HttpStatus.NOT_FOUND),
    SEAT_ALREADY_LOCKED      ("SEAT-002", "Seat is temporarily held by another user", HttpStatus.CONFLICT),
    SEAT_ALREADY_BOOKED      ("SEAT-003", "Seat is already booked",               HttpStatus.CONFLICT),

    // Booking Service
    BOOKING_NOT_FOUND        ("BKG-001", "Booking not found",                     HttpStatus.NOT_FOUND),
    BOOKING_EXPIRED          ("BKG-002", "Booking session has expired",           HttpStatus.GONE),
    BOOKING_ALREADY_CANCELLED("BKG-003", "Booking is already cancelled",         HttpStatus.CONFLICT),

    // Payment Service
    PAYMENT_FAILED           ("PAY-001", "Payment processing failed",             HttpStatus.PAYMENT_REQUIRED),
    PAYMENT_NOT_FOUND        ("PAY-002", "Payment not found",                     HttpStatus.NOT_FOUND),
    REFUND_FAILED            ("PAY-003", "Refund processing failed",              HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code       = code;
        this.message    = message;
        this.httpStatus = httpStatus;
    }

    public String getCode()           { return code; }
    public String getMessage()        { return message; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
