package com.back.handsUp.service;

import static com.back.handsUp.baseResponse.BaseResponseStatus.*;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponseStatus;
import com.back.handsUp.domain.Notification;
import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.board.BoardUser;
import com.back.handsUp.domain.chat.ChatRoom;
import com.back.handsUp.domain.fcmToken.FcmToken;
import com.back.handsUp.domain.user.Character;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.dto.chat.ChatDto;
import com.back.handsUp.dto.user.UserDto;
import com.back.handsUp.repository.NotificationRepository;
import com.back.handsUp.repository.board.BoardRepository;
import com.back.handsUp.repository.board.BoardTagRepository;
import com.back.handsUp.repository.board.BoardUserRepository;
import com.back.handsUp.repository.chat.ChatRoomRepository;
import com.back.handsUp.repository.fcm.FcmTokenRepository;
import com.back.handsUp.repository.user.UserRepository;
import com.back.handsUp.utils.FirebaseCloudMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.swing.text.html.Option;
import javax.transaction.Transactional;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final BoardUserRepository boardUserRepository;
    private final BoardRepository boardRepository;
    private final BoardService boardService;
    private final FcmTokenRepository fcmTokenRepository;
    private final FirebaseCloudMessageService firebaseCloudMessageService;
    private final BoardTagRepository boardTagRepository;

    private final NotificationRepository notificationRepository;
    //채팅방 내 게시물 미리보기
    public ChatDto.ResBoardPreview getPreViewBoard(Principal principal, Long boardIdx) throws BaseException {
        Optional<User> optional = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User loginUser = optional.get();

        Optional<Board> boardOptional = this.boardRepository.findByBoardIdx(boardIdx);
        if(boardOptional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDIDX);
        }
        Board board = boardOptional.get();

        //태그 가져오기
        String tagName = this.boardTagRepository.findTagNameByBoard(board).orElse(null);

//        Optional<ChatRoom> optional1 = this.chatRoomRepository.findChatRoomByChatRoomKey(chatRoomKey);
//        Optional<ChatRoom> optional1 = this.chatRoomRepository.findChatRoomByBoardIdx(board);
//        if(optional1.isEmpty()){
//            throw new BaseException(BaseResponseStatus.NON_EXIST_CHATROOMIDX);
//        }
//        ChatRoom chatRoom = optional1.get();

