package com.example.bonsai_shop.customer.repository;

import com.example.bonsai_shop.entity.RoleAction;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.RoleActionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoleActionRepository extends JpaRepository<RoleAction, RoleActionId> {
    List<RoleAction> findByRole(Role role);
    List<RoleAction> findByRoleRoleIdAndIsEnabledTrue(Integer roleId);
}
