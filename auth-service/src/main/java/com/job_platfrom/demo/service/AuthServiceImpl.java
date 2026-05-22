package com.job_platfrom.demo.service;

import com.job_platfrom.demo.entity.User;
import com.job_platfrom.demo.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Random random = new Random();

    @Override
    public User register(User user) {
        User targetUser = userRepository.findByEmail(user.getEmail()).orElse(new User());

        if (targetUser.getId() != null && Boolean.TRUE.equals(targetUser.getIsVerified())) {
            throw new RuntimeException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(user.getPassword());
        String code = String.format("%06d", random.nextInt(1_000_000));

        targetUser.setName(user.getName());
        targetUser.setEmail(user.getEmail());
        targetUser.setPassword(hashedPassword);
        targetUser.setRole(user.getRole());
        targetUser.setVerificationToken(code);
        targetUser.setTokenExpiry(LocalDateTime.now().plusMinutes(15));
        targetUser.setIsVerified(false);

        User savedUser = userRepository.save(targetUser);

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), code);
        } catch (MailException ex) {
            throw new RuntimeException("Failed to send verification email. " + ex.getMessage());
        }

        return savedUser;
    }

    @Override
    public void verifyUser(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email"));

        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(code)) {
            throw new RuntimeException("Invalid verification code");
        }

        if (user.getTokenExpiry() == null || user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code expired");
        }

        user.setIsVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);
    }

    @Override
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (Boolean.FALSE.equals(user.getIsVerified())) {
            throw new RuntimeException("Please verify your email first");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return user;
    }
}
