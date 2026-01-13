package com.example.biyeshiji.exception;

import java.time.LocalDateTime;

public class UserBannedException extends RuntimeException {
    private LocalDateTime banUntil;

    public UserBannedException(String message, LocalDateTime banUntil) {
        super(message);
        this.banUntil = banUntil;
    }

    public LocalDateTime getBanUntil() {
        return banUntil;
    }

    public void setBanUntil(LocalDateTime banUntil) {
        this.banUntil = banUntil;
    }
}