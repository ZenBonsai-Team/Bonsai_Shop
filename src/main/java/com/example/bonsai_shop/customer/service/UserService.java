package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.data.common.CloudinaryFolder;
import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.PasswordResetOtp;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.customer.repository.RegisterOtpRepository;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    private final RegisterOtpRepository otpRepository;
    private final CloudinaryStorageService cloudinaryStorageService;

    // ===== ĐĂNG KÝ =====
    @Transactional
    public void register(String fullName, String username, String email, String password, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Tên đăng nhập đã được sử dụng!");
        }

        // Lấy role mặc định
        Role role = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role không tồn tại!"));

        // Lưu user với status PENDING
        User user = User.builder()
                .fullName(fullName)
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .phone(phone)
                .role(role)
                .status("PENDING") // ← chưa kích hoạt
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // Gửi OTP xác thực email
        sendOtp(email);
    }

    // Kích hoạt tài khoản sau khi xác thực OTP
    public void activateUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
        user.setStatus("ACTIVE");
        userRepository.save(user);
    }

    // ===== LẤY USER THEO EMAIL =====
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
    }


    public User getCurrentUserProfile(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
    }

    @Transactional
    public void updateUserProfile(String email, String fullName, String username, String phone,
                                  String address, MultipartFile avatarFile) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

        if (fullName != null) user.setFullName(fullName);
        if (username != null) user.setUsername(username);
        if (phone != null) user.setPhone(phone);
        if (address != null) user.setAddress(address);

        String oldAvatarPublicID = user.getAvatarPublicId();

        if (avatarFile != null && !avatarFile.isEmpty()) {
            CloudinaryUploadResponse result =
                    cloudinaryStorageService.uploadImage(
                            avatarFile,
                            CloudinaryFolder.AVATAR
                    );


            user.setAvatar(result.getUrl());
            user.setAvatarPublicId(result.getPublicId());

            if (oldAvatarPublicID != null && !oldAvatarPublicID.isBlank()) {
                cloudinaryStorageService.deleteFile(oldAvatarPublicID, "image");
            }
        }
        userRepository.save(user);
    }

    // ===== OTP =====
    @Transactional
    public void sendOtp(String email) {
        otpRepository.deleteByEmail(email);

        String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));

        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpRepository.save(otp);
        emailService.sendOtpEmail(email, otpCode);
    }

    public void verifyOtp(String email, String otpCode) {
        PasswordResetOtp otp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("OTP không tồn tại!"));

        if (otp.getIsUsed()) {
            throw new RuntimeException("OTP đã được sử dụng!");
        }
        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP đã hết hạn!");
        }
        if (!otp.getOtpCode().equals(otpCode)) {
            throw new RuntimeException("OTP không đúng!");
        }

        otp.setIsUsed(true);
        otpRepository.save(otp);
    }

    //    GỬI OTP để đặt lại mật khẩu
    @Transactional
    public void sendOtpResetPassword(String email) {
        // Kiểm tra email tồn tại
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        // Xóa OTP cũ
        otpRepository.deleteByEmail(email);

        // Tạo OTP 6 số
        String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));

        // Lưu OTP vào database
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiredAt(LocalDateTime.now().plusMinutes(5)) // hết hạn sau 5 phút
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpRepository.save(otp);

        // Gửi email
        emailService.sendOtpResetPassword(email, otpCode);
    }


    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otpRepository.deleteByEmail(email);
    }

    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không đúng!");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }

        if (newPassword.length() < 3) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 3 ký tự!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ===== LẤY DANH SÁCH TẤT CẢ USER (cho Admin) =====
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ===== ĐỔI ROLE CHO USER =====
    @Transactional
    public void changeUserRole(Integer userId, Integer newRoleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

        Role newRole = roleRepository.findById(newRoleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại!"));

        user.setRole(newRole);
        userRepository.save(user);
    }

    // ===== KHÓA / MỞ KHÓA TÀI KHOẢN =====
    @Transactional
    public void toggleUserStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

        if ("ACTIVE".equals(user.getStatus())) {
            user.setStatus("LOCKED");
        } else {
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);
    }

    public void checkProfileEmailAndPhone(User user) {
        if(user.getPhone() == null || user.getPhone().isBlank()) {
            throw new RuntimeException("Vui lòng cập nhật số điện thoại trước khi đặt lịch");
        }
        if(user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Vui lòng cập nhật email trước khi đặt lịch");
        }
    }

}
