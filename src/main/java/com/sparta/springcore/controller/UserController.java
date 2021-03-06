package com.sparta.springcore.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sparta.springcore.dto.*;
import com.sparta.springcore.service.GoogleUserService;
import com.sparta.springcore.service.KakaoUserService;
import com.sparta.springcore.service.UserService;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.activity.InvalidActivityException;
import javax.validation.Valid;

@RestController
public class UserController {

    private final UserService userService;
    private final KakaoUserService kakaoUserService;
    private final GoogleUserService googleUserService;

    @Autowired
    public UserController(UserService userService, KakaoUserService kakaoUserService, GoogleUserService googleUserService) {
        this.userService = userService;
        this.kakaoUserService  = kakaoUserService;
        this.googleUserService = googleUserService;
    }

//    // 회원 로그인 페이지
//    @GetMapping("/user/loginView")
//    public String login() {
//        return "login";
//    }

    // 회원 가입 페이지
    @GetMapping("/user/signup")
    public String signup() {
        return "signup.html";
    }

    // 회원 가입 요청 처리
    @PostMapping("/user/signup")
    public String registerUser(@RequestBody SignupRequestDto requestDto) {
        userService.registerUser(requestDto);
        System.out.println(requestDto);
        return "redirect:/user/loginView";
    }
    //카카오 로그인
    @GetMapping("/user/kakao/callback")
    public String kakaoLogin(@RequestParam String code) throws JsonProcessingException {
        System.out.println("제일 시작점");
        kakaoUserService.kakaoLogin(code);
        return "redirect:/";
    }

    //구글 로그인
    @GetMapping("/api/user/google/callback")
//    @GetMapping("/login/oauth2/code/google")
    public String googleLogin(@RequestParam String code) throws JsonProcessingException {
//    public ResponseDto<GoogleUserResponseDto> googleLogin(@RequestParam String code) throws JsonProcessingException {
        System.out.println("구글로그인 시작");
         ResponseDto.<GoogleUserResponseDto>builder()
                .status(HttpStatus.OK.toString())
                .message("구글 소셜 로그인 요청")
                .data(googleUserService.googleLogin(code))
                .build();
         return "redirect:/";
    }

    //테스트용

    // 회원 로그인 페이지
    @GetMapping("/user/login")
    public String login2() {
        return "login";
    }

    //임시 비밀번호 보내기
    @PostMapping("/auth/send-temp-password")
    public ResponseEntity<CMResponseDto> sendTempPassword(@RequestBody @Valid EmailRequestDto emailRequestDto) throws InvalidActivityException, NotFoundException {
        System.out.println("이메일 보내기 시작");
        return userService.sendTempPassword(emailRequestDto);
    }

}