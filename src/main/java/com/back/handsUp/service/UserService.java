package com.back.handsUp.service;

import static com.back.handsUp.baseResponse.BaseResponseStatus.*;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponseStatus;
import com.back.handsUp.domain.Notification;
import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.jwt.RefreshToken;
import com.back.handsUp.domain.user.Character;
import com.back.handsUp.domain.user.School;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.dto.NotificationResponse;
import com.back.handsUp.dto.fcmToken.FcmTokenDto;
import com.back.handsUp.dto.jwt.TokenDto;
import com.back.handsUp.dto.user.CharacterDto;
import com.back.handsUp.dto.user.UserCharacterDto;
import com.back.handsUp.dto.user.UserDto;
import com.back.handsUp.repository.NotificationRepository;
import com.back.handsUp.repository.board.BoardUserRepository;
import com.back.handsUp.repository.fcm.FcmTokenRepository;
import com.back.handsUp.repository.user.CharacterRepository;
import com.back.handsUp.repository.user.SchoolRepository;
import com.back.handsUp.repository.user.UserRepository;
import com.back.handsUp.repository.user.jwt.RefreshTokenRepository;
import com.back.handsUp.utils.FirebaseCloudMessageService;
import com.back.handsUp.utils.Role;
import com.back.handsUp.utils.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final SchoolRepository schoolRepository;
    private final BoardUserRepository boardUserRepository;

    private final BoardService boardService;
    private final FcmTokenRepository fcmTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final FirebaseCloudMessageService firebaseCloudMessageService;

    private final NotificationRepository notificationRepository;

    public void signupUser(UserDto.ReqSignUp user) throws BaseException {
        Optional<User> emailCheck = userRepository.findByEmailAndStatus(user.getEmail(), "ACTIVE");
        if (emailCheck.isPresent()) {
            throw new BaseException(EXIST_USER);
        }

        String password = user.getPassword();
        try{
            String encodedPwd = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPwd);
        } catch (Exception e){
            throw new BaseException(BaseResponseStatus.PASSWORD_ENCRYPTION_ERROR);
        }

        Optional<Character> optional1= this.characterRepository.findByCharacterIdx(user.getCharacterIdx());
        if(optional1.isEmpty()){
            throw new BaseException(NON_EXIST_CHARACTERIDX);
        }
        Character character = optional1.get();

        Optional<School> optional2= this.schoolRepository.findByName(user.getSchoolName());
        School school;

        if(optional2.isEmpty()){
            school=School.builder()
                    .name(user.getSchoolName())
                    .build();
            this.schoolRepository.save(school);
        } else{
            school = optional2.get();
        }

        User userEntity = User.builder()
                .email(user.getEmail())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .character(character)
                .schoolIdx(school)
                .role(Role.ROLE_USER)
                .nicknameUpdatedAt(LocalDate.of(0000,1,1))
                .build();
        user.setPassword(password);

        this.userRepository.save(userEntity);
    }

    public TokenDto logIn(UserDto.ReqLogIn user) throws BaseException{
        Optional<User> optional = this.userRepository.findByEmailAndStatus(user.getEmail(), "ACTIVE");
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }else{
            User userEntity = optional.get();
            if(passwordEncoder.matches(user.getPassword(), userEntity.getPassword())) { // 그냥 받아온 password를 넣으면 알아서 암호화해서 비교함.
                // todo : front firebase 구현 시 주석 해제
                firebaseCloudMessageService.overWriteToken(user.getFcmToken(), userEntity);  //FCM token 저장.
                return token(user);
            }else{
                throw new BaseException(BaseResponseStatus.INVALID_PASSWORD);
            }
        }
    }

    public void logOut(Principal principal) throws BaseException {
        Optional<User> optional = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_USERIDX);
        }
        User userEntity = optional.get();

        Optional<RefreshToken> optional1 = this.refreshTokenRepository.findByKeyId(userEntity.getEmail());

        RefreshToken token = optional1.get();

        token.setValue("");
        // todo : front firebase 구현 시 주석 해제
        firebaseCloudMessageService.deleteToken(userEntity);
    }

    //캐릭터 수정
    public void updateChatacter(Principal principal, CharacterDto.GetCharacterInfo characterInfo) throws BaseException {

        Optional<User> optionalUser = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optionalUser.isEmpty()){
            throw new BaseException(NON_EXIST_USERIDX);
        }


        try{
            Character findCharacter = optionalUser.get().getCharacter();

            findCharacter.setEye(characterInfo.getEye());
            findCharacter.setBackGroundColor(characterInfo.getBackGroundColor());
            findCharacter.setGlasses(characterInfo.getGlasses());
            findCharacter.setHair(characterInfo.getHair());
            findCharacter.setEyeBrow(characterInfo.getEyeBrow());
            findCharacter.setHairColor(characterInfo.getHairColor());
            findCharacter.setMouth(characterInfo.getMouth());
            findCharacter.setNose(characterInfo.getNose());
            findCharacter.setSkinColor(characterInfo.getSkinColor());

        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }
    }

    //닉네임 수정
    public void updateNickname(Principal principal, String nickname) throws BaseException {
        Optional<User> optional = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()){
            throw new BaseException(NON_EXIST_USERIDX);
        }

        User findUser = optional.get();
        LocalDate lastUpdate = findUser.getNicknameUpdatedAt();
        long days = ChronoUnit.DAYS.between(lastUpdate, LocalDate.now());

        if (days < 7) {
            throw new BaseException(LIMIT_NICKNAME_CHANGE);
        }
        //닉네임 중복 확인
        List<User> findUsers = this.userRepository.findByNicknameAndSchoolIdx(nickname,
            findUser.getSchoolIdx(), "ACTIVE");
        if (!findUsers.isEmpty()) {
            throw new BaseException(EXIST_NICKNAME);
        }

        try {
            findUser.setNickname(nickname);
            findUser.setNicknameUpdatedAt(LocalDate.now());
        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }
    }



    public TokenDto token(UserDto.ReqLogIn user){
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());

        // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        //    authenticate 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
        Authentication authentication = this.authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenDto tokenDto = this.tokenProvider.generateTokenDto(authentication);

        // 4. RefreshToken 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .key(authentication.getName())
                .value(tokenDto.getRefreshToken())
                .build();
        this.refreshTokenRepository.save(refreshToken);

        // 5. 토큰 발급
        return tokenDto;
    }

    public TokenDto reissue(TokenDto tokenRequestDto, HttpServletRequest request) throws BaseException { //재발급
        // 1. Refresh Token 검증
        if (!this.tokenProvider.validateToken(tokenRequestDto.getRefreshToken(), request)) {
            throw new BaseException(BaseResponseStatus.REFRESH_TOKEN_ERROR);
        }

        // 2. Access Token 에서 Member ID 가져오기
        Authentication authentication = this.tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. 저장소에서 Member ID 를 기반으로 Refresh Token 값 가져옴
        RefreshToken refreshToken = this.refreshTokenRepository.findByKeyId(authentication.getName())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.LOGOUT_USER));

        // 4. Refresh Token 일치하는지 검사
        if (!refreshToken.getValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new BaseException(BaseResponseStatus.NOT_MATCH_TOKEN);
        }

        // 5. 새로운 토큰 생성
        TokenDto tokenDto = this.tokenProvider.generateTokenDto(authentication);

        // 6. 저장소 정보 업데이트
        RefreshToken newRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        this.refreshTokenRepository.save(newRefreshToken);

        // 토큰 발급
        return tokenDto;
    }

    //캐릭터 생성
    public Character createCharacter(CharacterDto.GetCharacterInfo characterInfo) throws BaseException{

        //not null 값이 null로 들어온 경우
        if(characterInfo.getEye().isBlank() || characterInfo.getEyeBrow().isBlank() || characterInfo.getHair().isBlank() ||
        characterInfo.getNose().isBlank() || characterInfo.getMouth().isBlank()|| characterInfo.getBackGroundColor().isBlank()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_CHARACTER_VALUE);
        }

        Character characterEntity = Character.builder()
                .eye(characterInfo.getEye())
                .eyeBrow(characterInfo.getEyeBrow())
                .glasses(characterInfo.getGlasses())
                .nose(characterInfo.getNose())
                .mouth(characterInfo.getMouth())
                .hair(characterInfo.getHair())
                .hairColor(characterInfo.getHairColor())
                .skinColor(characterInfo.getSkinColor())
                .backGroundColor(characterInfo.getBackGroundColor())
                .build();

        try{
            this.characterRepository.save(characterEntity);
            return characterEntity;

        } catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }

    }


    //비밀번호 변경
    public void patchPwd(Principal principal, UserDto.ReqPwd userPwd) throws BaseException {
        if (userPwd.getCurrentPwd().isEmpty() || userPwd.getNewPwd().isEmpty()) {
            throw new BaseException(BaseResponseStatus.INVALID_REQUEST);
        }

        if (userPwd.getCurrentPwd().equals(userPwd.getNewPwd())) {
            throw new BaseException(BaseResponseStatus.SAME_PASSWORD);
        }

        Optional<User> optional = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if (optional.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }
        User user = optional.get();

        if (passwordEncoder.matches(userPwd.getCurrentPwd(), user.getPassword())) {
            String encodedPwd = passwordEncoder.encode(userPwd.getNewPwd());
            user.changePWd(encodedPwd);
        } else {
            throw new BaseException(BaseResponseStatus.INVALID_PASSWORD);
        }
    }


    //회원 탈퇴 (patch)
    public UserDto.ReqWithdraw withdrawUser(Principal principal)  throws BaseException{

        Optional<User> optional = this.userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");

        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_USERIDX);
        }

        User userEntity1 = optional.get();

        if(userEntity1.getStatus().equals("DELETE")){
            throw new BaseException(BaseResponseStatus.ALREADY_DELETE_USER);
        }else{
            userEntity1.changeStatus("DELETE");
        }

        try{
            this.userRepository.save(userEntity1);
        }catch (Exception e) {
            throw new BaseException(DATABASE_INSERT_ERROR);
        }

        //게시물 상태 변경
        List<Board> myBoards = this.boardUserRepository.findBoardIdxByUserIdxAndStatus(userEntity1, "WRITE");
        for(Board board: myBoards){
            board.changeStatus("DELETE");
        }

        UserDto.ReqWithdraw response = UserDto.ReqWithdraw.builder()
                .userIdx(userEntity1.getUserIdx())
                .build();


        return response;

    }

    //닉네임, 캐릭터 정보 조회
    public UserCharacterDto getUserInfo(Principal principal) throws BaseException {
        Optional<User> optional = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_USERIDX);
        }
        User user = optional.get();
        Character character = user.getCharacter();


        UserCharacterDto userCharacterDto = UserCharacterDto.builder()
            .nickname(user.getNickname())
            .schoolName(user.getSchoolIdx().getName())
            .eye(character.getEye())
            .eyeBrow(character.getEyeBrow())
            .glasses(character.getGlasses())
            .nose(character.getNose())
            .mouth(character.getMouth())
            .hair(character.getHair())
            .hairColor(character.getHairColor())
            .skinColor(character.getSkinColor())
            .backGroundColor(character.getBackGroundColor())
            .build();

        return userCharacterDto;
    }

    //닉네임 중복 확인
    public void checkNickname(UserDto.ReqCheckNickname reqCheckNickname) throws BaseException {
        Optional<School> optionalSchool = this.schoolRepository.findByName(reqCheckNickname.getSchoolName());
        Optional<User> optional = Optional.empty();
        List<User> findUsers = new ArrayList<>();
        if(!optionalSchool.isEmpty()){
            School school = optionalSchool.get();
            findUsers = this.userRepository.findByNicknameAndSchoolIdx(
                reqCheckNickname.getNickname(), school, "ACTIVE");
        } else {
            optional = this.userRepository.findByNickname(reqCheckNickname.getNickname());
        }

        if(!findUsers.isEmpty()||!optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.EXIST_NICKNAME);
        }
    }

    public void updateFcmToken(Principal principal, FcmTokenDto.updateToken fcmToken) throws BaseException {
        Optional<User> optional = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_USERIDX);
        }
        User user = optional.get();

        firebaseCloudMessageService.overWriteToken(fcmToken.getFcmToken(), user);
    }

    public void deleteFcmToken(Principal principal) throws BaseException {
        Optional<User> optional = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE");
        if(optional.isEmpty()) {
            throw new BaseException(BaseResponseStatus.NON_EXIST_USERIDX);
        }
        User user = optional.get();

        firebaseCloudMessageService.deleteToken(user);
    }

    public List<NotificationResponse> notificationList(Principal principal, Pageable pageable) throws BaseException {
        User user = userRepository.findByEmailAndStatus(principal.getName(), "ACTIVE")
                .orElseThrow(() -> new BaseException(NON_EXIST_USERIDX));

        log.info("ajfkjal");
        List<Notification> byUserIdx = notificationRepository.findByUserIdx(user, pageable);
        log.info("notification : {}", byUserIdx);
        List<NotificationResponse> responseList = byUserIdx.stream().map(NotificationResponse::entityToDto).collect(Collectors.toList());
        return responseList;
    }
}
