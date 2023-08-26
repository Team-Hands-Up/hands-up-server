package com.back.handsUp.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.validation.constraints.*;

@NoArgsConstructor
public class UserDto {
    @Getter
    @AllArgsConstructor
    @Builder
    @Setter
    public static class ReqSignUp {
        @Email
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        private String email;

        @Pattern(regexp = "^(?=.*[~!@#$%^&*()_+\\[\\]{}\\\\\\|;:'\\\",\\.\\/<>\\?]).{8,16}$", message = "비밀번호는 특수문자를 포함해 8~16자리 이내여야 합니다." )
        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        private String password;

        @Pattern(regexp = "^[ㄱ-ㅎ가-힣0-9]{2,5}$", message = "닉네임은 특수문자를 제외한 2~5자리여야 합니다." )
        @NotBlank(message = "닉네임은 필수 입력 값입니다.")
        private String nickname;

        private Long characterIdx;
        private String schoolName;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ReqLogIn {
        @Email
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        private String email;

        @Pattern(regexp = "^(?=.*[~!@#$%^&*()_+\\[\\]{}\\\\\\|;:'\\\",\\.\\/<>\\?]).{8,16}$", message = "비밀번호는 특수문자를 포함해 8~16자리 이내여야 합니다." )
        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        private String password;

//        todo : fcmToken frontEnd 구현시 주석 해제
        private String fcmToken;

    }

 
    @AllArgsConstructor
    @Builder
    @Getter
    public static class ReqPwd {
        private String currentPwd;
        private String newPwd;
    }

    @Builder
    public static class ReqWithdraw {
        @JsonProperty
        private Long userIdx;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    public static class ResEmail {
        private String email;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ReqNickname {
        private String nickname;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ReqCheckNickname {
        private String schoolName;
        private String nickname;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ReqStartApp {
        private String fcmToken;
    }
}
