package com.sparta.springcore.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sparta.springcore.security.UserDetailsImpl;

import java.util.Date;

public final class JwtTokenUtils {

    private static final int SEC = 1;
    private static final int MINUTE = 60 * SEC;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    // JWT 토큰의 유효기간: 3일 (단위: seconds)
    private static final int JWT_TOKEN_VALID_SEC = 3 * DAY;
    // JWT 토큰의 유효기간: 3일 (단위: milliseconds)
    private static final int JWT_TOKEN_VALID_MILLI_SEC = JWT_TOKEN_VALID_SEC * 1000;

    public static final String CLAIM_EXPIRED_DATE = "EXPIRED_DATE";
    public static final String CLAIM_USER_NAME = "USER_NAME";
//    public static final String CLAIM_USER_PASSWORD = "USER_PASSWORD";
    public static final String JWT_SECRET = "jwt_secret_!@#$%";
//    public static final String CLAIM_NICKNAME = "NICKNAME";
    public static final String CLAIM_EMAIL = "EMAIL";
//    public static final String CLAIM_USER_ID = "USER_ID";

    public static String generateJwtToken(UserDetailsImpl userDetails) {
        String token = null;
        try {
            token = JWT.create()
                    .withIssuer("sparta")
                    .withClaim(CLAIM_USER_NAME, userDetails.getUsername())
//                    .withClaim(CLAIM_USER_PASSWORD, userDetails.getPassword())
//                    .withClaim(CLAIM_NICKNAME, userDetails.getUser().getNickname())
                    .withClaim(CLAIM_EMAIL, userDetails.getUser().getEmail())
//                    .withClaim(CLAIM_USER_ID, userDetails.getU)
                     // 토큰 만료 일시 = 현재 시간 + 토큰 유효기간)
                    .withClaim(CLAIM_EXPIRED_DATE, new Date(System.currentTimeMillis() + JWT_TOKEN_VALID_MILLI_SEC))
                    .sign(generateAlgorithm());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println("JWT tokenUtils jwttokenutils 입니다");
        System.out.println(token);
        return token;
    }

    private static Algorithm generateAlgorithm() {

        System.out.println("JWT token utils algorithm 입니다");
        return Algorithm.HMAC256(JWT_SECRET);
    }
}
