package com.example.bonsai_shop.owner.service;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.owner.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return accountRepository.findAll();
    }

    @Transactional
    public void createAccount(String fullName, String email, String password, String phone, Integer roleId) {
        if (accountRepository.existsByEmail(email)) {
            throw new RuntimeException("Email da ton tai trong he thong!");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role khong ton tai!"));
        if (isOwnerRole(role) && isCustomerRole(role)) {
            throw new RuntimeException("Khong the tao tai khoan voi quyen Owner va Customer!");
        }

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .phone(phone)
                .role(role)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

         if(password.length() < 6){
             throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự!");
         }
        accountRepository.save(user);
    }

    @Transactional
    public boolean toggleAccountStatus(Integer userId) {
        User user = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan!"));

        boolean lockAccount = !"LOCKED".equals(user.getStatus());
        if (lockAccount && isOwnerRole(user.getRole())) {
            throw new RuntimeException("Khong the khoa tai khoan Owner!");
        }

        user.setStatus(lockAccount ? "LOCKED" : "ACTIVE");
        accountRepository.save(user);
        return lockAccount;
    }

    @Transactional
    public void lockAccount(Integer userId) {
        User user = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan!"));

        if ("LOCKED".equals(user.getStatus())) {
            throw new RuntimeException("Tai khoan da bi khoa roi!");
        }
        if (isOwnerRole(user.getRole())) {
            throw new RuntimeException("Khong the khoa tai khoan Owner!");
        }

        user.setStatus("LOCKED");
        accountRepository.save(user);
    }

    @Transactional
    public void unlockAccount(Integer userId) {
        User user = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan!"));

        if ("ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Tai khoan dang hoat dong roi!");
        }

        user.setStatus("ACTIVE");
        accountRepository.save(user);
    }

    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    public List<Role> findAssignableRoles() {
        return roleRepository.findAll()
                .stream()
                .filter(role -> !isOwnerRole(role) &&  !isCustomerRole(role))
                .toList();
    }

    private boolean isOwnerRole(Role role) {
        if (role == null || role.getRoleName() == null) {
            return false;
        }

        String normalized = role.getRoleName().trim().toUpperCase(Locale.ROOT);
        return "OWNER".equals(normalized) || "ROLE_OWNER".equals(normalized);
    }

    private boolean isCustomerRole(Role role) {
        if (role == null || role.getRoleName() == null) {
            return false;
        }

        String normalized = role.getRoleName().trim().toUpperCase(Locale.ROOT);
        return "CUSTOMER".equals(normalized) || "ROLE_CUSTOMER".equals(normalized);
    }
}