//        if(loginUser.equals(chatRoom.getHostUserIdx()) || loginUser.equals(chatRoom.getSubUserIdx())){
//        } else {
//            throw new BaseException(BaseResponseStatus.NON_EXIST_CHATROOM_USER);
//        }

        Optional<BoardUser> optional2 = this.boardUserRepository.findBoardUserByBoardIdxAndStatus(board, "WRITE")
                .stream().findFirst();
        if(optional2.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDUSERIDX);
        }
        BoardUser boardUser = optional2.get();

        ChatDto.ResBoardPreview boardPreview = ChatDto.ResBoardPreview.builder()
                .board(board)
                .tag(tagName)
                .character(loginUser.getCharacter())
                .nickname(boardUser.getUserIdx().getNickname())
                .build();

        return boardPreview;
    }

    public String blockChatAndBoards(Principal principal, Long chatRoomIdx) throws BaseException {
        Optional<ChatRoom> optionalChatRoom = chatRoomRepository.findByChatRoomIdx(chatRoomIdx);
        if(optionalChatRoom.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_CHATROOMIDX);
        }

        ChatRoom chatRoom = optionalChatRoom.get();
        chatRoom.setStatus("INACTIVE");
        String result = "채팅방을 차단하였습니다.\n";
        Board board = chatRoom.getBoardIdx();
        result += boardService.blockBoard(principal, board.getBoardIdx());
        return result;
    }

    public String chatAlarm(Principal principal, ChatDto.ResSendChat sendChat) throws BaseException {
        Optional<User> optionalMe = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalMe.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User me = optionalMe.get();

        Optional<ChatRoom> opChatRoom = chatRoomRepository.findByChatRoomIdx(sendChat.getChatRoomIdx());
        String chatContent = sendChat.getChatContent();
        if (opChatRoom.isEmpty()) {
            throw new BaseException(NON_EXIST_CHATROOMIDX);
        }
        ChatRoom chatRoom = opChatRoom.get();
        chatRoom.changeLastContent(chatContent, me);
        chatRoom.plusNotRead();



        Optional<User> optionalUser = userRepository.findByEmailAndStatus(sendChat.getEmail(), "ACTIVE");

        if (optionalUser.isEmpty()) {

            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User you = optionalUser.get();
        Optional<FcmToken> optionalFcmToken = fcmTokenRepository.findFcmTokenByUser(you);
        if (optionalFcmToken.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_FCMTOKEN);
        }
        FcmToken fcmToken = optionalFcmToken.get();

        Notification notificationEntity = Notification.builder()
                .userIdx(you)
                .title(me.getNickname())
                .body("채팅이 도착하였습니다.")
                .build();
        try {
            notificationRepository.save(notificationEntity);
            firebaseCloudMessageService.sendMessageTo(fcmToken.getFcmToken(), me.getNickname(), "채팅이 도착하였습니다.");
            return "채팅 알림을 성공적으로 보냈습니다.";
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.PUSH_NOTIFICATION_SEND_ERROR);
        }
    }

    //채팅방 개설
    public String createChat(Principal principal, ChatDto.ReqCreateChat reqCreateChat) throws BaseException{
        if (this.chatRoomRepository.findChatRoomByChatRoomKey(reqCreateChat.getChatRoomKey()).isPresent()) {
            throw new BaseException(EXIST_CHATROOMKEY);
        }

        Optional<User> optionalMe = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if (optionalMe.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        //채팅방을 개설할 때는 사용자가 게시글 작성자에게 채팅을 보냄 -> 개설자는 항상 참여자, 작성자는 항상 호스트.
        User subUser = optionalMe.get();

        Long boardIdx = reqCreateChat.getBoardIdx();

        Optional<Board> optionalBoard = this.boardRepository.findByBoardIdx(boardIdx);
        if (optionalBoard.isEmpty()) {
            throw new BaseException(NON_EXIST_BOARDIDX);
        }
        Board board = optionalBoard.get();

        Optional<User> optionalHostUser = this.boardUserRepository.findUserIdxByBoardIdxAndStatus(boardIdx, "WRITE");
        if (optionalHostUser.isEmpty()) {
            throw new BaseException(NON_EXIST_USERIDX);
        }
        User hostUser = optionalHostUser.get();

        ChatRoom chatRoom = ChatRoom.builder()
                .boardIdx(board)
                .subUserIdx(subUser)
                .hostUserIdx(hostUser)
                .chatRoomKey(reqCreateChat.getChatRoomKey()).build();

        try {
            this.chatRoomRepository.save(chatRoom);
            return "채팅방을 개설하였습니다.";
        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }
    }

    public List<ChatDto.ResChatList> viewAllList(Principal principal, Pageable pageable) throws BaseException {
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if (optionalUser.isEmpty()) {
            throw new BaseException(NON_EXIST_USERIDX);
        }
        User user = optionalUser.get();

        Page<ChatRoom> chatRooms = chatRoomRepository.findAllProjectedByHostUserIdxOrSubUserIdxOrderByUpdatedAtDesc(user, user, pageable);

        return chatRooms.getContent().stream().map(chatRoom -> {
            ChatDto.ResChatList chatRoomDto = new ChatDto.ResChatList();

            chatRoomDto.setChatRoomIdx(chatRoom.getChatRoomIdx());
            chatRoomDto.setChatRoomKey(chatRoom.getChatRoomKey());
            chatRoomDto.setUpdatedAt(chatRoom.getUpdatedAt());
            chatRoomDto.setNotRead(chatRoom.getNotRead());
            if (chatRoom.getLastChatContent()==null) {
                chatRoomDto.setLastContent("아직 채팅이 시작되지 않았습니다[NULL]");
            }else chatRoomDto.setLastContent(chatRoom.getLastChatContent());
            if (chatRoom.getLastSender()==null) {
                chatRoomDto.setLastSenderEmail("아직 채팅이 시작되지 않았습니다[NULL]");
            }else chatRoomDto.setLastSenderEmail(chatRoom.getLastSender().getEmail());
            User hostUser = chatRoom.getHostUserIdx();
            User subUser = chatRoom.getSubUserIdx();
            if (Objects.equals(hostUser.getUserIdx(), user.getUserIdx())) {
                chatRoomDto.setCharacter(subUser.getCharacter());
                chatRoomDto.setNickname(subUser.getNickname());
            } else {
                chatRoomDto.setCharacter(hostUser.getCharacter());
                chatRoomDto.setNickname(hostUser.getNickname());
            }

            return chatRoomDto;
        }).collect(Collectors.toList());
    }

    public ChatDto.ResCheckKey checkChatKeySaved(Principal principal,
                                                 Long boardIdx,
                                                 String oppositeUserEmail,
                                                 String chatRoomKey) throws BaseException {
        Optional<User> opOppositeUser = this.userRepository.findByEmailAndStatus(oppositeUserEmail, "ACTIVE");
        if (opOppositeUser.isEmpty()) {
            throw new BaseException(NON_EXIST_USERIDX);
        }
        User oppositeUser = opOppositeUser.get();

        Optional<ChatRoom> optionalChatRoom = this.chatRoomRepository.findChatRoomByChatRoomKey(chatRoomKey);
        Optional<Board> opBoard = this.boardRepository.findByBoardIdx(boardIdx);
        if (opBoard.isEmpty()) {
            throw new BaseException(NON_EXIST_BOARDIDX);
        }
        Board board = opBoard.get();

        //opWriter - 게시물 작성자
        Optional<User> opWriter = boardUserRepository.findUserIdxByBoardIdxAndStatus(boardIdx, "WRITE");
        if (opWriter.isEmpty()) {
            throw new BaseException(NON_EXIST_USERIDX);
        }
        User writer = opWriter.get();

        ChatDto.ResCheckKey result = ChatDto.ResCheckKey.builder()
                .isSaved(optionalChatRoom.isPresent())
                .board(board)
                .character(oppositeUser.getCharacter())
                .writerEmail(writer.getEmail())
                .nickname(oppositeUser.getNickname()).build();

        return result;
    }

    public String readChat(Principal principal, ChatDto.ReadChat resRead) throws BaseException{
        log.info("chat room idx = {}", resRead.getChatRoomIdx());
        Optional<User> opMe = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if (opMe.isEmpty()) {
            throw new BaseException(NON_EXIST_USERIDX);
        }
        User me = opMe.get();
        Optional<ChatRoom> opChatRoom = chatRoomRepository.findByChatRoomIdx(resRead.getChatRoomIdx());
        if (opChatRoom.isEmpty()) {
            throw new BaseException(NON_EXIST_CHATROOMIDX);
        }
        ChatRoom chatRoom = opChatRoom.get();

        if (Objects.equals(me.getUserIdx(), chatRoom.getLastSender().getUserIdx())) {
            return "변화 없음";
        }else {
            chatRoom.read();
            return "채팅을 읽었습니다.";
        }
    }
}
