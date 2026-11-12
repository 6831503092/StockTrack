package com.sras.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserCreateRequest(
        @JsonProperty("userId")
        @JsonAlias({"userID"})
        String userId,
        String fullName,
        String role,
        String password,
        String reason
) {
    public UserCreateRequest(String userId, String fullName, String role, String password) {
        this(userId, fullName, role, password, null);
    }

    public String userID() {
        return userId;
    }
}
