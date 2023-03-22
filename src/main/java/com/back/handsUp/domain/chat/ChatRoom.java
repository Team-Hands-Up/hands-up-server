package com.back.handsUp.domain.chat;

import com.back.handsUp.baseResponse.BaseEntity;
import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.*;
import java.util.List;

@Entity
@Setter
@Getter
@Table(name = "chat_room")
@NoArgsConstructor
@DynamicInsert
public class ChatRoom extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomIdx;

    @Column(nullable = false)
    private String chatRoomKey;

    @OneToOne
    @JoinColumn(name = "boardIdx")
    private Board boardIdx;

    @OneToOne
    @JoinColumn(name = "subUserIdx")
    private User subUserIdx;

    @OneToOne
    @JoinColumn(name = "hostUserIdx")
    private User hostUserIdx;

    @Column(columnDefinition = "varchar(10) default 'ACTIVE'")
    private String status;

    private String lastChatContent;

    private int notRead;

    @OneToOne
    @JoinColumn(name = "lastSender")
    private User lastSender;

    @Builder
    public ChatRoom(Board boardIdx, User subUserIdx, User hostUserIdx, String chatRoomKey) {
        this.boardIdx = boardIdx;
        this.subUserIdx = subUserIdx;
        this.hostUserIdx = hostUserIdx;
        this.chatRoomKey = chatRoomKey;
    }

    public void changeLastContent(String lastChatContent, User lastSender) {
        this.lastChatContent = lastChatContent;
        this.lastSender = lastSender;
    }

    public void plusNotRead() {
        this.notRead += 1;
    }

    public void read() {
        this.notRead = 0;
    }

}
