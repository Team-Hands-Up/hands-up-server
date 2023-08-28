package com.back.handsUp.dto.board;

import com.back.handsUp.domain.board.Board;
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
        private String location;
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
//        @JsonProperty
//        private String schoolName;
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
    public static class GetBoardListWithPage {
        @JsonProperty
        private String schoolName;
        @JsonProperty
        private int totalPage;
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
        private String schoolName;
        private String didLike;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class SingleBoardRes {
        private String nickname;
        private String schoolName;
        private String locationAgreement;
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
    public static class ReceivedLikeInfo implements Comparable<ReceivedLikeInfo> {
        private Long boardIdx;
        private String emailFrom;
        private LocalDateTime LikeCreatedAt;
        private String text;
        private String boardContent;
        private CharacterDto.GetCharacterInfo character;
        private Long boardUserIdx; //페이지네이션을 위한 인덱스

        @Override
        public int compareTo(@NotNull ReceivedLikeInfo res) {
            if (res.LikeCreatedAt.isBefore(LikeCreatedAt)) {
                return 1;
            } else if (res.LikeCreatedAt.isAfter(LikeCreatedAt)) {
                return -1;
            }
            return 0;
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class TotalReceivedLikeRes {
        private List<ReceivedLikeInfo> receivedLikeInfo;
        private boolean hasNext; //다음 페이지 존재 여부
    }

    @Getter
    public static class School {
        private String schoolName;
    }
}
