package com.back.handsUp.service;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponseStatus;
import com.back.handsUp.domain.Notification;
import com.back.handsUp.domain.board.*;
import com.back.handsUp.domain.fcmToken.FcmToken;
import com.back.handsUp.domain.user.Character;
import com.back.handsUp.domain.user.School;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.dto.board.BoardDto;
import com.back.handsUp.dto.board.BoardPreviewRes;
import com.back.handsUp.dto.user.CharacterDto;
import com.back.handsUp.repository.NotificationRepository;
import com.back.handsUp.repository.board.BoardRepository;
import com.back.handsUp.repository.board.BoardTagRepository;
import com.back.handsUp.repository.board.BoardUserRepository;
import com.back.handsUp.repository.board.TagRepository;
import com.back.handsUp.repository.fcm.FcmTokenRepository;
import com.back.handsUp.repository.user.SchoolRepository;
import com.back.handsUp.repository.user.UserRepository;
import com.back.handsUp.utils.FirebaseCloudMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.back.handsUp.baseResponse.BaseResponseStatus.*;

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
    private final FcmTokenRepository fcmTokenRepository;

    private final SchoolRepository schoolRepository;
    private final NotificationRepository notificationRepository;


    //단일 게시물 조회
    public BoardDto.SingleBoardRes boardViewByIdx(Principal principal, Long boardIdx) throws BaseException {

        //조회하는 유저
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        //조회하는 게시물
        Optional<Board> optionalBoard = this.boardRepository.findByBoardIdx(boardIdx);
        if(optionalBoard.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDIDX);
        }
        Board board = optionalBoard.get();

        //like 확인
        Optional<BoardUser> optionalBoardUserEntity = boardUserRepository.findBoardUserByBoardIdxAndUserIdx(board, user);

        String didLike;
        if(optionalBoardUserEntity.isEmpty()){
            didLike = "false";
        }else {
            BoardUser boardUserEntity = optionalBoardUserEntity.get();
            didLike = boardUserEntity.getStatus();
        }

        //like 눌렀을 때만 true 반환
        if (Objects.equals(didLike, "LIKE")) {
            didLike = "true";
        }

        //게시글 작성자
        Optional<User> optionalBoardUser = this.boardUserRepository.findUserIdxByBoardIdxAndStatus(boardIdx, "WRITE");
        if(optionalBoardUser.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDUSERIDX);
        }
        User boardUser = optionalBoardUser.get();

        //태그
        Optional<Tag> optionalTag = this.boardTagRepository.findTagIdxByBoardIdx(boardIdx);
        if(optionalTag.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_TAG_VALUE);
        }
        Tag tag = optionalTag.get();

        return BoardDto.SingleBoardRes.builder()
                .content(board.getContent())
                .tag(tag.getName())
                .nickname(boardUser.getNickname())
                .schoolName(boardUser.getSchoolIdx().getName())
                .messageDuration(board.getMessageDuration())
                .locationAgreement(board.getIndicateLocation())
                .latitude(board.getLatitude())
                .longitude(board.getLongitude())
                .didLike(didLike)
                .createdAt(board.getCreatedAt()).build();
    }

    //전체 게시물 조회
    public BoardDto.GetBoardListWithPage showBoardList(Principal principal, int page) throws BaseException {

        //학교 이름으로 찾기
//        Optional<School> optionalSchool = schoolRepository.findByName(schoolName);
//        if(optionalSchool.isEmpty()) {
//            throw new BaseException(BaseResponseStatus.NON_EXIST_SCHOOLNAME);
//        }
//        School school = optionalSchool.get();

        //조회하는 유저
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        return getBoardsWithPage(principal, user.getSchoolIdx(), page);
    }


    // 전체 게시물(지도 상) 조회
    // 캐릭터, 위치(Board), boardIdx
    public BoardDto.GetBoardMapAndSchool showBoardMapList(Principal principal) throws BaseException {

        //학교 정보
//        Optional<School> optionalSchool = schoolRepository.findByName(schoolName);
//
//        if(optionalSchool.isEmpty()) {
//            throw new BaseException(BaseResponseStatus.NON_EXIST_SCHOOLNAME);
//        }
//        School school = optionalSchool.get();
        //조회하는 유저
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        List<BoardDto.BoardWithTag> getBoards = getBoardsWithOutPage(principal, user.getSchoolIdx());

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
//                .schoolName(schoolName)
                .getBoardMap(getBoardsMapList)
                .build();

        return getBoardMapAndSchool;
    }

    //게시물 조회 리스트,지도 중복 코드
    public BoardDto.GetBoardListWithPage getBoardsWithPage(Principal principal, School school, int page) throws BaseException {
        //조회하는 유저
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        // 같은 학교의 게시물만 조회 시
//        List<BoardUser> getSchoolBoards = boardUserRepository.findBoardBySchoolAndStatus(school, "ACTIVE");

        // 학교 상관 없이 모든 게시물
        List<BoardUser> getSchoolBoards = boardUserRepository.findBoardUserByStatus("ACTIVE");
        log.info("getSchoolBoards size={}", getSchoolBoards.size());

        List<BoardDto.BoardWithTag> getBoards = new ArrayList<>();

        LocalDateTime currentTime = LocalDateTime.now();

        for (BoardUser b: getSchoolBoards) {
            //시간 만료 체크
            Duration timeCheck = Duration.between(b.getBoardIdx().getCreatedAt(), currentTime);
//            log.info("timecheck={}",timeCheck.getSeconds());
            if (timeCheck.getSeconds() > b.getBoardIdx().getMessageDuration() * 3600L) {
                b.getBoardIdx().changeStatus("EXPIRED");
            }
        }
        // 페이지 1부터 시작
        PageRequest pageRequest = PageRequest.of(page-1, 10);
        //차단 체크
        Page<BoardUser> nonBlockBoardsWithPage = boardUserRepository.findNotBlockedBoardsByUserIdxWithPage(user, pageRequest);

        List<BoardUser> nonBlockBoards = nonBlockBoardsWithPage.getContent();

        addGetBoards(user, getBoards, nonBlockBoards);

        return BoardDto.GetBoardListWithPage.builder()
                .getBoardList(getBoards)
                .totalPage(nonBlockBoardsWithPage.getTotalPages())
                .schoolName(user.getSchoolIdx().getName())
                .build();

    }

    public List<BoardDto.BoardWithTag> getBoardsWithOutPage(Principal principal, School school) throws BaseException {
        //조회하는 유저
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        // 같은 학교의 게시물만 조회 시
//        List<BoardUser> getSchoolBoards = boardUserRepository.findBoardBySchoolAndStatus(school, "ACTIVE");

        // 학교 상관 없이 모든 게시물
        List<BoardUser> getSchoolBoards = boardUserRepository.findBoardUserByStatus("ACTIVE");
        log.info("getSchoolBoards size={}", getSchoolBoards.size());

        List<BoardDto.BoardWithTag> getBoards = new ArrayList<>();

        LocalDateTime currentTime = LocalDateTime.now();

        for (BoardUser b: getSchoolBoards) {
            //시간 만료 체크
            Duration timeCheck = Duration.between(b.getBoardIdx().getCreatedAt(), currentTime);
//            log.info("timecheck={}",timeCheck.getSeconds());
            if (timeCheck.getSeconds() > b.getBoardIdx().getMessageDuration() * 3600L) {
                b.getBoardIdx().changeStatus("EXPIRED");
            }
        }
        //차단 체크
        List<BoardUser> nonBlockBoards = boardUserRepository.findNotBlockedBoardsByUserIdx(user);

        addGetBoards(user, getBoards, nonBlockBoards);

        return getBoards;
    }

    private void addGetBoards(User user, List<BoardDto.BoardWithTag> getBoards, List<BoardUser> nonBlockBoards) {
        for(BoardUser b1: nonBlockBoards){

            Character character = b1.getUserIdx().getCharacter();
            CharacterDto.GetCharacterInfo characterInfo = new CharacterDto.GetCharacterInfo(character.getEye(),
                    character.getEyeBrow(), character.getGlasses(), character.getNose(), character.getMouth(),
                    character.getHair(), character.getHairColor(), character.getSkinColor(), character.getBackGroundColor());

            Board shownBoard = b1.getBoardIdx();

            String tagName;

            Optional<String> opTagName = this.boardTagRepository.findTagNameByBoard(shownBoard);
            if (opTagName.isEmpty()) {
                tagName = null;
            }else tagName = opTagName.get();

            //like 확인
            Optional<BoardUser> opBoard = boardUserRepository.findBoardUserByBoardIdxAndUserIdx(shownBoard, user);
            String didLike;
            if(opBoard.isEmpty()){
                didLike = "false";
            }else {
                BoardUser boardUserEntity = opBoard.get();
                didLike = boardUserEntity.getStatus();
            }
            //like 눌렀을 때만 true 반환
            if (Objects.equals(didLike, "LIKE")) {
                didLike = "true";
            }

            BoardDto.BoardWithTag boardWithTag = BoardDto.BoardWithTag.builder()
                    .board(shownBoard)
                    .nickname(b1.getUserIdx().getNickname())
                    .character(characterInfo)
                    .tag(tagName)
                    .schoolName(b1.getUserIdx().getSchoolIdx().getName()) // todo : 초기 앱 런칭에만 삽입
                    .didLike(didLike)
                    .build();

            getBoards.add(boardWithTag);
        }
    }

    public String likeBoard(Principal principal, Long boardIdx) throws BaseException {

        Optional<Board> optionalBoard = boardRepository.findByBoardIdx(boardIdx);

        if (optionalBoard.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARD_LIST);
        }
        Board board = optionalBoard.get();

        //user : 하트 누르는 사용자.
        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        //boardUser : 게시글 작성자.
        Optional<User> optionalBoardUser = boardUserRepository.findUserIdxByBoardIdxAndStatus(boardIdx, "WRITE");

        if (optionalBoardUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User boardUser = optionalBoardUser.get();

        Optional<BoardUser> checkAlreadyLike = boardUserRepository.findByUserIdxAndBoardIdxAndStatus(user, board, "LIKE");
        if (checkAlreadyLike.isPresent()) {
            boardUserRepository.delete(checkAlreadyLike.get());
            return "좋아요를 취소하였습니다.";
        }

        BoardUser likeEntity = BoardUser.builder()
                .userIdx(user)
                .boardIdx(board)
                .status("LIKE").build();

        Notification notificationEntity = Notification.builder()
                .userIdx(boardUser)
                .title(user.getNickname())
                .body("회원님의 게시물에 누군가 하트를 눌렀습니다.")
                .build();

        try {
            boardUserRepository.save(likeEntity);
            notificationRepository.save(notificationEntity);
        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }

//Todo : User FcmToken 추가 후 주석 해제.
//      하트 알림 전송 부분.
//
        Optional<FcmToken> optionalFcmToken = fcmTokenRepository.findFcmTokenByUser(boardUser);
        if (optionalFcmToken.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_FCMTOKEN);
        }
        FcmToken fcmToken = optionalFcmToken.get();
        if (!Objects.equals(user.getUserIdx(), boardUser.getUserIdx())) {
            try {
                firebaseCloudMessageService.sendMessageTo(fcmToken.getFcmToken(), boardUser.getNickname(), "회원님의 핸즈업에 누군가 하트를 눌렀습니다.");
            } catch (Exception e) {
                e.printStackTrace();
                log.info("ERROR MESSAGE : {}", e.getMessage());
                throw new BaseException(BaseResponseStatus.PUSH_NOTIFICATION_SEND_ERROR);
            }
        }
        return "해당 게시글에 좋아요를 눌렀습니다.";

    }
    //내 게시물 조회 페이징
    public BoardDto.MyBoard viewMyBoard(Principal principal, Pageable pageable) throws BaseException{

        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if (optionalUser.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optionalUser.get();

        Character character = user.getCharacter();


        List<BoardPreviewRes> myBoardList = boardUserRepository.findBoardIdxByUserIdxAndStatusInOrderByBoardUserIdxDesc(user, "WRITE", "ACTIVE", pageable)
                .stream()
                .filter(board -> board.getStatus().equals("ACTIVE"))
                .map(Board::toPreviewRes)
                .collect(Collectors.toList());

        return new BoardDto.MyBoard(character, myBoardList);
    }




    public void addBoard(Principal principal, BoardDto.GetBoardInfo boardInfo) throws BaseException {

        checkLocationError(boardInfo);

        Board boardEntity = Board.builder()
                .content(boardInfo.getContent())
                .indicateLocation(boardInfo.getIndicateLocation())
                .latitude(boardInfo.getLatitude())
                .longitude(boardInfo.getLongitude())
                .location(boardInfo.getLocation())
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


    //게시물 삭제
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

        //게시물이 존재하는지 체크
        if(boardUser.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARD_LIST);
        }

        //해당 게시물을 로그인한 유저가 작성했는지 체크
        if(!Objects.equals(boardUser.get().getUserIdx().getUserIdx(), userEntity.getUserIdx())){
            throw new BaseException(BaseResponseStatus.NON_EXIST_BOARDUSERIDX);
        }

        //이미 삭제된 게시물인지 체크
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

        boardEntity.changeBoard(boardInfo.getContent(), boardInfo.getLatitude(), boardInfo.getLongitude(), boardInfo.getLocation(), boardInfo.getIndicateLocation(), boardInfo.getMessageDuration());
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
        String successResult = "게시물을 차단하였습니다.";

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


        //BoardUser 객체가 없을 때
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
        } else if (Objects.equals(optionalBoardUser.get().getStatus(), "LIKE")) { //하트를 누른 게시물일 때
            boardUser = optionalBoardUser.get();
            boardUser.changeStatus("BLOCK");

            try {
                boardUserRepository.save(boardUser);
                return successResult;
            } catch (Exception e) {
                throw new BaseException(DATABASE_INSERT_ERROR);
            }
        } else if (Objects.equals(optionalBoardUser.get().getStatus(), "WRITE")) { //본인이 작성자일 때
            throw new BaseException(BaseResponseStatus.SELF_BLOCK_ERROR);
        } else if (Objects.equals(optionalBoardUser.get().getStatus(), "BLOCK")) { //이미 차단한 게시물일 때
            throw new BaseException(BaseResponseStatus.BLOCKED_BOARD_ERROR);
        } else throw new BaseException(DATABASE_INSERT_ERROR); //알 수 없는 이유

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

    //받은 하트 목록 조회
    public BoardDto.TotalReceivedLikeRes fetchReceivedLikePagesBy (Principal principal, int size, Long lastIdx) throws BaseException {
        Optional<User> optional = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_USERIDX);
        }
        User user = optional.get();

        List<Board> boardList = this.boardUserRepository.findBoardIdxByUserIdxAndStatus(user, "WRITE");

        //페이징 처리
        Page<BoardUser> boardUsers = fetchReceivedLikes(size, lastIdx, boardList);
        if(boardUsers.getContent().isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_LIKE_BOARDS);
        }
        //dto 생성
        List<BoardDto.ReceivedLikeInfo> receivedLikeInfoList = new ArrayList<>();
        for(BoardUser boardUser: boardUsers.getContent()){
            Character character = boardUser.getUserIdx().getCharacter();
            CharacterDto.GetCharacterInfo characterInfo = new CharacterDto.GetCharacterInfo(character.getEye(),
                    character.getEyeBrow(), character.getGlasses(), character.getNose(), character.getMouth(),
                    character.getHair(), character.getHairColor(), character.getSkinColor(), character.getBackGroundColor());

            BoardDto.ReceivedLikeInfo receivedLikeInfo = BoardDto.ReceivedLikeInfo.builder()
                    .emailFrom(boardUser.getUserIdx().getEmail())
                    .text("아래 글에 "+boardUser.getUserIdx().getNickname()+"님이 관심있어요")
                    .boardContent(boardUser.getBoardIdx().getContent())
                    .LikeCreatedAt(boardUser.getCreatedAt())
                    .character(characterInfo)
                    .boardIdx(boardUser.getBoardIdx().getBoardIdx())
                    .boardUserIdx(boardUser.getBoardUserIdx())
                    .build();
            receivedLikeInfoList.add(receivedLikeInfo);
        }

        receivedLikeInfoList.sort(Collections.reverseOrder());

        BoardDto.TotalReceivedLikeRes receivedLikeRes = BoardDto.TotalReceivedLikeRes.builder()
                .receivedLikeInfo(receivedLikeInfoList)
                .hasNext(boardUsers.hasNext())
                .build();
        return receivedLikeRes;
    }
    //받은 좋아요(하트) 목록 페이징으로 가져오기
    private Page<BoardUser> fetchReceivedLikes(int size, Long lastIdx, List<Board> boardList){
        PageRequest pageRequest = PageRequest.of(0, size); //페이지는 0으로 고정, 페이지에 표시되는 개수는 size로 결정
        return lastIdx == null? boardUserRepository.findAllByStatusAndBoardIdxInOrderByBoardUserIdxDesc("LIKE", boardList, pageRequest):
                boardUserRepository.findByBoardUserIdxLessThanAndStatusAndBoardIdxInOrderByBoardUserIdxDesc(lastIdx, "LIKE", boardList, pageRequest);
    }

    public void testFcmToken(Principal principal, String fcmToken) throws BaseException {
        User user = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE")
                .orElseThrow(() -> new BaseException(NON_EXIST_USERIDX));
        Notification notificationEntity = Notification.builder()
                .userIdx(user)
                .title("TEST")
                .body("NOTIFICATION TEST")
                .build();
        try {
            notificationRepository.save(notificationEntity);
            firebaseCloudMessageService.sendMessageTo(fcmToken, "TEST", "NOTIFICATION TEST");
        } catch (Exception e) {
            e.printStackTrace();
            throw new BaseException(PUSH_NOTIFICATION_SEND_ERROR);
        }
    }
}
