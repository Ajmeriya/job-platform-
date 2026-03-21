package com.job_platfrom.demo.service;

import org.springframework.mail.MailException;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String code) throws MailException;
}
