package com.back.handsUp.dto.board;

import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.board.BoardUser;
import com.back.handsUp.dto.user.CharacterDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import com.back.handsUp.domain.user.Character;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public class BoardDto {
    @Getter
    @AllArgsConstructor
    @Builder
    public static class GetBoardInfo {
        private String indicateLocation;
        private double latitude;
        private double longitude;
        private String content;
        private String tag;
        private int messageDuration;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GetBoardMap {
        private Long boardIdx;
        private String nickname;
        private CharacterDto.GetCharacterInfo character;
        private double latitude;
        private double longitude;
        private LocalDateTime createdAt;
        private String tag;
    }

    @Builder
    public static class GetBoardMapAndSchool {
        @JsonProperty
        private String schoolName;
        @JsonProperty
        private List<GetBoardMap> getBoardMap;
    }

    @Builder
    public static class GetBoardList {
        @JsonProperty
        private String schoolName;
        @JsonProperty
        private List<BoardWithTag> getBoardList;
    }

    @Builder
    @Getter
    public static class BoardWithTag {
        private Board board;
        private String nickname;
        private CharacterDto.GetCharacterInfo character;
        private String tag;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class SingleBoardRes {
        private String nickname;
        private double latitude;
        private double longitude;
        private String content;
        private String tag;
        private String didLike;
        private int messageDuration;
        private LocalDateTime createdAt;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class BriefBoard {
        private Long boardIdx;
        private double latitude;
        private double longitude;
        private String content;
        private LocalDateTime createdAt;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class MyBoard {
        private Character character;
        private List<BoardPreviewRes> myBoardList;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ReceivedLikeRes implements Comparable<ReceivedLikeRes> {
        private Long chatRoomIdx;
        private String emailFrom;
        private LocalDateTime LikeCreatedAt;
        private String text;
        private String boardContent;
        private CharacterDto.GetCharacterInfo character;

        @Override
        public int compareTo(@NotNull ReceivedLikeRes res) {
            if (res.LikeCreatedAt.isBefore(LikeCreatedAt)) {
                return 1;
            } else if (res.LikeCreatedAt.isAfter(LikeCreatedAt)) {
                return -1;
            }
            return 0;
        }
    }

    @Getter
    public static class School {
        private String schoolName;
    }
}
