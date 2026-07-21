package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.entity.User;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ROLE_OWNER') or hasAuthority('ACTION_USER_MANAGE')")

public class AuthenticationController {
    public String managerUser(User user) {
        return "managerUser";
    }
}
