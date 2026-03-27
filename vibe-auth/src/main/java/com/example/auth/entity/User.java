package com.example.auth.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String role;  // ADMIN, USER, MERCHANT
    private Integer status;  // 0-禁用, 1-启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
