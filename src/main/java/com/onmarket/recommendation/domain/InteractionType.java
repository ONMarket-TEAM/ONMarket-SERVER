package com.onmarket.recommendation.domain;

public enum InteractionType {
    VIEW,           // 게시물 조회
    SCROLL,         // 스크롤
    CLICK_LINK,     // 바로가기 링크 클릭
    SCRAP,          // 스크랩
    UNSCRAP,        // 스크랩 해제
    RATING,         // 평점 작성
    COMMENT         // 댓글 작성
}
