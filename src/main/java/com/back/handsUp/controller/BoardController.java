package com.back.handsUp.controller;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponse;
import com.back.handsUp.dto.board.BoardDto;
import com.back.handsUp.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/boards")
@RestController
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/test")
    public ResponseEntity<String> test(Principal principal) {
        String str = principal.getName();
        return ResponseEntity.ok(str);
    }


    @GetMapping("/singleList/{boardIdx}")
    public BaseResponse<BoardDto.SingleBoardRes> BoardViewByIdx(Principal principal, @PathVariable("boardIdx") Long boardIdx) throws BaseException {
        BoardDto.SingleBoardRes board = boardService.boardViewByIdx(principal, boardIdx);
        return new BaseResponse<>(board);
    }

    //전체 게시물 조회
    @ResponseBody
    @RequestMapping("/showList")
    public BaseResponse<BoardDto.GetBoardList> showBoardList(Principal principal){
        try {
            BoardDto.GetBoardList getBoards = boardService.showBoardList(principal);
            return new BaseResponse<>(getBoards);
        }catch (BaseException exception){
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    //전체 게시물(지도 상) 조회
    @ResponseBody
    @RequestMapping("/showMapList")
    public BaseResponse<BoardDto.GetBoardMapAndSchool> showBoardMapList(Principal principal){
        try {
            BoardDto.GetBoardMapAndSchool getBoardsMap = boardService.showBoardMapList(principal);
            return new BaseResponse<>(getBoardsMap);
        }catch (BaseException exception){
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    @PostMapping("/{boardIdx}/like")
    public BaseResponse<String> like(Principal principal,@PathVariable Long boardIdx) {
        try{
            String str = this.boardService.likeBoard(principal, boardIdx);

            //좋아요 알림 전송 실패 케이스 분리
            if(str.equals("해당 게시글에 좋아요를 눌렀지만 알림 전송에는 실패했습니다.")){
                return new BaseResponse(false, 5003, str);
            }

            return new BaseResponse<>(str);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @GetMapping("/myBoards")
    public BaseResponse<BoardDto.MyBoard> viewMyBoard(
            Principal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable){
        try {
            BoardDto.MyBoard myBoards = boardService.viewMyBoard(principal, pageable);
            return new BaseResponse<>(myBoards);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @ResponseBody
    @PostMapping("/")
    public BaseResponse<String> addBoard(Principal principal, @RequestBody BoardDto.GetBoardInfo boardInfo){
        try{
            this.boardService.addBoard(principal, boardInfo);
            return new BaseResponse<>("게시글을 등록하였습니다.");
        } catch (BaseException e){
            return new BaseResponse<>(e.getStatus());
        }
    }

    @ResponseBody
    @PostMapping("/delete/{boardIdx}")
    public BaseResponse<String> deleteBoard(Principal principal, @PathVariable Long boardIdx){
        try{
            this.boardService.deleteBoard(principal, boardIdx);
            return new BaseResponse<>("게시글을 삭제하였습니다.");
        } catch (BaseException e){
            return new BaseResponse<>(e.getStatus());
        }
    }

    @ResponseBody
    @PatchMapping("/{boardIdx}")
    public BaseResponse<String> patchBoard(Principal principal, @PathVariable Long boardIdx, @RequestBody BoardDto.GetBoardInfo boardInfo){
        try{
            this.boardService.patchBoard(principal, boardIdx, boardInfo);
            return new BaseResponse<>("게시글을 수정하였습니다.");
        } catch (BaseException e){
            return new BaseResponse<>(e.getStatus());
        }
    }

    @PostMapping("/block/{boardIdx}")
    public BaseResponse<String> blockBoard(Principal principal, @PathVariable Long boardIdx) {
        try {
            String result = this.boardService.blockBoard(principal, boardIdx);
            return new BaseResponse<>(result);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    //내가 받은 좋아요(하트) 목록 조회
    @ResponseBody
    @GetMapping({"/like/{size}/{lastIdx}", "/like/{size}/"})
    public BaseResponse<BoardDto.TotalReceivedLikeRes> getReceivedLikeList(Principal principal, @PathVariable int size, @PathVariable(required = false) Long lastIdx){
        try{
            BoardDto.TotalReceivedLikeRes receivedLikeRes = this.boardService.fetchReceivedLikePagesBy(principal, size, lastIdx);
            return new BaseResponse<>(receivedLikeRes);
        } catch (BaseException e){
            return new BaseResponse<>(e.getStatus());
        }
    }

    @PostMapping("/test/fcm-token")
    public BaseResponse<String> testToSendFcmToken(Principal principal, @RequestParam String fcmToken) {
        try {
            boardService.testFcmToken(principal, fcmToken);
            return new BaseResponse<>("푸쉬 알림을 보냈습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }
}
