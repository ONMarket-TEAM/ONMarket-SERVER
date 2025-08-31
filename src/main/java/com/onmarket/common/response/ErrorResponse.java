package com.onmarket.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private String message;
    private String error;
    private String path;

    public ErrorResponse(LocalDateTime timestamp, String message, String error, String path) {
        this.timestamp = timestamp;
        this.message = message;
        this.error = error;
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public String getPath() {
        return path;
    }
}
