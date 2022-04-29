package com.sparta.springcore.mail;


import com.sparta.springcore.model.EmailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

public interface EmailService {

    @Async
    void sendEmail(EmailMessage emailMessage);

}