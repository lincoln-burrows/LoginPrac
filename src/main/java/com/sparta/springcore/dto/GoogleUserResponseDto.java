package com.sparta.springcore.dto;


import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoogleUserResponseDto {
    private String token;
    private String username;
    private String nickname;
}