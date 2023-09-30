package com.back.handsUp.service;

import com.back.handsUp.baseResponse.BaseException;
import com.back.handsUp.baseResponse.BaseResponseStatus;
import com.back.handsUp.domain.user.User;
import com.back.handsUp.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.*;

@PropertySource("classpath:application.properties")
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class MailService {
    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${spring.mail.username}")
    private String email;

    private static final char[] rndAllCharacters = new char[]{   //number
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            //uppercase
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            //lowercase
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            //special symbols
            '@', '$', '!', '%', '*', '?', '&' };


private static final char[] numberCharacters = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };

private static final char[] uppercaseCharacters = new char[] {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
        };

private static final char[] lowercaseCharacters = new char[] {
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        };

private static final char[] specialSymbolCharacters = new char[] {
        '@', '$', '!', '%', '*', '?', '&'
        };


private MimeMessage createMessage(String code, String emailTo, String title, String content) throws MessagingException, UnsupportedEncodingException {
        MimeMessage  message = javaMailSender.createMimeMessage();

        message.addRecipients(MimeMessage.RecipientType.TO, emailTo); //보내는 사람
        message.setSubject(title+":"); //메일 제목


        // 메일 내용 메일의 subtype을 html로 지정하여 html문법 사용 가능
        String msg="";
        msg += "<head> <link href=\'http://fonts.googleapis.com/css?family=Roboto\' rel=\'stylesheet\' type=\'text/css\'></head>";
        msg += "<div style=\"text-align: center; margin: 20px;\"> <img src=\"https://handsupbucket.s3.ap-northeast-2.amazonaws.com/image/handsUpLogo_orange_2x.png\"></div>";
        msg +="<hr size=\"1px\" color=\"#DBDBDB\">";
        msg += "<h1 style=\"font-size: 16px; text-align: center;  margin-top: 40px; color: #111111; font-family: 'Roboto'; font-weight: 600;\">이메일 주소 확인</h1>";
        msg += "<div style=\"font-size: 12px; text-align: center; color: #747474; font-family: 'Roboto'; font-weight: 400;\">";
        msg += content;
        msg += "</div>";
        msg += "<div style=\"padding-right: 30px; padding-left: 30px; margin: 32px 0 40px;\">";
        msg +=  "<table style=\"border-collapse: collapse; border: 0; background-color: #F47C16; height: 61px; table-layout: fixed; word-wrap: break-word; border-radius: 15px; margin-top: 10px; margin-left:auto; margin-right:auto;\"><tbody> <tr><td style = \"text-align: center; vertical-align: middle; font-size: 32px; color: #FFFFFF; font-family: 'Roboto'; font-weight: 500; padding-left: 109px; padding-right: 109px; padding-top: 11px; padding-bottom: 12px; text-align: center;\">";
        msg += code;
        msg += "</td></tr></tbody></table></div>";
        msg += "<div style=\"font-size: 12px;  text-align: center; color: #111111; font-family: 'Roboto'; font-weight: 500;\"><b>핸즈업</b>에 오신 것을 환영합니다🖐🏻</div>";

        message.setText(msg, "utf-8", "html"); //내용, charset타입, subtype
        message.setFrom(new InternetAddress(email,"HandsUp_Official")); //보내는 사람의 메일 주소, 보내는 사람 이름

        return message;
    }

    private String createCode() {
        StringBuffer code = new StringBuffer();
        Random random = new Random();

        for (int i = 0; i < 5; i++) { // 인증번호 5자리
            code.append((random.nextInt(10))); // 0~9
            }
        return code.toString();
    }

    private String createPw() {
        StringBuilder pwd = new StringBuilder();
        SecureRandom random = new SecureRandom();
        List<Character> passwordCharacters = new ArrayList<>();

        int numberCharactersLength = numberCharacters.length;
        passwordCharacters.add(numberCharacters[random.nextInt(numberCharactersLength)]);

        int uppercaseCharactersLength = uppercaseCharacters.length;
        passwordCharacters.add(uppercaseCharacters[random.nextInt(uppercaseCharactersLength)]);

        int lowercaseCharactersLength = lowercaseCharacters.length;
        passwordCharacters.add(lowercaseCharacters[random.nextInt(lowercaseCharactersLength)]);

        int specialSymbolCharactersLength = specialSymbolCharacters.length;
        passwordCharacters.add(specialSymbolCharacters[random.nextInt(specialSymbolCharactersLength)]);

        int rndAllCharactersLength = rndAllCharacters.length;
        for (int i = 0; i < 6; i++) {
            passwordCharacters.add(rndAllCharacters[random.nextInt(rndAllCharactersLength)]);
        }

        Collections.shuffle(passwordCharacters);

        for (Character character : passwordCharacters) {
            pwd.append(character);
        }
        log.warn("randomPw={}", pwd.toString());
        return pwd.toString();
    }

    //메일 발송
    public String sendMail(String email) throws BaseException {
        //이메일 중복 확인
        Optional<User> optional = this.userRepository.findByEmailAndStatus(email, "ACTIVE");
        if(!optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.EXIST_USER);
        }

        String code = createCode();
        try{
            MimeMessage mimeMessage = createMessage(code, email, "핸즈업 이메일 인증번호", "아래 인증번호를 회원가입에서 입력해주세요.");
            this.javaMailSender.send(mimeMessage);
            return code;
        }catch (Exception e){
            throw new BaseException(BaseResponseStatus.EMAIL_SEND_ERROR);
        }
    }

    public void initPw(String email) throws BaseException {
        //유저인지 확인
        Optional<User> optional = this.userRepository.findByEmailAndStatus(email, "ACTIVE");
        if(optional.isEmpty()){
            throw new BaseException(BaseResponseStatus.NON_EXIST_EMAIL);
        }

        User user = optional.get();

        String pw = createPw();
        try{
            MimeMessage mimeMessage = createMessage(pw, email, "핸즈업 임시 비밀번호", "아래 임시 비밀번호를 로그인에서 입력해주세요.\n로그인 후 비밀번호를 꼭 변경해주세요.");
            this.javaMailSender.send(mimeMessage);
            String encodedPwd = passwordEncoder.encode(pw);
            user.changePWd(encodedPwd);
        }catch (Exception e){
            throw new BaseException(BaseResponseStatus.EMAIL_SEND_ERROR);
        }
    }
}
