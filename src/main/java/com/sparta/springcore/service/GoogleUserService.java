package com.sparta.springcore.service;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import com.sparta.springcore.dto.GoogleUserInfoDto;
import com.sparta.springcore.dto.GoogleUserResponseDto;
import com.sparta.springcore.model.User;
import com.sparta.springcore.repository.UserRepository;
import com.sparta.springcore.security.GoogleOAuthRequest;
import com.sparta.springcore.security.GoogleOAuthResponse;
import com.sparta.springcore.security.UserDetailsImpl;
import com.sparta.springcore.security.jwt.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import sun.net.www.http.HttpClient;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleUserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Autowired
    public GoogleUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;


    public GoogleUserResponseDto googleLogin(String code) throws JsonProcessingException {
        //HTTP Request??? ?????? RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // 1. "?????? ??????"??? "????????? ??????" ??????
        String accessToken = getAccessToken(restTemplate, code);
        System.out.println("1.\"?????? ??????\"??? \"????????? ??????\" ??????");
        // 2. "????????? ??????"?????? "????????? ????????? ??????" ????????????
        GoogleUserInfoDto snsUserInfoDto = getGoogleUserInfo(restTemplate, accessToken);
        System.out.println("2. ????????? ??????\"?????? \" ????????? ??????\" ????????????");
        // 3. "?????? ????????? ??????"??? ????????? ????????????  ??? ?????? ?????? ???????????? ????????? ?????????????????? ?????????
        User googleUser = registerGoogleOrUpdateGoogle(snsUserInfoDto);
        System.out.println(googleUser.getUsername());
        System.out.println("3. \"?????? ????????? ??????\"??? ????????? ???????????? ??? ?????? ?????? ???????????? ????????? ?????????????????? ?????????");

        // 4. ?????? ????????? ??????
        final String AUTH_HEADER = "Authorization";
        final String TOKEN_TYPE = "BEARER";
        System.out.println("4. ?????? ????????? ??????");

        String jwt_token = forceLogin(googleUser); // ??????????????? ??? ?????? ????????????
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTH_HEADER, TOKEN_TYPE + " " + jwt_token);
        GoogleUserResponseDto googleUserResponseDto = GoogleUserResponseDto.builder()
                .token(TOKEN_TYPE + " " + jwt_token)
                .username(googleUser.getUsername())
                .nickname(googleUser.getNickname())
                .build();
        System.out.println("Google user's token : " + TOKEN_TYPE + " " + jwt_token);
        System.out.println("LOGIN SUCCESS!");
        return googleUserResponseDto;
    }


    private String getAccessToken(RestTemplate restTemplate, String code) throws JsonProcessingException {

        //Google OAuth Access Token ????????? ?????? ???????????? ??????
        GoogleOAuthRequest googleOAuthRequestParam = GoogleOAuthRequest
                .builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .code(code)
//                .redirectUri("https://memegle.xyz/redirect/google")
//                .redirectUri("http://localhost:3000/redirect/google")
                .redirectUri("http://localhost:8080/api/user/google/callback")
                .grantType("authorization_code")
                .accessType("offline")
                .scope("openid https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email").build();


        //JSON ????????? ?????? ????????? ??????
        //????????? ??????????????? ???????????? ???????????? ??????????????? Object mapper??? ?????? ???????????????.
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        //AccessToken ?????? ??????
        ResponseEntity<String> resultEntity = restTemplate.postForEntity("https://oauth2.googleapis.com/token", googleOAuthRequestParam, String.class);
        System.out.println(resultEntity+"resultEntity");
        //Token Request
        GoogleOAuthResponse result = mapper.readValue(resultEntity.getBody(), new TypeReference<GoogleOAuthResponse>() {
        });
        System.out.println(result+"result");
        String jwtToken = result.getId_token();
        System.out.println(jwtToken);
        return jwtToken;
    }


    private GoogleUserInfoDto getGoogleUserInfo(RestTemplate restTemplate, String jwtToken) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String requestUrl = UriComponentsBuilder.fromHttpUrl("https://oauth2.googleapis.com/tokeninfo")
//        String requestUrl = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/oauth2/v3/userinfo")
                .queryParam("id_token", jwtToken).encode().toUriString();

        String resultJson = restTemplate.getForObject(requestUrl, String.class);

        Map<String,String> userInfo = mapper.readValue(resultJson, new TypeReference<Map<String, String>>(){});


        GoogleUserInfoDto googleUserInfoDto = GoogleUserInfoDto.builder()
                .username(userInfo.get("email"))
                .nickname(userInfo.get("name"))
                .build();

        String nickname = userInfo.get("name");
        String email = userInfo.get("email");
        System.out.println("?????? ????????? ??????:  " + nickname + ", " + email);
        return googleUserInfoDto;
    }


    private User registerGoogleOrUpdateGoogle(GoogleUserInfoDto googleUserInfoDto) {

        User sameUser = userRepository.findByUsername(googleUserInfoDto.getUsername())
                .orElse(null);

        if (sameUser == null) {
            return registerGoogleUserIfNeeded(googleUserInfoDto);
        }
        else {
            return updateGoogleUser(sameUser, googleUserInfoDto);
        }
    }

    private User registerGoogleUserIfNeeded(GoogleUserInfoDto googleUserInfoDto) {

        // DB ??? ????????? google Id ??? ????????? ??????
        String googleUserId = googleUserInfoDto.getUsername();
        User googleUser = userRepository.findByUsername(googleUserId)
                .orElse(null);

        if (googleUser == null) {
            // ????????????
            // username: google ID(email)
            String username = googleUserInfoDto.getUsername();

            // nickname: google name
            String nickname = googleUserInfoDto.getNickname();
            Optional<User> user = userRepository.findByNickname(nickname);
            if(user.isPresent()) {
                String dbUserNickname = user.get().getNickname();

                int beginIndex= nickname.length();
                String nicknameIndex = dbUserNickname.substring(beginIndex, dbUserNickname.length());

                if (!nicknameIndex.isEmpty()) {
                    int newIndex = Integer.parseInt(nicknameIndex) + 1;
                    nickname = nickname + newIndex;
                } else {
                    nickname = dbUserNickname + 1;
                }
            }

            // password: random UUID
            String password = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);

//            googleUser = new User("nickname", encodedPassword, username);

            googleUser = User.builder()
                    .username(nickname)
                    .password(encodedPassword)
                    .email(username)
                    .build();
            userRepository.save(googleUser);
        }

        return googleUser;
    }

    private User updateGoogleUser(User sameUser, GoogleUserInfoDto googleUserInfoDto) {
        if (sameUser.getUsername() == null) {
            System.out.println("??????");
            sameUser.setUsername(googleUserInfoDto.getUsername());
            sameUser.setNickname(googleUserInfoDto.getNickname());
            userRepository.save(sameUser);
        }
        return sameUser;
    }

    private String forceLogin(User googleUser) {
        UserDetailsImpl userDetails = new UserDetailsImpl(googleUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("??????????????? ????????????");
//        UserDetailsImpl userDetails = UserDetailsImpl.builder()
//                .username(googleUser.getUsername())
//                .password(googleUser.getPassword())
//                .build();
        System.out.println(userDetails.getUsername()+"?????????????????? ????????????");


        return JwtTokenUtils.generateJwtToken(userDetails);
    }
}