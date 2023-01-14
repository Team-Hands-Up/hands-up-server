package com.back.handsUp.service;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponseStatus;
import com.back.handsUp.domain.jwt.RefreshToken;
import com.back.handsUp.domain.user.Character;
import com.back.handsUp.domain.user.School;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.dto.jwt.TokenDto;
import com.back.handsUp.dto.user.UserDto;
import com.back.handsUp.repository.user.CharacterRepository;
import com.back.handsUp.repository.user.SchoolRepository;
import com.back.handsUp.repository.user.UserRepository;
import com.back.handsUp.repository.user.jwt.RefreshTokenRepository;
import com.back.handsUp.utils.Role;
import com.back.handsUp.utils.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final SchoolRepository schoolRepository;

    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;


    public void signupUser(UserDto.ReqSignUp user) throws BaseException {
        //이메일 중복 확인
        Optional<User> optional = this.userRepository.findByEmail(user.getEmail());
        if(!optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.EXIST_USER);
        }

        //닉네임 중복 확인
        optional = this.userRepository.findByNickname(user.getNickname());
        if(!optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.EXIST_USER);
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
            throw new BaseException(BaseResponseStatus.NON_EXIST_CHARACTERIDX);
        }
        Character character = optional1.get();

        Optional<School> optional2= this.schoolRepository.findBySchoolIdx(user.getSchoolIdx());
        if(optional2.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_SCHOOLIDX);
        }
        School school = optional2.get();

        User userEntity = User.builder()
                .email(user.getEmail())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .characterIdx(character)
                .schoolIdx(school)
                .role(Role.ROLE_USER)
                .build();
        user.setPassword(password);

        this.userRepository.save(userEntity);
    }

    public TokenDto logIn(UserDto.ReqLogIn user) throws BaseException{
        //이메일 형식 확인

        Optional<User> optional = this.userRepository.findByEmail(user.getEmail());
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }else{
            User userEntity = optional.get();
            if(passwordEncoder.matches(user.getPassword(), userEntity.getPassword())) { // 그냥 받아온 password를 넣으면 알아서 암호화해서 비교함.
                return token(user);
            }else{
                throw new BaseException(BaseResponseStatus.INVALID_PASSWORD);
            }

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

    public TokenDto reissue(TokenDto tokenRequestDto) { //재발급
        // 1. Refresh Token 검증
        if (!this.tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("Refresh Token 이 유효하지 않습니다.");
        }

        // 2. Access Token 에서 Member ID 가져오기
        Authentication authentication = this.tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. 저장소에서 Member ID 를 기반으로 Refresh Token 값 가져옴
        RefreshToken refreshToken = this.refreshTokenRepository.findByKeyId(authentication.getName())
                .orElseThrow(() -> new RuntimeException("로그아웃 된 사용자입니다."));

        // 4. Refresh Token 일치하는지 검사
        if (!refreshToken.getValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
        }

        // 5. 새로운 토큰 생성
        TokenDto tokenDto = this.tokenProvider.generateTokenDto(authentication);

        // 6. 저장소 정보 업데이트
        RefreshToken newRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        this.refreshTokenRepository.save(newRefreshToken);

        // 토큰 발급
        return tokenDto;
    }

}