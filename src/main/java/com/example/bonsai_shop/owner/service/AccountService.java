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

    // Lay toan bo tai khoan de hien thi cho Owner tren man hinh quan ly nguoi dung.
    public List<User> findAll() {
        return accountRepository.findAll();
    }

    // Tao tai khoan nhan vien moi sau khi chuan hoa input, validate va ma hoa mat khau.
    @Transactional
    public void createAccount(String fullName, String email, String password, String phone, Integer roleId) {
        // Chuan hoa du lieu dau vao truoc khi validate de tranh loi do khoang trang/chu hoa.
        fullName = fullName == null ? "" : fullName.trim();
        email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        password = password == null ? "" : password;
        phone = phone == null ? "" : phone.trim();

        // Validate cac truong bat buoc va dinh dang truoc khi truy van database.
        validateCreateAccountInput(fullName, email, password, phone, roleId);

        // Chan tao trung email vi email la dinh danh dang nhap cua tai khoan.
        if (accountRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        // Lay role duoc chon va dam bao Owner khong tao duoc Owner/Customer tu man hinh nay.
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Phân quyền không tồn tại!"));
        if (isOwnerRole(role) || isCustomerRole(role)) {
            throw new RuntimeException("Không thể tạo tài khoản với quyền Owner hoặc Customer!");
        }

        // Build entity User moi voi trang thai mac dinh ACTIVE va mat khau da ma hoa.
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

    // Validate du lieu tao tai khoan de bao loi som ngay tai tang service.
    private void validateCreateAccountInput(String fullName, String email, String password, String phone, Integer roleId) {
        // Ho ten can co do dai hop le de tranh du lieu rong hoac qua dai.
        if (fullName.length() < 3 || fullName.length() > 50) {
            throw new RuntimeException("Họ và tên phải có từ 3 đến 50 ký tự!");
        }
        // Email phai dung dinh dang co ban de co the dung lam tai khoan dang nhap.
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Email không đúng định dạng!");
        }
        // Mat khau toi thieu 6 ky tu truoc khi ma hoa.
        if (password.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự!");
        }
        // So dien thoai la tuy chon, nhung neu nhap thi phai dung dinh dang Viet Nam bat dau bang 0.
        if (!phone.isBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new RuntimeException("Số điện thoại phải bắt đầu bằng 0 và có từ 10 đến 11 chữ số!");
        }
        // Role bat buoc de xac dinh tai khoan moi thuoc nhom nhan vien nao.
        if (roleId == null) {
            throw new RuntimeException("Vui lòng chọn phân quyền!");
        }
    }

    // Dao trang thai tai khoan: ACTIVE -> LOCKED hoac LOCKED -> ACTIVE.
    @Transactional
    public boolean toggleAccountStatus(Integer userId) {
        // Tim tai khoan can thao tac, neu khong co thi bao loi cho controller.
        User user = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan!"));

        // Neu tai khoan hien khong LOCKED thi hanh dong tiep theo se la khoa.
        boolean lockAccount = !"LOCKED".equals(user.getStatus());
        if (lockAccount && isOwnerRole(user.getRole())) {
            throw new RuntimeException("Khong the khoa tai khoan Owner!");
        }

        // Cap nhat status theo hanh dong vua xac dinh va luu lai.
        user.setStatus(lockAccount ? "LOCKED" : "ACTIVE");
        accountRepository.save(user);
        return lockAccount;
    }

    // Khoa tai khoan nhan vien, khong cho phep khoa tai khoan Owner.
    @Transactional
    public void lockAccount(Integer userId) {
        User user = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan!"));

        // Validate trang thai hien tai de khong khoa lap lai mot tai khoan da LOCKED.
        if ("LOCKED".equals(user.getStatus())) {
            throw new RuntimeException("Tai khoan da bi khoa roi!");
        }
        // Bao ve tai khoan Owner de tranh mat quyen quan tri he thong.
        if (isOwnerRole(user.getRole())) {
            throw new RuntimeException("Khong the khoa tai khoan Owner!");
        }

        // Gan status LOCKED va persist vao database.
        user.setStatus("LOCKED");
        accountRepository.save(user);
    }

    // Mo khoa tai khoan dang bi LOCKED ve ACTIVE.
    @Transactional
    public void unlockAccount(Integer userId) {
        User user = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan!"));

        // Validate de tranh thao tac mo khoa lap lai voi tai khoan da ACTIVE.
        if ("ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Tai khoan dang hoat dong roi!");
        }

        // Gan lai ACTIVE de tai khoan co the dang nhap/su dung he thong.
        user.setStatus("ACTIVE");
        accountRepository.save(user);
    }

    // Lay tat ca role trong he thong, dung khi can hien thi/kiem tra day du.
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    // Lay cac role Owner duoc phep gan cho tai khoan nhan vien moi.
    public List<Role> findAssignableRoles() {
        return roleRepository.findAll()
                .stream()
                // Khong cho gan Owner va Customer tu man hinh tao nhan vien.
                .filter(role -> !isOwnerRole(role) &&  !isCustomerRole(role))
                .toList();
    }

    // Nhan dien role Owner theo ca hai cach luu: OWNER hoac ROLE_OWNER.
    private boolean isOwnerRole(Role role) {
        if (role == null || role.getRoleName() == null) {
            return false;
        }

        String normalized = role.getRoleName().trim().toUpperCase(Locale.ROOT);
        return "OWNER".equals(normalized) || "ROLE_OWNER".equals(normalized);
    }

    // Nhan dien role Customer de chan Owner tao truc tiep tai khoan khach hang.
    private boolean isCustomerRole(Role role) {
        if (role == null || role.getRoleName() == null) {
            return false;
        }

        String normalized = role.getRoleName().trim().toUpperCase(Locale.ROOT);
        return "CUSTOMER".equals(normalized) || "ROLE_CUSTOMER".equals(normalized);
    }
}
