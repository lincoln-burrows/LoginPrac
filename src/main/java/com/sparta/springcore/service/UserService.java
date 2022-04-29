package com.sparta.springcore.service;

import com.sparta.springcore.dto.CMResponseDto;
import com.sparta.springcore.dto.EmailRequestDto;
import com.sparta.springcore.dto.SignupRequestDto;
import com.sparta.springcore.mail.EmailService;
import com.sparta.springcore.model.EmailMessage;
import com.sparta.springcore.model.User;
import com.sparta.springcore.repository.UserRepository;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.activity.InvalidActivityException;
import javax.transaction.Transactional;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailService emailService;


    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void registerUser(SignupRequestDto requestDto) {
        // 회원 ID 중복 확인
        String username = requestDto.getUsername();
        System.out.println(username+"1");
        Optional<User> found = userRepository.findByUsername(username);
        System.out.println(found+"2");

        if (found.isPresent()) {
            throw new IllegalArgumentException("중복된 사용자 ID 가 존재합니다.");
        }

        // 패스워드 암호화
        String password = passwordEncoder.encode(requestDto.getPassword());
        String email = requestDto.getEmail();
        System.out.println(password+"3");


        User user = new User(username, password, email);
        userRepository.save(user);
        System.out.println(user+"4");

    }

    @Transactional
    public ResponseEntity<CMResponseDto> sendTempPassword(EmailRequestDto emailRequestDto) throws InvalidActivityException, NotFoundException {

        User findUser = userRepository.findByEmail(emailRequestDto.getEmail()).orElseThrow(
                () -> new NotFoundException("존재하지 않는 이메일입니다.")
        );
        System.out.println("이메일 존재여부 체크");
         //인증 이메일 1시간 지났는지 체크
//        if (!findUser.canSendConfirmEmail())
//            throw new InvalidActivityException("인증 이메일은 1시간에 한번만 전송할 수 있습니다.");
//        System.out.println("인증이메일 1시간 체크");
        String tempPassword = temporaryPassword(10); // 8글자 랜덤으로 임시 비밀번호 생성

        String tempEncPassword = passwordEncoder.encode(tempPassword); // 암호화
        System.out.println("암호화"+tempEncPassword);
        findUser.changeTempPassword(tempEncPassword);

        sendTempPasswordConfirmEmail(findUser, tempPassword);
        System.out.println("작업완료");
        return ResponseEntity.ok(new CMResponseDto("true"));
    }

    private void sendTempPasswordConfirmEmail(User user, String tempPwd) {
        EmailMessage emailMessage = EmailMessage.builder()
                .to(user.getEmail())
                .subject("소행성(소소한 행동 습관 형성 챌린지), 임시 비밀번호 발급")
                .message("<p>임시 비밀번호: <b>" + tempPwd + "</b></p><br>" +
                        "<p>로그인 후 비밀번호를 변경해주세요.</p>")
                .build();
        System.out.println("sendTempPasswordConfirmEmail");
        System.out.println(emailMessage);
        emailService.sendEmail(emailMessage);
        System.out.println("sendEmail");
    }

    private String temporaryPassword(int size) {
        StringBuffer buffer = new StringBuffer();
        Random random = new Random();
        String chars[] = ("A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z," +
                "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,0,1,2,3,4,5,6,7,8,9").split(",");
        for (int i = 0; i < size; i++) {
            buffer.append(chars[random.nextInt(chars.length)]);
        }
        buffer.append("!a1");
        System.out.println("임시비밀번호 생성"+buffer.toString());
        return buffer.toString();
    }
//
//
//    private void sendSignupConfirmEmail(User user) {
//        String path = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
//
//        Context context = new Context();
//        context.setVariable("link", path+"/auth/check-email-token?token=" + user.getEmailCheckToken() +
//                "&email=" + user.getEmail());
//
//        String message = templateEngine.process("mail/email-link", context);
//
//        EmailMessage emailMessage = EmailMessage.builder()
//                .to(user.getEmail())
//                .subject("소행성(소소한 행동 습관 형성 챌린지), 회원 가입 인증 메일")
//                .message(message)
//                .build();
//
//        emailService.sendEmail(emailMessage);
//    }
}