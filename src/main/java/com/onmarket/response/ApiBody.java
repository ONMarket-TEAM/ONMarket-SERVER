package com.onmarket.response;

public class ApiBody <T> {

    private T data;

    public ApiBody(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}