package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
// Repository thao tac Role, dung khi gan role mac dinh va build authority dang nhap.
public interface RoleRepository extends JpaRepository<Role, Integer> {
    // Tim role theo ten, vi du ROLE_CUSTOMER khi dang ky hoac tao user Google.
    Optional<Role> findByRoleName(String roleName);
}
