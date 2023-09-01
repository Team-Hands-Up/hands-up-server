package com.back.handsUp.baseResponse;

import lombok.Getter;

@Getter
public enum BaseResponseStatus {

    /**
     * 2000번대 성공
     */
    SUCCESS(true, 2000, "요청에 성공하였습니다."),

    /**
     * 4000번대 클라이언트 에러
     */
    NON_EXIST_EMAIL(false, 4000, "존재하지 않는 이메일입니다."),
    INVALID_PASSWORD(false, 4001, "비밀번호가 틀렸습니다."),
    EXIST_USER(false, 4002, "이미 존재하는 유저입니다."),
    INVALID_REQUEST(false, 4003, "입력 양식이 잘못되었습니다."),
    SAME_PASSWORD(false, 4004, "현재 비밀번호와 변경할 비밀번호가 같습니다."),
    LIMIT_NICKNAME_CHANGE(false, 4005, "마지막 닉네임 변경일로부터 7일이 경과하지 않았습니다."),
    EXIST_NICKNAME(false, 4006, "이미 존재하는 닉네임입니다."),


    NON_EXIST_BOARDIDX(false, 4010, "게시물 인덱스가 존재하지 않습니다."),
    NON_EXIST_USERIDX(false, 4011, "유저 인덱스가 존재하지 않습니다."),
    EMPTY_USER(false, 4012, "공백없이 입력해주세요."),
    NON_EXIST_CHARACTERIDX(false, 4013, "캐릭터 인덱스가 존재하지 않습니다."),
    NON_EXIST_SCHOOLIDX(false, 4014, "학교 인덱스가 존재하지 않습니다."),
    NON_EXIST_BOARDUSERIDX(false, 4015, "유저가 작성한 게시물이 아닙니다."),
    NON_EXIST_BOARD_LIST(false, 4016, "게시물이 존재하지 않습니다."),
    NON_EXIST_CHATROOMKEY(false, 4017, "채팅방이 존재하지 않습니다."),
    ALREADY_DELETE_USER(false, 4017, "이미 탈퇴한 회원입니다."),
    NON_CORRESPOND_USER(false, 4018, "userIdx가 로그인한 유저와 일치하지 않습니다."),
    NON_EXIST_SCHOOLNAME(false, 4019, "학교명이 존재하지 않습니다."),
    ALREADY_DELETE_BOARD(false, 4020, "이미 삭제된 게시물입니다."),


    LOCATION_ERROR(false, 4021, "위치정보에 오류가 발생했습니다."),
    MESSAGEDURATION_ERROR(false, 4022, "메세지 지속시간에 오류가 발생했습니다."),

    NON_EXIST_CHARACTER_VALUE(false, 4031, "캐릭터 생성에 필요한 값이 모두 입력되지 않았습니다."),

    NON_EXIST_TAG_VALUE(false, 4032, "유효하지 않은 테그값입니다."),

    REFRESH_TOKEN_ERROR(false, 4040, "Refresh Token이 유효하지 않습니다."),
    LOGOUT_USER(false, 4041, "로그아웃된 사용자입니다."),
    NOT_MATCH_TOKEN(false, 4042, "토큰의 유저 정보가 일치하지 않습니다."),
    MALFORMED_JWT(false, 4043, "잘못된 JWT 서명입니다."),
    EXPIRED_JWT(false, 4044, "만료된 JWT 토큰입니다."),
    UNSUPPORTED_JWT(false, 4045, "지원되지 않는 JWT 토큰입니다."),
    ILLEGAL_JWT(false, 4046, "JWT 토큰이 잘못되었습니다."),
    JWT_ERROR(false, 4047, "JWT 토큰에 오류가 발생했습니다."),
    ACCESS_DENIED_USER(false, 4048, "접근 권한이 없는 유저입니다"),
    SELF_REPORT_ERROR(false, 4049, "본인을 신고할 수 없습니다."),
    REPORTED_ERROR(false, 4050, "이미 신고가 접수되었습니다."),
    SELF_BLOCK_ERROR(false, 4051, "본인을 차단할 수 없습니다."),
    BLOCKED_USER_ERROR(false, 4052, "이미 차단된 유저입니다."),
    BLOCKED_BOARD_ERROR(false, 4053, "이미 차단된 게시물입니다."),
    NON_EXIST_FCMTOKEN(false, 4054, "상대방의 FCM 토큰이 없습니다."),

    EXIST_CHATROOMKEY(false, 4055, "이미 존재하는 채팅방 키입니다."),
    NON_EXIST_LIKE_BOARDS(false, 4056, "다른 사람이 좋아요를 누른 게시물이 없습니다."),
    NON_EXIST_CHATROOM_USER(false, 4057, "채팅방 내 존재하는 유저가 아닙니다."),


    /**
     * 5000번대 서버 에러
     */
    DATABASE_INSERT_ERROR(false, 5000, "데이터베이스 저장 오류가 발생했습니다."),
    PASSWORD_ENCRYPTION_ERROR(false, 5001, "비밀번호 암호화 오류가 발생했습니다."),
    EMAIL_SEND_ERROR(false, 5002, "이메일 인증번호 발송에 오류가 발생했습니다."),
    PUSH_NOTIFICATION_SEND_ERROR(false, 5003, "알림을 보내는데 실패하였습니다."),
    DATABASE_DELETE_ERROR(false, 5004, "데이터를 삭제하는데 오류가 발생하였습니다.");

    private final boolean isSuccess;
    private final int statusCode;
    private final String message;

    BaseResponseStatus(boolean isSuccess, int statusCode, String message) {
        this.isSuccess = isSuccess;
        this.statusCode = statusCode;
        this.message = message;
    }
}
