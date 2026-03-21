package com.job_platfrom.demo.service;

import com.job_platfrom.demo.entity.User;

public interface AuthService {

    User register(User user);

    void verifyUser(String email, String code);

    User login(String email, String password);
}
