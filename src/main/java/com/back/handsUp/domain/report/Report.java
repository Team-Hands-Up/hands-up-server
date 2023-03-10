package com.back.handsUp.domain.report;

import com.back.handsUp.baseResponse.BaseEntity;
import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.dto.inquiryAndReport.ReportDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@Table(name = "report")
@NoArgsConstructor
@DynamicInsert
public class Report extends BaseEntity {
    @Id
    @org.springframework.data.annotation.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportIdx;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contents;

    @Column(columnDefinition = "varchar(10) default 'ACTIVE'")
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userIdx")
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportedUserIdx")
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportedBoardIdx")
    @Nullable
    private Board reportedBoard;

    @Builder
    public Report(Long reportIdx, String contents, String status, User user, User reportedUser, @Nullable Board reportedBoard) {
        this.reportIdx = reportIdx;
        this.contents = contents;
        this.status = status;
        this.user = user;
        this.reportedUser = reportedUser;
        this.reportedBoard = reportedBoard;
    }

    public ReportDto.GetAllReport ToGetReport() {
        long reportedBoardIdx;
        String reportedBoardContents;
        if (this.reportedBoard == null) {
            reportedBoardIdx = 0;
            reportedBoardContents = null;
        }else {
            reportedBoardIdx = this.reportedBoard.getBoardIdx();
            reportedBoardContents = this.reportedBoard.getContent();
        }
        return ReportDto.GetAllReport.builder().content(this.contents)
                .userIdx(user.getUserIdx())
                .userNickname(user.getNickname())
                .reportedUserIdx(this.reportedUser.getUserIdx())
                .reportedUserNickname(this.reportedUser.getNickname())
                .reportedBoardIdx(reportedBoardIdx)
                .reportedBoardContents(reportedBoardContents)
                .build();
    }
}
