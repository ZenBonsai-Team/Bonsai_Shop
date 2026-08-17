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

@Service
@RequiredArgsConstructor
// Service quan ly user customer: dang ky, OTP, profile va mat khau.
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RegisterOtpRepository otpRepository;
    private final CloudinaryStorageService cloudinaryStorageService;

    // ===== ĐĂNG KÝ =====
    // Dang ky customer moi bang email/password, luu trang thai PENDING va gui OTP.
    @Transactional
    public void register(String fullName, String username, String email, String password, String phone) {
        // Validate email khong duoc trung vi email la dinh danh dang nhap.
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        // Validate username khong duoc trung.
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Tên đăng nhập đã được sử dụng!");
        }

        // Lấy role mặc định
        // Tai khoan tu form dang ky luon duoc gan ROLE_CUSTOMER.
        Role role = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role không tồn tại!"));

        // Lưu user với status PENDING
        // Ma hoa mat khau truoc khi luu va giu PENDING cho den khi xac thuc OTP.
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

        // Luu user moi vao database truoc khi tao OTP.
        userRepository.save(user);

        // Gửi OTP xác thực email
        // Tao va gui OTP xac thuc email dang ky.
        sendOtp(email);
    }

    // Kích hoạt tài khoản sau khi xác thực OTP
    // Chuyen tai khoan tu PENDING sang ACTIVE sau khi OTP hop le.
    public void activateUser(String email) {
        // Tim user theo email, neu khong co thi khong the kich hoat.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
        user.setStatus("ACTIVE");
        userRepository.save(user);
    }

    // ===== LẤY USER THEO EMAIL =====
    // Lay User theo email hoac nem loi neu khong tim thay.
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
    }

    // Lay profile user hien tai theo email trong SecurityContext.
    public User getCurrentUserProfile(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
    }

    // Cap nhat thong tin profile va upload avatar moi neu co.
    @Transactional
    public void updateUserProfile(String email, String fullName, String username, String phone,
            String address, MultipartFile avatarFile) {
        // Validate user phai ton tai truoc khi cap nhat profile.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

        // Chi cap nhat truong nao duoc gui len, cac truong null giu nguyen.
        if (fullName != null)
            user.setFullName(fullName);
        if (username != null)
            user.setUsername(username);
        if (phone != null)
            user.setPhone(phone);
        if (address != null)
            user.setAddress(address);

        // Luu publicId avatar cu de xoa tren Cloudinary sau khi upload avatar moi thanh
        // cong.
        String oldAvatarPublicID = user.getAvatarPublicId();

        // Neu co file avatar moi thi upload len Cloudinary.
        if (avatarFile != null && !avatarFile.isEmpty()) {
            CloudinaryUploadResponse result = cloudinaryStorageService.uploadImage(
                    avatarFile,
                    CloudinaryFolder.AVATAR);

            // Cap nhat URL/publicId moi vao user.
            user.setAvatar(result.getUrl());
            user.setAvatarPublicId(result.getPublicId());

            // Xoa file avatar cu de tranh rac storage.
            if (oldAvatarPublicID != null && !oldAvatarPublicID.isBlank()) {
                cloudinaryStorageService.deleteFile(oldAvatarPublicID, "image");
            }
        }
        userRepository.save(user);
    }

    // ===== OTP =====
    // Tao OTP moi cho email dang ky va gui qua email.
    @Transactional
    public void sendOtp(String email) {
        // Xoa OTP cu cua email de chi OTP moi nhat con hieu luc.
        otpRepository.deleteByEmail(email);

        // Sinh ma OTP 6 chu so.
        String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));

        // Luu OTP voi han su dung 5 phut va trang thai chua dung.
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email(email)
                .otpCode(otpCode)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Luu OTP roi gui ma qua email cho nguoi dung.
        otpRepository.save(otp);
        emailService.sendOtpEmail(email, otpCode);
    }

    // Kiem tra OTP dang ky co ton tai, chua dung, chua het han va dung ma nguoi
    // dung nhap.
    public void verifyOtp(String email, String otpCode) {
        // Lay OTP moi nhat cua email.
        PasswordResetOtp otp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("OTP không tồn tại!"));

        // Validate OTP chua duoc su dung.
        if (otp.getIsUsed()) {
            throw new RuntimeException("OTP đã được sử dụng!");
        }
        // Validate OTP chua het han.
        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP đã hết hạn!");
        }
        // Validate ma OTP nguoi dung nhap phai khop voi ma da gui.
        if (!otp.getOtpCode().equals(otpCode)) {
            throw new RuntimeException("OTP không đúng!");
        }

        // Danh dau OTP da dung de khong the dung lai.
        otp.setIsUsed(true);
        otpRepository.save(otp);
    }

    // GỬI OTP để đặt lại mật khẩu
    // Tao OTP cho luong quen mat khau sau khi xac nhan email ton tai.
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

    // Doi mat khau moi sau luong quen mat khau va xoa OTP lien quan.
    @Transactional
    public void resetPassword(String email, String newPassword) {
        // Validate email phai ton tai trong he thong.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));
        // Validate mat khau moi toi thieu 6 ky tu.
        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự!");
        }
        // Ma hoa mat khau moi truoc khi luu.
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otpRepository.deleteByEmail(email);
    }

    // Doi mat khau khi user dang dang nhap, yeu cau mat khau cu dung va xac nhan
    // khop.
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword, String confirmPassword) {
        // Validate email phai ton tai.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        // Validate mat khau cu bang BCrypt matches.
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không đúng!");
        }

        // Validate mat khau moi va xac nhan mat khau phai giong nhau.
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }

        // Validate do dai mat khau moi.
        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự!");
        }

        // Luu mat khau moi da ma hoa.
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Validate profile co phone/email truoc khi cho thuc hien cac chuc nang can
    // thong tin lien he.
    public void checkProfileEmailAndPhone(User user) {
        // So dien thoai bat buoc khi dat lich.
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new RuntimeException("Vui lòng cập nhật số điện thoại trước khi đặt lịch");
        }
        // Email bat buoc khi dat lich.
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Vui lòng cập nhật email trước khi đặt lịch");
        }
    }

}
