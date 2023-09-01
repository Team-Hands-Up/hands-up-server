package com.back.handsUp.controller;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponse;
import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.chat.ChatMessage;
import com.back.handsUp.dto.chat.ChatDto;
import com.back.handsUp.dto.user.UserDto;
import com.back.handsUp.service.BoardService;
import com.back.handsUp.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/chats")
@RestController
public class ChatController {
    private final ChatService chatService;
    private final BoardService boardService;
    //채팅방 내 게시글 미리보기
    @ResponseBody
    @GetMapping("/{boardIdx}")
    public BaseResponse<ChatDto.ResBoardPreview> getPreViewBoard(Principal principal, @PathVariable Long boardIdx){
        try {
            ChatDto.ResBoardPreview boardPreview = this.chatService.getPreViewBoard(principal,boardIdx);
            return new BaseResponse<>(boardPreview);
        }catch (BaseException exception){
            return new BaseResponse<>((exception.getStatus()));
        }
    }


    @PostMapping("/block/{chatRoomIdx}")
    public BaseResponse<String> blockChatAndBoards(Principal principal, @PathVariable Long chatRoomIdx) {

        try {
            String result = chatService.blockChatAndBoards(principal, chatRoomIdx);

            return new BaseResponse<>(result);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @PostMapping("/alarm")
    public BaseResponse<String> chatAlarm(Principal principal, @RequestBody ChatDto.ResSendChat sendChat) {

        try {
            String result = chatService.chatAlarm(principal, sendChat);

            return new BaseResponse<>(result);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @PostMapping("/create")
    public BaseResponse<String> createChat(Principal principal, @RequestBody ChatDto.ReqCreateChat reqCreateChat) {

        try {
            String result = chatService.createChat(principal, reqCreateChat);

            return new BaseResponse<>(result);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @GetMapping("/list")
    public BaseResponse<List<ChatDto.ResChatList>> viewChatList(Principal principal, Pageable pageable) {

        try {
            List<ChatDto.ResChatList> result = chatService.viewAllList(principal, pageable);

            return new BaseResponse<>(result);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @GetMapping("/check-key/{chatRoomKey}/{boardIdx}/{oppositeUserEmail}")
    public BaseResponse<ChatDto.ResCheckKey> checkChatKeySaved(Principal principal,
                                                               @PathVariable String chatRoomKey,
                                                               @PathVariable Long boardIdx,
                                                               @PathVariable String oppositeUserEmail) {
        try {
            ChatDto.ResCheckKey result = chatService.checkChatKeySaved(principal, boardIdx, oppositeUserEmail, chatRoomKey);
            return new BaseResponse<>(result);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @PostMapping("/read")
    public BaseResponse<String> readChat(Principal principal, @RequestBody ChatDto.ReadChat resRead) {
        try {
            String result = chatService.readChat(principal, resRead);
            return new BaseResponse<>(result);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @DeleteMapping("/{chatRoomKey}")
    public BaseResponse<String> deleteChatRoom(Principal principal, @PathVariable String chatRoomKey){
        try {
            chatService.deleteChatRoom(principal, chatRoomKey);
            return new BaseResponse<>("chatRoomKey가 "+chatRoomKey+"인 채팅방이 삭제되었습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }
}
