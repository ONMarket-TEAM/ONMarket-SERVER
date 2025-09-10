package com.onmarket.common.response;

public class ApiResponse <T> {

    private ApiHeader header;
    private ApiBody <T> body;

    public ApiResponse(ApiHeader header){
        this.header = header;
    }

    public ApiResponse(ApiHeader header, ApiBody body){
        this.header = header;
        this.body = body;
    }

    public static <T> ApiResponse<T> success(ResponseCode responseCode, T data) {
        return new ApiResponse<T>(new ApiHeader(responseCode.getHttpStatus(), responseCode.getMessage()), new ApiBody<>(data));
    }

    public static <T> ApiResponse<T> success(ResponseCode responseCode) {
        return new ApiResponse<T>(new ApiHeader(responseCode.getHttpStatus(), responseCode.getMessage()));
    }

    public static <T> ApiResponse<T> fail(ResponseCode responseCode) {
        return new ApiResponse<T>(new ApiHeader(responseCode.getHttpStatus(), responseCode.getMessage()));
    }

    public static <T> ApiResponse<T> fail(ResponseCode responseCode, String message) {
        return new ApiResponse<T>(new ApiHeader(responseCode.getHttpStatus(), responseCode.getMessage()), new ApiBody(message));
    }

    public static <T> ApiResponse<T> fail(ResponseCode responseCode, T data) {
        return new ApiResponse<T>(
                new ApiHeader(responseCode.getHttpStatus(), responseCode.getMessage()),
                new ApiBody<>(data)
        );
    }

    public ApiHeader getHeader() {
        return header;
    }

    public ApiBody<T> getBody() {
        return body;
    }
}