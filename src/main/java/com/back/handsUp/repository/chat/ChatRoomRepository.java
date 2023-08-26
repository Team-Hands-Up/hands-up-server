package com.back.handsUp.repository.chat;

import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.chat.ChatRoom;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.dto.chat.ChatDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByChatRoomIdx(Long chatRoomIdx);
    Optional<ChatRoom> findChatRoomByBoardIdxAndSubUserIdx(Board boardIdx, User subUserIdx);

    @Query("select c from ChatRoom c where (c.hostUserIdx = ?1 or c.subUserIdx= ?1) and c.status = 'ACTIVE'")
    List<ChatRoom> findChatRoomByUserIdx(User userIdx);

    Optional<ChatRoom> findChatRoomByChatRoomKey(String chatRoomKey);

    @Query("select c from ChatRoom c where c.boardIdx = ?1")
    Optional<ChatRoom> findChatRoomByBoardIdx(Board boardIdx);

    Page<ChatRoom> findAllProjectedByHostUserIdxOrSubUserIdxOrderByUpdatedAtDesc(User hostUserIdx, User subUserIdx, Pageable pageable);
}
