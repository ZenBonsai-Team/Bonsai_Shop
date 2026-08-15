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
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class AccountService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9,10}$");

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return accountRepository.findAll();
    }

    @Transactional
    public void createAccount(String fullName, String email, String password, String phone, Integer roleId) {
        fullName = fullName == null ? "" : fullName.trim();
        email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        password = password == null ? "" : password;
        phone = phone == null ? "" : phone.trim();

        validateCreateAccountInput(fullName, email, password, phone, roleId);

        if (accountRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Phân quyền không tồn tại!"));
        if (isOwnerRole(role) || isCustomerRole(role)) {
            throw new RuntimeException("Không thể tạo tài khoản với quyền Owner hoặc Customer!");
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

        accountRepository.save(user);
    }

    private void validateCreateAccountInput(String fullName, String email, String password, String phone, Integer roleId) {
        if (fullName.length() < 3 || fullName.length() > 50) {
            throw new RuntimeException("Họ và tên phải có từ 3 đến 50 ký tự!");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Email không đúng định dạng!");
        }
        if (password.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự!");
        }
        if (!phone.isBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new RuntimeException("Số điện thoại phải bắt đầu bằng 0 và có từ 10 đến 11 chữ số!");
        }
        if (roleId == null) {
            throw new RuntimeException("Vui lòng chọn phân quyền!");
        }
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
