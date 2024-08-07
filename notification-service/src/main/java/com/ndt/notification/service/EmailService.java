package com.ndt.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ndt.notification.dto.request.EmailRequest;
import com.ndt.notification.dto.request.SendEmailRequest;
import com.ndt.notification.dto.request.Sender;
import com.ndt.notification.dto.response.EmailResponse;
import com.ndt.notification.exception.AppException;
import com.ndt.notification.exception.ErrorCode;
import com.ndt.notification.repository.httpclient.EmailClient;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {
    EmailClient emailClient;

    String apiKey = "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";

    public EmailResponse sendEmail(SendEmailRequest request) {
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("Tai Nguyen Dinh")
                        .email("tairazerx@gmail.com")
                        .build())
                .to(List.of(request.getTo()))
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            throw new AppException(ErrorCode.CANNOT_SEND_EMAIL);
        }
    }
}
