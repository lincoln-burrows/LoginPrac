package com.sparta.springcore.dto;

import jdk.nashorn.internal.runtime.Debug;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class SignupRequestDto {
    private String username;
    private String password;
    private String email;

}