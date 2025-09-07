package com.onmarket.post.domain;

public enum PostType {
    LOAN("대출"),
    SUPPORT("공공지원금");

    private final String description;

    PostType(String description) {
        this.description = description;
    }
}
