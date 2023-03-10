package com.back.handsUp.dto.chat;

import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.user.Character;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
public class ChatDto {
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ResChat {
        private Board board;
        private Character character;
        private String nickname;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    public static class ReqCreateChat {
        private Long boardIdx;
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
        private Character character;
        private String nickname;
    }
}
