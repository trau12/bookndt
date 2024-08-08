package com.ndt.notification.controller;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ndt.event.dto.NotificationEvent;
import com.ndt.notification.dto.request.Recipient;
import com.ndt.notification.dto.request.SendEmailRequest;
import com.ndt.notification.exception.AppException;
import com.ndt.notification.service.EmailService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    EmailService emailService;

    @KafkaListener(topics = "notification-delivery")
    public void listenNotificationDelivery(NotificationEvent message) {
        log.info("Message received: {}", message);
        try {
            emailService.sendEmail(SendEmailRequest.builder()
                    .to(Recipient.builder().email(message.getRecipient()).build())
                    .subject(message.getSubject())
                    .htmlContent(message.getBody())
                    .build());

        } catch (AppException e) {
            log.error("Không thể gửi email : {}. Lỗi: {}", message, e.getMessage());
            // Xử lý lỗi: có thể thử lại, gửi vào hàng đợi lỗi, hoặc các hành động khác
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi xử lý thông báo: {}. Lỗi: {}", message, e.getMessage(), e);
            // Xử lý các ngoại lệ không mong đợi
        }
    }
}
