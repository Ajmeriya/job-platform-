package com.job_platfrom.demo.service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public void sendVerificationEmail(String toEmail, String code) throws MailException {
        String verificationLink = baseUrl + "/auth/verify";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            // Include code/time to avoid Gmail threading hiding newer emails.
            helper.setSubject("AI Hiring verification code: " + code + " [" + LocalDateTime.now() + "]");
            helper.setText(
                    "Welcome to AI Hiring Platform.\n\n" +
                    "Your verification code is: " + code + "\n\n" +
                    "Verify endpoint: " + verificationLink + "\n\n" +
                    "This code expires in 15 minutes.",
                    false
            );

            mailSender.send(mimeMessage);
            log.info("Verification email accepted by SMTP for {}", toEmail);
        } catch (Exception ex) {
            log.error("Verification email send failed for {}", toEmail, ex);
            throw new org.springframework.mail.MailSendException("Unable to send verification email", ex);
        }

        // Fallback output so user can proceed even if inbox delivery is delayed.
        System.out.println("Verification code for " + toEmail + ": " + code);
    }
}
