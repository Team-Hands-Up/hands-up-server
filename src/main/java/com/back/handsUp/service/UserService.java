package com.back.handsUp.service;

import static com.back.handsUp.baseResponse.BaseResponseStatus.*;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponseStatus;
import com.back.handsUp.domain.board.Board;
import com.back.handsUp.domain.jwt.RefreshToken;
import com.back.handsUp.domain.user.Character;
import com.back.handsUp.domain.user.School;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.dto.fcmToken.FcmTokenDto;
import com.back.handsUp.dto.jwt.TokenDto;
import com.back.handsUp.dto.user.CharacterDto;
import com.back.handsUp.dto.user.UserCharacterDto;
import com.back.handsUp.dto.user.UserDto;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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


    public void signupUser(UserDto.ReqSignUp user) throws BaseException {
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
            if(passwordEncoder.matches(user.getPassword(), userEntity.getPassword())) { // ?????? ????????? password??? ????????? ????????? ??????????????? ?????????.
                // todo : front firebase ?????? ??? ?????? ??????
                firebaseCloudMessageService.overWriteToken(user.getFcmToken(), userEntity);  //FCM token ??????.
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
        // todo : front firebase ?????? ??? ?????? ??????
        firebaseCloudMessageService.deleteToken(userEntity);
    }

    //????????? ??????
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

    //????????? ??????
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

        //????????? ?????? ??????
        optional = this.userRepository.findByNicknameAndSchoolIdx(nickname, findUser.getSchoolIdx());
        if (!optional.isEmpty()) {
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

        // 2. ????????? ?????? (????????? ???????????? ??????) ??? ??????????????? ??????
        //    authenticate ???????????? ????????? ??? ??? CustomUserDetailsService ?????? ???????????? loadUserByUsername ???????????? ?????????
        Authentication authentication = this.authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. ?????? ????????? ???????????? JWT ?????? ??????
        TokenDto tokenDto = this.tokenProvider.generateTokenDto(authentication);

        // 4. RefreshToken ??????
        RefreshToken refreshToken = RefreshToken.builder()
                .key(authentication.getName())
                .value(tokenDto.getRefreshToken())
                .build();
        this.refreshTokenRepository.save(refreshToken);

        // 5. ?????? ??????
        return tokenDto;
    }

    public TokenDto reissue(TokenDto tokenRequestDto, HttpServletRequest request) throws BaseException { //?????????
        // 1. Refresh Token ??????
        if (!this.tokenProvider.validateToken(tokenRequestDto.getRefreshToken(), request)) {
            throw new BaseException(BaseResponseStatus.REFRESH_TOKEN_ERROR);
        }

        // 2. Access Token ?????? Member ID ????????????
        Authentication authentication = this.tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. ??????????????? Member ID ??? ???????????? Refresh Token ??? ?????????
        RefreshToken refreshToken = this.refreshTokenRepository.findByKeyId(authentication.getName())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.LOGOUT_USER));

        // 4. Refresh Token ??????????????? ??????
        if (!refreshToken.getValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new BaseException(BaseResponseStatus.NOT_MATCH_TOKEN);
        }

        // 5. ????????? ?????? ??????
        TokenDto tokenDto = this.tokenProvider.generateTokenDto(authentication);

        // 6. ????????? ?????? ????????????
        RefreshToken newRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        this.refreshTokenRepository.save(newRefreshToken);

        // ?????? ??????
        return tokenDto;
    }

    //????????? ??????
    public Character createCharacter(CharacterDto.GetCharacterInfo characterInfo) throws BaseException{

        //not null ?????? null??? ????????? ??????
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


    //???????????? ??????
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


    //?????? ?????? (patch)
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

        //????????? ?????? ??????
        List<Board> myBoards = this.boardUserRepository.findBoardIdxByUserIdxAndStatus(userEntity1, "WRITE");
        for(Board board: myBoards){
            board.changeStatus("DELETE");
        }

        UserDto.ReqWithdraw response = UserDto.ReqWithdraw.builder()
                .userIdx(userEntity1.getUserIdx())
                .build();


        return response;

    }

    //?????????, ????????? ?????? ??????
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

    //????????? ?????? ??????
    public void checkNickname(UserDto.ReqCheckNickname reqCheckNickname) throws BaseException {
        Optional<School> optionalSchool = this.schoolRepository.findByName(reqCheckNickname.getSchoolName());
        Optional<User> optional;

        if(!optionalSchool.isEmpty()){
            School school = optionalSchool.get();
            optional = this.userRepository.findByNicknameAndSchoolIdx(reqCheckNickname.getNickname(), school);
        } else {
            optional = this.userRepository.findByNickname(reqCheckNickname.getNickname());
        }

        if(!optional.isEmpty()){
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
}
