package com.back.handsUp.service;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponseStatus;
import com.back.handsUp.domain.board.*;
import com.back.handsUp.domain.chat.ChatRoom;
import com.back.handsUp.domain.fcmToken.FcmToken;
import com.back.handsUp.domain.user.Character;
import com.back.handsUp.domain.user.School;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.dto.board.BoardDto;
import com.back.handsUp.dto.board.BoardPreviewRes;
import com.back.handsUp.dto.user.CharacterDto;
import com.back.handsUp.repository.board.BoardRepository;
import com.back.handsUp.repository.board.BoardTagRepository;
import com.back.handsUp.repository.board.BoardUserRepository;
import com.back.handsUp.repository.board.TagRepository;
import com.back.handsUp.repository.chat.ChatRoomRepository;
import com.back.handsUp.repository.fcm.FcmTokenRepository;
import com.back.handsUp.repository.user.SchoolRepository;
import com.back.handsUp.repository.user.UserRepository;
import com.back.handsUp.utils.FirebaseCloudMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static com.back.handsUp.baseResponse.BaseResponseStatus.DATABASE_INSERT_ERROR;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BoardService {
    private final BoardRepository boardRepository;
    private final TagRepository tagRepository;
    private final BoardTagRepository boardTagRepository;
    private final UserRepository userRepository;
    private final BoardUserRepository boardUserRepository;
    private final FirebaseCloudMessageService firebaseCloudMessageService;
    private final ChatRoomRepository chatRoomRepository;
    private final FcmTokenRepository fcmTokenRepository;

    private final SchoolRepository schoolRepository;


    //?????? ????????? ??????
    public BoardDto.SingleBoardRes boardViewByIdx(Principal principal, Long boardIdx) throws BaseException {

        //???????????? ??????
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        //???????????? ?????????
        Optional<Board> optionalBoard = this.boardRepository.findByBoardIdx(boardIdx);
        if(optionalBoard.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDIDX);
        }
        Board board = optionalBoard.get();

        //like ??????
        Optional<BoardUser> optionalBoardUserEntity = boardUserRepository.findBoardUserByBoardIdxAndUserIdx(board, user);

        String didLike;
        if(optionalBoardUserEntity.isEmpty()){
            didLike = "false";
        }else {
            BoardUser boardUserEntity = optionalBoardUserEntity.get();
            didLike = boardUserEntity.getStatus();
        }

        //like ????????? ?????? true ??????
        if (Objects.equals(didLike, "LIKE")) {
            didLike = "true";
        }

        //????????? ?????????
        Optional<User> optionalBoardUser = this.boardUserRepository.findUserIdxByBoardIdxAndStatus(boardIdx, "WRITE");
        if(optionalBoardUser.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDUSERIDX);
        }
        User boardUser = optionalBoardUser.get();

        //??????
        Optional<Tag> optionalTag = this.boardTagRepository.findTagIdxByBoardIdx(boardIdx);
        if(optionalTag.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_TAG_VALUE);
        }
        Tag tag = optionalTag.get();

        return BoardDto.SingleBoardRes.builder()
                .content(board.getContent())
                .tag(tag.getName())
                .nickname(boardUser.getNickname())
                .messageDuration(board.getMessageDuration())
                .latitude(board.getLatitude())
                .longitude(board.getLongitude())
                .didLike(didLike)
                .createdAt(board.getCreatedAt()).build();
    }

    //?????? ????????? ??????
    public BoardDto.GetBoardList showBoardList(Principal principal, String schoolName) throws BaseException {

        Optional<School> optionalSchool = schoolRepository.findByName(schoolName);
        if(optionalSchool.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_SCHOOLNAME);
        }
        School school = optionalSchool.get();

        List<BoardDto.BoardWithTag> getBoards = getBoards(principal, school);

        BoardDto.GetBoardList getBoardList = BoardDto.GetBoardList.builder()
                .schoolName(schoolName)
                .getBoardList(getBoards)
                .build();

        return getBoardList;

    }

    // ?????? ?????????(?????? ???) ??????
    // ?????????, ??????(Board), boardIdx
    public BoardDto.GetBoardMapAndSchool showBoardMapList(Principal principal, String schoolName) throws BaseException {

        Optional<School> optionalSchool = schoolRepository.findByName(schoolName);
        
        if(optionalSchool.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_SCHOOLNAME);
        }
        School school = optionalSchool.get();

        List<BoardDto.BoardWithTag> getBoards = getBoards(principal, school);

        List<BoardDto.GetBoardMap> getBoardsMapList = new ArrayList<>();

        for(BoardDto.BoardWithTag b : getBoards) {

            Optional<String> opTagName = this.boardTagRepository.findTagNameByBoard(b.getBoard());
            String tagName;
            if (opTagName.isEmpty()) {
                tagName = null;
            } else tagName = opTagName.get();

            BoardDto.GetBoardMap getBoardMap = BoardDto.GetBoardMap.builder()
                    .boardIdx(b.getBoard().getBoardIdx())
                    .nickname(b.getNickname())
                    .character(b.getCharacter())
                    .latitude(b.getBoard().getLatitude())
                    .longitude(b.getBoard().getLongitude())
                    .createdAt(b.getBoard().getCreatedAt())
                    .tag(tagName)
                    .build();

            getBoardsMapList.add(getBoardMap);
        }

        BoardDto.GetBoardMapAndSchool getBoardMapAndSchool = BoardDto.GetBoardMapAndSchool.builder()
                .schoolName(schoolName)
                .getBoardMap(getBoardsMapList)
                .build();

        return getBoardMapAndSchool;
    }

    //????????? ?????? ?????????,?????? ?????? ??????
    public List<BoardDto.BoardWithTag> getBoards(Principal principal, School school) throws BaseException {
        //???????????? ??????
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        List<BoardUser> getSchoolBoards = boardUserRepository.findBoardBySchoolAndStatus(school, "ACTIVE");

        List<BoardDto.BoardWithTag> getBoards = new ArrayList<>();
        List<Board> blockedBoard = new ArrayList<>();

        LocalDateTime currentTime = LocalDateTime.now();


        for (BoardUser b: getSchoolBoards){
            //?????? ?????? ??????
            Duration timeCheck = Duration.between(b.getBoardIdx().getCreatedAt(),currentTime);
//            log.info("timecheck={}",timeCheck.getSeconds());
            if(timeCheck.getSeconds() > b.getBoardIdx().getMessageDuration() * 3600L) {
                b.getBoardIdx().changeStatus("EXPIRED");
            }

            Optional<BoardUser> optional = this.boardUserRepository.findBoardUserByBoardIdxAndStatus(b.getBoardIdx(), "WRITE").stream().findFirst();
            if (optional.isEmpty()) {
                throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDUSERIDX);
            }
            BoardUser boardUser = optional.get();

            Character character = boardUser.getUserIdx().getCharacter();
            CharacterDto.GetCharacterInfo characterInfo = new CharacterDto.GetCharacterInfo(character.getEye(),
                    character.getEyeBrow(), character.getGlasses(), character.getNose(), character.getMouth(),
                    character.getHair(), character.getHairColor(), character.getSkinColor(), character.getBackGroundColor());


            //?????? ??????
            if(b.getUserIdx()==user && b.getStatus().equals("BLOCK")){
                blockedBoard.add(b.getBoardIdx());
            }else{
                if(!getBoards.contains(b.getBoardIdx()) && b.getBoardIdx().getStatus().equals("ACTIVE")){
                    Board shownBoard = b.getBoardIdx();

                    String tagName;

                    Optional<String> opTagName = this.boardTagRepository.findTagNameByBoard(shownBoard);
                    if (opTagName.isEmpty()) {
                        tagName = null;
                    }else tagName = opTagName.get();


                    BoardDto.BoardWithTag boardWithTag = BoardDto.BoardWithTag.builder()
                            .board(shownBoard)
                            .nickname(boardUser.getUserIdx().getNickname())
                            .character(characterInfo)
                            .tag(tagName).build();

                    getBoards.add(boardWithTag);
                }
            }
        }

        getBoards.removeAll(blockedBoard);


        return getBoards;
    }

    public void likeBoard(Principal principal, Long boardIdx) throws BaseException {

        Optional<Board> optionalBoard = boardRepository.findByBoardIdx(boardIdx);

        if (optionalBoard.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARD_LIST);
        }
        Board board = optionalBoard.get();

        //user : ?????? ????????? ?????????.
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        //boardUser : ????????? ?????????.
        Optional<User> optionalBoardUser = boardUserRepository.findUserIdxByBoardIdxAndStatus(boardIdx, "WRITE");

        if (optionalBoardUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User boardUser = optionalBoardUser.get();

        BoardUser likeEntity = BoardUser.builder()
                .userIdx(user)
                .boardIdx(board)
                .status("LIKE").build();

        try {
            boardUserRepository.save(likeEntity);
        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }

//Todo : User FcmToken ?????? ??? ?????? ??????.
//      ?????? ?????? ?????? ??????.
//
        Optional<FcmToken> optionalFcmToken = fcmTokenRepository.findFcmTokenByUser(boardUser);
        if (optionalFcmToken.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_FCMTOKEN);
        }
        FcmToken fcmToken = optionalFcmToken.get();
        if (!Objects.equals(user.getUserIdx(), boardUser.getUserIdx())) {
            try {
                firebaseCloudMessageService.sendMessageTo(fcmToken.getFcmToken(), boardUser.getNickname(), "???????????? ???????????? ????????? ????????? ???????????????.");
            } catch (Exception e) {
                throw new BaseException(BaseResponseStatus.PUSH_NOTIFICATION_SEND_ERROR);
            }
        }


    }

    public BoardDto.MyBoard viewMyBoard(Principal principal) throws BaseException{
        //long myIdx = 1L; // = jwtService.getUserIdx(token);
        log.info("principal.getName() = {}",principal.getName());
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        Character character = user.getCharacter();


//        List<Board> boardUser = boardUserRepository.findBoardIdxByUserIdxAndStatus(user, "WRITE");



        List<BoardPreviewRes> myBoardList = boardUserRepository.findBoardIdxByUserIdxAndStatus(user, "WRITE").stream()
                .map(Board::toPreviewRes)
                .collect(Collectors.toList());

        BoardDto.MyBoard myBoard = BoardDto.MyBoard.builder().myBoardList(myBoardList).character(character).build();


        return myBoard;
    }

    public void addBoard(Principal principal, BoardDto.GetBoardInfo boardInfo) throws BaseException {

        checkLocationError(boardInfo);

        Board boardEntity = Board.builder()
                .content(boardInfo.getContent())
                .indicateLocation(boardInfo.getIndicateLocation())
                .latitude(boardInfo.getLatitude())
                .longitude(boardInfo.getLongitude())
                .messageDuration(boardInfo.getMessageDuration())
                .build();
        try{
            this.boardRepository.save(boardEntity);
            setTags(boardInfo, boardEntity);
        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }

        Optional<User> optional = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User userEntity = optional.get();
        BoardUser boardUserEntity = BoardUser.builder()
                .boardIdx(boardEntity)
                .userIdx(userEntity)
                .status("WRITE")
                .build();
        try{
            this.boardUserRepository.save(boardUserEntity);
        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }

    }


    //????????? ??????
    public void deleteBoard(Principal principal, Long boardIdx) throws BaseException{
        Optional<User> optional = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_USERIDX);
        }
        User userEntity = optional.get();

        Optional<Board> myBoards = this.boardRepository.findByBoardIdx(boardIdx);
        if(myBoards.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDIDX);
        }
        Board myBoardsEntity = myBoards.get();

        Optional<BoardUser> boardUser = this.boardUserRepository.findBoardUserByBoardIdxAndStatus(myBoardsEntity, "WRITE").stream().findFirst();

        //???????????? ??????????????? ??????
        if(boardUser.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARD_LIST);
        }

        //?????? ???????????? ???????????? ????????? ??????????????? ??????
        if(!Objects.equals(boardUser.get().getUserIdx().getUserIdx(), userEntity.getUserIdx())){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDUSERIDX);
        }

        //?????? ????????? ??????????????? ??????
        if(myBoardsEntity.getStatus().equals("DELETE")){
            throw new BaseException(BaseResponseStatus.ALREADY_DELETE_BOARD);
        }


        myBoards.get().changeStatus("DELETE");


    }

    public void patchBoard(Principal principal, Long boardIdx, BoardDto.GetBoardInfo boardInfo) throws BaseException{
        checkLocationError(boardInfo);

        Optional<Board> optional = this.boardRepository.findByBoardIdx(boardIdx);
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDIDX);
        }
        Board boardEntity = optional.get();


        Optional<User> optional1 = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional1.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User userEntity = optional1.get();

        Optional<BoardUser> optionalBoardUser = this.boardUserRepository.findBoardUserByBoardIdxAndUserIdx(boardEntity, userEntity);
        if(optionalBoardUser.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDUSERIDX);
        }

        boardEntity.changeBoard(boardInfo.getContent(), boardInfo.getLatitude(), boardInfo.getLongitude(), boardInfo.getIndicateLocation(), boardInfo.getMessageDuration());
        try{
            this.boardRepository.save(boardEntity);
        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }

        List<BoardTag> boardTagEntityList = this.boardTagRepository.findAllByBoardIdx(boardEntity);
        for(BoardTag boardTag : boardTagEntityList){
            boardTag.changeStatus("INACTIVE");
        }
        try{
            setTags(boardInfo, boardEntity);
        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }
    }

    private void checkLocationError(BoardDto.GetBoardInfo boardInfo) throws BaseException {
        if(boardInfo.getIndicateLocation().equals("true") && boardInfo.getLatitude() == 0.0 && boardInfo.getLongitude() == 0.0){
            throw new BaseException(BaseResponseStatus.LOCATION_ERROR);
        }

        if(boardInfo.getMessageDuration()<1 || boardInfo.getMessageDuration()>48){
            throw new BaseException(BaseResponseStatus.MESSAGEDURATION_ERROR);
        }
    }

    public String blockBoard(Principal principal, Long boardIdx) throws BaseException {
        String successResult = "???????????? ?????????????????????.";

        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_USERIDX);
        }

        User user = optionalUser.get();

        Optional<Board> optionalBoard = boardRepository.findByBoardIdx(boardIdx);
        if (optionalBoard.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDIDX);
        }

        Board board = optionalBoard.get();

        Optional<BoardUser> optionalBoardUser = boardUserRepository.findBoardUserByBoardIdxAndUserIdx(board, user);

        BoardUser boardUser;


        //BoardUser ????????? ?????? ???
        if (optionalBoardUser.isEmpty()) {
            boardUser = BoardUser.builder()
                    .userIdx(user)
                    .boardIdx(board)
                    .status("BLOCK").build();

            try {
                boardUserRepository.save(boardUser);
                return successResult;
            } catch (Exception e) {
                throw new BaseException(DATABASE_INSERT_ERROR);
            }
        } else if (Objects.equals(optionalBoardUser.get().getStatus(), "LIKE")) { //????????? ?????? ???????????? ???
            boardUser = optionalBoardUser.get();
            boardUser.changeStatus("BLOCK");

            try {
                boardUserRepository.save(boardUser);
                return successResult;
            } catch (Exception e) {
                throw new BaseException(DATABASE_INSERT_ERROR);
            }
        } else if (Objects.equals(optionalBoardUser.get().getStatus(), "WRITE")) { //????????? ???????????? ???
            throw new BaseException(BaseResponseStatus.SELF_BLOCK_ERROR);
        } else if (Objects.equals(optionalBoardUser.get().getStatus(), "BLOCK")) { //?????? ????????? ???????????? ???
            throw new BaseException(BaseResponseStatus.BLOCKED_BOARD_ERROR);
        } else throw new BaseException(DATABASE_INSERT_ERROR); //??? ??? ?????? ??????

    }

    private void setTags(BoardDto.GetBoardInfo boardInfo, Board boardEntity) {
            Optional<Tag> tagEntity = this.tagRepository.findTagByName(boardInfo.getTag());
            Tag targetTag;

            if(tagEntity.isEmpty()){
                targetTag = Tag.builder()
                        .name(boardInfo.getTag())
                        .build();
                this.tagRepository.save(targetTag);
            } else {
               targetTag = tagEntity.get();

            }

            Optional<BoardTag> optional = this.boardTagRepository.findByBoardIdxAndTagIdx(boardEntity, targetTag);
            if(optional.isEmpty()){
                BoardTag boardTagEntity = BoardTag.builder()
                        .boardIdx(boardEntity)
                        .tagIdx(targetTag)
                        .build();
                this.boardTagRepository.save(boardTagEntity);

            } else{
                BoardTag boardTagEntity = optional.get();
                boardTagEntity.changeStatus("ACTIVE");
            }
    }

    //?????? ?????? ?????? ??????
    public List<BoardDto.ReceivedLikeRes> receivedLikeList (Principal principal) throws BaseException {
        Optional<User> optional = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_USERIDX);
        }
        User user = optional.get();

        List<Board> boardList = this.boardUserRepository.findBoardIdxByUserIdxAndStatus(user, "WRITE");
        List<BoardUser> boardUserList = new ArrayList<>();
        for (Board board : boardList){
                boardUserList.addAll(this.boardUserRepository.findBoardUserByBoardIdxAndStatus(board, "LIKE"));
            }

        if(boardUserList.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_LIKE_BOARDS);
        }

        List<BoardDto.ReceivedLikeRes> receivedLikeList = new ArrayList<>();
        for(BoardUser boardUser: boardUserList){
            Character character = boardUser.getUserIdx().getCharacter();
            CharacterDto.GetCharacterInfo characterInfo = new CharacterDto.GetCharacterInfo(character.getEye(),
                    character.getEyeBrow(), character.getGlasses(), character.getNose(), character.getMouth(),
                    character.getHair(), character.getHairColor(), character.getSkinColor(), character.getBackGroundColor());

            Optional<ChatRoom> chatRoomOptional = this.chatRoomRepository.findChatRoomByBoardIdxAndSubUserIdx(boardUser.getBoardIdx(), boardUser.getUserIdx());
            if(chatRoomOptional.isEmpty()){
                throw new BaseException(BaseResponseStatus.NON_EXIST_CHATROOMIDX);
            }

            BoardDto.ReceivedLikeRes receivedLike = BoardDto.ReceivedLikeRes.builder()
                    .emailFrom(boardUser.getUserIdx().getEmail())
                    .text("?????? ?????? "+boardUser.getUserIdx().getNickname()+"?????? ???????????????")
                    .boardContent(boardUser.getBoardIdx().getContent())
                    .LikeCreatedAt(boardUser.getCreatedAt())
                    .character(characterInfo)
                    .chatRoomIdx(chatRoomOptional.get().getChatRoomIdx())
                    .build();
            receivedLikeList.add(receivedLike);
        }
        receivedLikeList.sort(Collections.reverseOrder());
        return receivedLikeList;
    }
}
