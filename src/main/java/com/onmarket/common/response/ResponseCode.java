package com.onmarket.common.response;

import org.springframework.http.HttpStatus;

public enum ResponseCode {
    /**
     * Auth / Token 관련 응답
     */
    AUTH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "인증 토큰이 존재하지 않거나 올바르지 않습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    /**
     * Member response
     */
    LOGIN_SUCCESS(HttpStatus.OK, "로그인에 성공했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "사용자 ID 또는 비밀번호가 일치하지 않습니다."),
    MEMBER_DELETED(HttpStatus.FORBIDDEN, "탈퇴된 회원입니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "계정이 잠겨 있습니다."),
    ACCOUNT_EXPIRED(HttpStatus.UNAUTHORIZED, "계정 사용 기간이 만료되었습니다."),
    MEMBER_INFO_SUCCESS(HttpStatus.OK, "사용자 정보 조회에 성공했습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다,"),
    MEMBER_WITHDRAW_SUCCESS(HttpStatus.OK, "회원탈퇴가 완료되었습니다."),
    MEMBER_WITHDRAW_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "회원탈퇴에 실패했습니다."),
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    PROFILE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "회원정보 수정에 실패했습니다."),
    INVALID_BIRTHDATE_FORMAT(HttpStatus.BAD_REQUEST, "생년월일 형식이 올바르지 않습니다."),
    PHONE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "휴대폰 인증을 완료해 주세요."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "이메일이 일치하지 않습니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관에 동의해 주세요."),
    MISSING_REQUIRED_FIELDS(HttpStatus.BAD_REQUEST, "모든 필수 입력값을 입력해 주세요."),
    SIGNUP_SUCCESS(HttpStatus.CREATED, "회원가입이 완료되었습니다."),
    VALID_EMAIL(HttpStatus.OK, "사용 가능한 이메일입니다."),
    DUPLICATED_EMAIL(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다."),
    VALID_NICKNAME(HttpStatus.OK, "사용 가능한 닉네임입니다."),
    DUPLICATED_NICKNAME(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "본인인증이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다."),
    TOKEN_REFRESH_SUCCESS(HttpStatus.OK, "토큰이 성공적으로 갱신되었습니다."),
    ID_FIND_SUCCESS(HttpStatus.OK, "아이디 찾기에 성공했습니다."),
    PASSWORD_FIND_SUCCESS(HttpStatus.OK, "비밀번호 찾기에 성공했습니다."),
    PASSWORD_RESET_SUCCESS(HttpStatus.OK, "비밀번호가 성공적으로 변경되었습니다."),
    PROFILE_UPDATE_SUCCESS(HttpStatus.OK, "프로필이 성공적으로 수정되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃이 성공적으로 처리되었습니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),
    PROFILE_IMAGE_UPLOAD_SUCCESS(HttpStatus.OK, "사진이 성공적으로 업로드되었습니다."),
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    PASSWORD_CHECK_SUCCESS(HttpStatus.OK,"본인인증에 성공했습니다.") ,
    TOKEN_EXCHANGE_FAILED(HttpStatus.UNAUTHORIZED, "토큰 교환에 실패했습니다."),

    /**
     * 비밀번호 강도 검증 관련
     */
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "비밀번호를 입력해주세요."),
    PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "비밀번호는 최소 8자 이상이어야 합니다."),
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "비밀번호는 최대 20자 이하여야 합니다."),
    PASSWORD_COMPLEXITY_INSUFFICIENT(HttpStatus.BAD_REQUEST, "영문 대/소문자, 숫자, 특수문자 중 최소 2가지를 조합해주세요."),
    PASSWORD_WHITESPACE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "비밀번호에 공백문자를 포함할 수 없습니다."),

    /**
     * 회원정보 수정 관련 응답
     */
    CURRENT_PASSWORD_VERIFY_SUCCESS(HttpStatus.OK, "현재 비밀번호와 일치합니다."),
    UPDATE_PROFILE_SUCCESS(HttpStatus.OK, "회원정보 수정 완료"),
    REQUIRED_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호를 입력해주세요"),
    REQUIRED_NEW_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호를 입력해주세요."),
    NEW_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "새 비밀번호 확인이 일치하지 않습니다."),
    SOCIAL_ACCOUNT_PASSWORD_VERIFICATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "소셜 로그인은 비밀번호 검증 불가"),
    SOCIAL_ACCOUNT_PASSWORD_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "소셜 로그인은 비밀번호 변경 불가"),
    INVALID_REQUEST_PARAM(HttpStatus.BAD_REQUEST, "잘못된 요청 파라미터입니다."),
    INVALID_IMAGE_KEY(HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 키입니다."),
    PROFILE_IMAGE_DELETE_SUCCESS(HttpStatus.OK, "프로필 이미지가 성공적으로 삭제되었습니다."),


    /**
     * 사업정보 수정 관련 응담
     */
    BUSINESS_DUPLICATED(HttpStatus.BAD_REQUEST, "중복 사업장 방지"),
    BUSINESS_FORBIDDEN(HttpStatus.BAD_REQUEST, "내부 유출 방지"),

    /**
     * OAuth2 / Social Login 관련 응답
     */
    INSTAGRAM_STATUS_SUCCESS(HttpStatus.OK, "Instagram 연결 상태 조회 성공했습니다."),
    INSTAGRAM_LOGIN_SUCCESS(HttpStatus.OK, "Instagram 로그인 성공"),
    INSTAGRAM_LOGOUT_SUCCESS(HttpStatus.OK, "Instagram 로그아웃 성공"),
    INSTAGRAM_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "Instagram 계정이 연결되어 있지 않습니다."),
    INSTAGRAM_ALREADY_CONNECTED(HttpStatus.CONFLICT, "이미 Instagram 계정이 연결되어 있습니다."),
    OAUTH2_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다."),
    OAUTH2_EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "소셜 계정에서 이메일 정보를 가져올 수 없습니다."),
    OAUTH2_DIFFERENT_SOCIAL_TYPE(HttpStatus.CONFLICT, "다른 소셜 계정으로 이미 가입된 이메일입니다."),
    SOCIAL_SIGNUP_SUCCESS(HttpStatus.CREATED, "소셜 회원가입이 완료되었습니다."),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_AGE_RANGE(HttpStatus.BAD_REQUEST, "유효하지 않은 나이 범위입니다"),
    INVALID_ASSET_RANGE(HttpStatus.BAD_REQUEST, "유효하지 않은 자산 범위입니다"),
    INVALID_WMTI_CODE_FORMAT(HttpStatus.BAD_REQUEST, "WMTI 코드 형식이 올바르지 않습니다"),
    INVALID_CATEGORY_OR_SUBCATEGORY(HttpStatus.BAD_REQUEST, "카테고리 및 소분류는 필수입니다."),
    OAUTH2_ADDITIONAL_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "추가 정보 입력이 필요합니다."),

    /**
     * SMS response
     */
    SMS_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "SMS 전송에 실패했습니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "유효하지 않은 전화번호입니다."),
    SMS_SEND_SUCCESS(HttpStatus.OK, "인증번호가 전송되었습니다."),
    SMS_VERIFY_SUCCESS(HttpStatus.OK, "인증이 완료되었습니다."),
    SMS_VERIFY_FAILED(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),

    // 스케줄러 관련
    SCHEDULER_UPDATE_SUCCESS(HttpStatus.OK, "스케줄러 업데이트 성공"),
    SCHEDULER_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "스케줄러 업데이트 실패"),

    /**
     * 이메일 인증 관련
     */
    MAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "인증 메일을 보내지 못했습니다."),
    INVALID_MAIL(HttpStatus.BAD_REQUEST, "인증 이메일 발송 이력 없음/인증 시간 만료"),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "인증코드 불일치"),
    VERIFICATION_MAIL_SENT(HttpStatus.OK, "인증 메일을 보냈습니다."),
    VERIFIED_CODE(HttpStatus.OK, "인증코드 일치"),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다."),

    /**
     * Business response
     */
    BUSINESS_REGISTER_SUCCESS(HttpStatus.CREATED, "사업장 등록에 성공했습니다."),
    BUSINESS_UPDATE_SUCCESS(HttpStatus.OK, "사업장 수정에 성공했습니다."),
    BUSINESS_DELETE_SUCCESS(HttpStatus.OK, "사업장 삭제에 성공했습니다."),
    BUSINESS_READ_SUCCESS(HttpStatus.OK, "사업장 조회에 성공했습니다."),

    BUSINESS_NOT_FOUND(HttpStatus.NOT_FOUND, "사업장을 찾을 수 없습니다."),
    BUSINESS_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 사업장이 있습니다."),
    BUSINESS_UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "해당 사업장에 접근할 권한이 없습니다."),
    BUSINESS_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "사업장 처리 중 오류가 발생했습니다."),

    /**
     * post response
     */
    POST_LIST_SUCCESS(HttpStatus.OK,"타입별 전체 리스트 조회 성공"),
    POST_DETAIL_SUCCESS(HttpStatus.OK,"게시글 상세 조회 성공"),
    POST_CREDIT_LOAN_CREATE_SUCCESS(HttpStatus.CREATED, "금융 감독원 신용 대출 상품 게시물 동기화 완료"),
    POST_LOAN_CREATE_SUCCESS(HttpStatus.CREATED, "서민금융진흥원 대출 상품 게시물 동기화 완료"),
    POST_SUPPORT_CREATE_SUCCESS(HttpStatus.OK,"공공지원금 게시물 생성이 완료되었습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),

    /**
     * scrap response
     */
    SCRAP_CREATE_SUCCESS(HttpStatus.CREATED, "관심상품 등록 성공"),
    SCRAP_DELETE_SUCCESS(HttpStatus.OK, "관심상품 삭제 성공"),
    SCRAP_READ_SUCCESS(HttpStatus.OK, "관심상품 목록 조회 성공"),
    SCRAP_STATUS_CHECK_SUCCESS(HttpStatus.OK, "관심상품 여부 확인 성공"),

    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "관심상품을 찾을 수 없습니다"),
    SCRAP_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 관심상품으로 등록된 상품입니다"),

    SCRAP_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "관심상품 목록 조회에 실패했습니다"),
    SCRAP_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "관심상품 여부 확인에 실패했습니다"),

    /**
     * commnet response
     */
    // ResponseCode.java에 추가해야 할 상수들
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "댓글에 대한 권한이 없습니다."),
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."),
    COMMENT_CREATE_SUCCESS(HttpStatus.CREATED, "댓글이 작성되었습니다."),
    COMMENT_UPDATE_SUCCESS(HttpStatus.OK, "댓글이 수정되었습니다."),
    COMMENT_DELETE_SUCCESS(HttpStatus.OK, "댓글이 삭제되었습니다."),
    COMMENT_LIST_SUCCESS(HttpStatus.OK, "댓글 목록 조회에 성공했습니다."),
    COMMENT_DETAIL_SUCCESS(HttpStatus.OK, "댓글 조회에 성공했습니다."),
    COMMENT_INVALID_RATING(HttpStatus.BAD_REQUEST, "별점은 1~5점 사이여야 합니다."),
    COMMENT_REPLY_RATING_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "대댓글에는 별점을 줄 수 없습니다."),
    COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글의 대댓글은 작성할 수 없습니다."),

    /**
     *  database operation response
     */
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 오류가 발생했습니다"),

    /**
     * 유효성 검증 관련
     */
    INVALID_DAYS_RANGE_MIN(HttpStatus.BAD_REQUEST, "조회 기간은 1일 이상이어야 합니다"),
    INVALID_DAYS_RANGE_MAX(HttpStatus.BAD_REQUEST, "조회 기간은 365일을 초과할 수 없습니다"),
    INVALID_PRODUCT_ID(HttpStatus.BAD_REQUEST, "상품 ID는 필수입니다"),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "가입 금액은 0보다 커야 합니다"),
    INVALID_SAVE_TERM(HttpStatus.BAD_REQUEST, "저축 기간은 0보다 커야 합니다"),
    INVALID_MEMBER_ID(HttpStatus.BAD_REQUEST, "회원 ID는 양수여야 합니다"),

    /**
     * Notification response
     */
    NOTIFICATION_LIST_SUCCESS(HttpStatus.OK, "알림 목록 조회에 성공했습니다."),
    NOTIFICATION_READ_SUCCESS(HttpStatus.OK, "알림 읽음 처리에 성공했습니다."),
    NOTIFICATION_READ_ALL_SUCCESS(HttpStatus.OK, "모든 알림 읽음 처리에 성공했습니다."),
    NOTIFICATION_CREATE_SUCCESS(HttpStatus.CREATED, "알림이 생성되었습니다."),
    NOTIFICATION_SETTINGS_UPDATE_SUCCESS(HttpStatus.OK, "알림 설정이 업데이트되었습니다."),
    NOTIFICATION_SETTINGS_GET_SUCCESS(HttpStatus.OK, "알림 설정 조회에 성공했습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "해당 알림에 접근할 권한이 없습니다."),
    NOTIFICATION_ALREADY_READ(HttpStatus.BAD_REQUEST, "이미 읽은 알림입니다."),
    NOTIFICATION_INVALID_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 알림 타입입니다."),

    /**
     * S3 / 파일 스토리지 관련 응답
     */
    S3_PRESIGN_PUT_SUCCESS(HttpStatus.OK, "업로드용 프리사인 URL 발급에 성공했습니다."),
    S3_PRESIGN_GET_SUCCESS(HttpStatus.OK, "다운로드용 프리사인 URL 발급에 성공했습니다."),
    S3_MISSING_PARAMS(HttpStatus.BAD_REQUEST, "필수 파라미터(dir, filename, contentType)가 누락되었습니다."),
    S3_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "필수 파라미터(key)가 누락되었습니다."),
    S3_INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않은 Content-Type 입니다."),
    S3_OPERATION_SUCCESS(HttpStatus.OK, "S3 작업 처리 성공하였습니다."),
    S3_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3 작업 처리 중 오류가 발생했습니다."),
    NOTIFICATION_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림 생성에 실패했습니다."),
    S3_DELETE_SUCCESS(HttpStatus.OK, "S3 파일 삭제 성공"),

    /**
     * 데이터 조회 response
     */
    DATA_FETCH_SUCCESS(HttpStatus.OK, "데이터 수집에 성공했습니다."),
    DATA_STATUS_READ_SUCCESS(HttpStatus.OK, "데이터 상태 조회에 성공했습니다."),
    OPTION_READ_SUCCESS(HttpStatus.OK, "상품 옵션 조회에 성공했습니다."),
    DATA_FETCH_FAILURE(HttpStatus.BAD_REQUEST, "데이터 수집에 실패했습니다."),

    /**
     * Product response
     */
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_READ_SUCCESS(HttpStatus.OK, "상품 목록 조회에 성공했습니다."),
    PRODUCT_FETCH_SUCCESS(HttpStatus.OK, "상품 데이터 수집에 성공했습니다."),
    PRODUCT_FETCH_FAILURE(HttpStatus.BAD_REQUEST, "상품 데이터 수집에 실패했습니다."),

    /**
     * Caption response
     */
    CAPTION_GENERATE_SUCCESS(HttpStatus.OK, "캡션 생성에 성공했습니다."),
    CAPTION_FROM_S3_SUCCESS(HttpStatus.OK, "S3 키로 캡션 생성에 성공했습니다."),
    CAPTION_PRESIGN_ISSUED(HttpStatus.OK, "캡션 업로드용 프리사인 URL 발급에 성공했습니다."),

    /**
     * CardNews response
     */
    CARDNEWS_BUILD_SUCCESS(HttpStatus.OK, "카드뉴스 생성에 성공했습니다."),
    CARDNEWS_BUILD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "카드뉴스 생성에 실패했습니다."),
    CARDNEWS_INVALID_PARAMS(HttpStatus.BAD_REQUEST, "type 또는 id 파라미터가 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;



    ResponseCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
