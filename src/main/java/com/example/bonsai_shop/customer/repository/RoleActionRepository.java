package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.RoleAction;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.RoleActionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
// Repository quan he Role-Action, dung de build authority ACTION_xxx cho SecurityContext.
public interface RoleActionRepository extends JpaRepository<RoleAction, RoleActionId> {
    // Lay tat ca action cua mot role, ke ca action tat.
    List<RoleAction> findByRole(Role role);
    // Lay cac action dang enabled cua role de dua vao phan quyen hasAuthority.
    List<RoleAction> findByRoleRoleIdAndIsEnabledTrue(Integer roleId);
}
