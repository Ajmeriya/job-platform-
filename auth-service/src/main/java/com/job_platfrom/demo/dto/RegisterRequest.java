    package com.job_platfrom.demo.dto;

    import com.job_platfrom.demo.Enum.Role;
    import lombok.Getter;
    import lombok.Setter;

    @Getter
    @Setter
    public class RegisterRequest {
        private String name;
        private String email;
        private String password;
        private Role role;
    }
