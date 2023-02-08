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
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.Random;

@PropertySource("classpath:application.properties")
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class MailService {
    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;
    @Value("${spring.mail.username}")
    private String email;

    private MimeMessage createMessage(String code, String emailTo) throws MessagingException, UnsupportedEncodingException {
        MimeMessage  message = javaMailSender.createMimeMessage();

        message.addRecipients(MimeMessage.RecipientType.TO, emailTo); //보내는 사람
        message.setSubject("핸즈업 이메일 인증번호:"); //메일 제목


        // 메일 내용 메일의 subtype을 html로 지정하여 html문법 사용 가능
        String msg="";
        msg += "<head> <link href=\'http://fonts.googleapis.com/css?family=Roboto\' rel=\'stylesheet\' type=\'text/css\'></head>";
        msg += "<div style=\"text-align: center; margin: 20px;\"> <img src=\"https://s3.us-west-2.amazonaws.com/secure.notion-static.com/b24987da-502b-44a3-ae66-2fec399e7846/handsUpLogo_orange_2x.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Credential=AKIAT73L2G45EIPT3X45%2F20230131%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Date=20230131T083455Z&X-Amz-Expires=86400&X-Amz-Signature=179f5daeb90b8582c0b78594cb69e7b15bc8e1e8204468aaf0ff70b30e6efb0f&X-Amz-SignedHeaders=host&response-content-disposition=filename%3D%22handsUpLogo_orange_2x.png%22&x-id=GetObject\"></div>";
        msg +="<hr size=\"1px\" color=\"#DBDBDB\">";
        msg += "<h1 style=\"font-size: 16px; text-align: center;  margin-top: 40px; color: #111111; font-family: 'Roboto'; font-weight: 600;\">이메일 주소 확인</h1>";
        msg += "<div style=\"font-size: 12px; text-align: center; color: #747474; font-family: 'Roboto'; font-weight: 400;\">아래 인증번호를 회원가입에서 입력해주세요.</div>";
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

    //메일 발송
    public String sendMail(String email) throws BaseException {
        //이메일 중복 확인
        Optional<User> optional = this.userRepository.findByEmail(email);
        if(!optional.isEmpty() && optional.get().getStatus().equals("ACTIVE")){
            throw new BaseException(BaseResponseStatus.EXIST_USER);
        }

        String code = createCode();
        try{
            MimeMessage mimeMessage = createMessage(code, email);
            this.javaMailSender.send(mimeMessage);
            return code;
        }catch (Exception e){
            throw new BaseException(BaseResponseStatus.EMAIL_SEND_ERROR);
        }
    }
}
