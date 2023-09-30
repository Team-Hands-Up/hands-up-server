package com.back.handsUp.controller;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponse;
import com.back.handsUp.baseResponse.BaseResponseStatus;
import com.back.handsUp.domain.Notification;
import com.back.handsUp.domain.user.Character;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.dto.NotificationResponse;
import com.back.handsUp.dto.fcmToken.FcmTokenDto;
import com.back.handsUp.dto.jwt.TokenDto;
import com.back.handsUp.dto.user.CharacterDto;
import com.back.handsUp.dto.user.UserCharacterDto;
import com.back.handsUp.dto.user.UserDto;
import com.back.handsUp.service.MailService;
import com.back.handsUp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/users")
public class UserController {
    private final UserService userService;
    private final MailService mailService;

    @ResponseBody
    @PostMapping("/signup")
    public BaseResponse<String> signUp(@Valid @RequestBody UserDto.ReqSignUp user, BindingResult result){
        if (result.hasErrors()){
            String message = result.getFieldError().getDefaultMessage();
            return new BaseResponse<>(false, 4003, message);
        }

        try {
            this.userService.signupUser(user);
            return new BaseResponse<>("회원가입이 완료되었습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @ResponseBody
    @PostMapping("/login")
    public BaseResponse<TokenDto> logIn(@Valid @RequestBody UserDto.ReqLogIn user, BindingResult result){
        if (result.hasErrors()){
            String message = result.getFieldError().getDefaultMessage();
            return new BaseResponse<>(false, 4003, message);
        }

        try {
            TokenDto token = this.userService.logIn(user);
            return new BaseResponse<>(token);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @ResponseBody
    @PostMapping("/logout")
    public BaseResponse<String> logOut(Principal principal){
        try{
            this.userService.logOut(principal);
            return new BaseResponse<>("로그아웃이 완료되었습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }



    @ResponseBody
    @PostMapping("create/character")
    public BaseResponse<Long> createCharacter(@RequestBody CharacterDto.GetCharacterInfo characterInfo){

        try{
            Character character = this.userService.createCharacter(characterInfo);
            return new BaseResponse<>(character.getCharacterIdx());
        }catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }

    }


    @ResponseBody
    @PatchMapping("/password")
    public BaseResponse<String> changePwd(Principal principal, @RequestBody UserDto.ReqPwd userPwd) {
        try {
            this.userService.patchPwd(principal, userPwd);
            return new BaseResponse<>("비밀번호 변경이 완료되었습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());

        }
    }

    //회원 탈퇴
    @ResponseBody
    @PatchMapping("/withdraw")
    public BaseResponse<UserDto.ReqWithdraw> withdrawUser(Principal principal){
        try{
            UserDto.ReqWithdraw userIdx = this.userService.withdrawUser(principal);
            return new BaseResponse<>(userIdx);
        }catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    //액세스 토큰 재발급
    @ResponseBody
    @PostMapping("/reissue")
    public BaseResponse<TokenDto> reissue(@RequestBody TokenDto token, HttpServletRequest request) {
        try {
            TokenDto newToken = this.userService.reissue(token, request);
            return new BaseResponse<>(newToken);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    //학교 인증
    @ResponseBody
    @PostMapping("/certify")
    public BaseResponse<String> certify(@RequestBody UserDto.ResEmail resEmail){
        try {
            String code = this.mailService.sendMail(resEmail.getEmail());
            return new BaseResponse<>(code);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @PatchMapping("/nickname")
    public BaseResponse<String> updateUsername(Principal principal, @RequestBody UserDto.ReqNickname nickname){

        try{
            userService.updateNickname(principal, nickname.getNickname());
            return new BaseResponse<>("닉네임을 수정하였습니다.");
        } catch (BaseException e){
            return new BaseResponse<>(e.getStatus());
        }
    }

    @PatchMapping("/character")
    public BaseResponse<String> updateCharacter(Principal principal, @RequestBody CharacterDto.GetCharacterInfo characterInfo){
        try{
            userService.updateChatacter(principal, characterInfo);
            return new BaseResponse<>("캐릭터를 수정하였습니다.");
        } catch (BaseException e){
            return new BaseResponse<>(e.getStatus());
        }
    }

    @GetMapping("")
    public BaseResponse<UserCharacterDto> getUserInfo(Principal principal){
        try {
            UserCharacterDto userCharacterDto = userService.getUserInfo(principal);
            return new BaseResponse<>(userCharacterDto);

        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @ResponseBody
    @PostMapping("/nickname")
    public BaseResponse<String> checkNickname(@RequestBody UserDto.ReqCheckNickname checkNickname){
        try {
            this.userService.checkNickname(checkNickname);
            return new BaseResponse<>("사용할 수 있는 닉네임입니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @ResponseBody
    @PostMapping("/update-fcmToken")
    public BaseResponse<String> updateFcmToken(@RequestBody FcmTokenDto.updateToken fcmToken, Principal principal) {
        try {
            this.userService.updateFcmToken(principal, fcmToken);
            return new BaseResponse<>("FCM 토큰 비교에 성공하였습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @PostMapping("/delete-fcmToken")
    public BaseResponse<String> deleteFcmToken(Principal principal) {
        try {
            this.userService.deleteFcmToken(principal);
            return new BaseResponse<>("FCM 토큰을 삭제하였습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @GetMapping("/notification")
    public BaseResponse<List<NotificationResponse>> notificationList(Principal principal,
                                                             @PageableDefault(size = 20,
                                                                     sort = "createdAt",
                                                                     direction = Sort.Direction.DESC)
                                                             Pageable pageable) {
        try {
            List<NotificationResponse> notifications = userService.notificationList(principal, pageable);
            return new BaseResponse<>(notifications);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

    @ResponseBody
    @PostMapping("/initPw")
    public BaseResponse<String> initPw(@RequestBody UserDto.ResEmail resEmail){
        try {
            this.mailService.initPw(resEmail.getEmail());
            return new BaseResponse<>("임시 비밀번호 생성에 성공했습니다.");
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }

}
