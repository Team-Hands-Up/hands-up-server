package com.back.handsUp.dto.chat;

import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.user.Character;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
public class ChatDto {
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ResBoardPreview {
        private Board board;
        private String tag;
        private Character character;
        private String writerEmail;
        private String nickname;
        private String schoolName;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    public static class ReqCreateChat {
        private Long boardIdx;
        private String chatRoomKey;
        private String oppositeEmail;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class ReadChat {
        private String chatRoomKey;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class ResChatList {
        private Long chatRoomIdx;
        private String chatRoomKey;
        private Long boardIdx;
        private Character character;
        private String nickname;
        private LocalDateTime updatedAt;
        private String lastContent;
        private String lastSenderEmail;
        private int notRead;
        private String oppositeEmail;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    public static class ReqCheckKey {
        private Long boardIdx;
        private String oppositeUserEmail;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class ResCheckKey {
        private Board board;
        private Character character;
        private String nickname;
        private String writerEmail;
        private Boolean isSaved;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class ResSendChat {
        private String email;
        private String chatContent;
        private String chatRoomKey;
    }
}
