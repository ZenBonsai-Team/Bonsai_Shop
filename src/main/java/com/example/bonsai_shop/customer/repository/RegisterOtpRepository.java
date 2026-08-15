package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.PasswordResetOtp;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
// Repository luu OTP dang ky va quen mat khau theo email.
public interface RegisterOtpRepository extends JpaRepository<PasswordResetOtp, Integer> {
    // Lay OTP moi nhat cua email de validate ma nguoi dung nhap.
    Optional<PasswordResetOtp> findTopByEmailOrderByCreatedAtDesc(String email);


    @Transactional
    // Xoa OTP cu cua email truoc khi tao OTP moi hoac sau khi reset password.
    void deleteByEmail(String email);
}
