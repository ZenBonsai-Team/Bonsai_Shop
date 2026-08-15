package com.example.bonsai_shop.owner.repository;

import com.example.bonsai_shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<User, Integer> {
    // Kiem tra email da duoc dung cho tai khoan nao trong he thong chua.
    boolean existsByEmail(String email);
}
