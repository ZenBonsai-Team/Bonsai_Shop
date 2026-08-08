package com.example.bonsai_shop.system;

import com.example.bonsai_shop.customer.repository.RoleRepository;
import com.example.bonsai_shop.customer.service.CustomUserDetails;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.owner.repository.AccountRepository;
import com.example.bonsai_shop.owner.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BF05StaffAccountSystemTest {

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor ownerUser() {
        User owner = User.builder()
                .userId(1)
                .email("owner@bonsai.com")
                .password("123")
                .fullName("Owner")
                .avatar("")
                .role(Role.builder().roleId(2).roleName("OWNER").build())
                .status("ACTIVE")
                .build();

        return user(new CustomUserDetails(owner, List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));
    }

    private Role findAssignableStaffRole() {
        return roleRepository.findAll()
                .stream()
                .filter(role ->
                        !"OWNER".equalsIgnoreCase(role.getRoleName())
                                && !"ROLE_OWNER".equalsIgnoreCase(role.getRoleName())
                                && !"CUSTOMER".equalsIgnoreCase(role.getRoleName())
                                && !"ROLE_CUSTOMER".equalsIgnoreCase(role.getRoleName())
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No assignable staff role found"));
    }

    private void deleteUserByEmail(String email) {
        accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .forEach(accountRepository::delete);
    }

    @Test
    void tcSysBF05001_ownerLoginSuccessfully() throws Exception {

        mockMvc.perform(formLogin("/login")
                        .user("owner@bonsai.com")
                        .password("123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner"));
    }

    @Test
    void tcSysBF05002_ownerCanViewAccountList() throws Exception {

        mockMvc.perform(get("/owner/users").with(ownerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/user_list"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    void tcSysBF05003_ownerCanOpenCreateStaffPage() throws Exception {

        mockMvc.perform(get("/owner/users/create").with(ownerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/users_create"))
                .andExpect(model().attributeExists("roles"));
    }
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void tcSysBF05004_ownerCanCreateStaffAccount() throws Exception {

        Role staffRole = findAssignableStaffRole();

        String email = "staff.bf05@test.com";
        String rawPassword = "123456";

        deleteUserByEmail(email);

        mockMvc.perform(post("/owner/users/create")
                        .with(ownerUser())
                        .with(csrf())
                        .param("fullName", "BF05 Test Staff")
                        .param("email", email)
                        .param("password", rawPassword)
                        .param("phone", "0900000000")
                        .param("roleId",
                                String.valueOf(staffRole.getRoleId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash()
                        .attribute("success",
                                "Tao tai khoan thanh cong cho "
                                        + email + "!"));

        User createdUser = accountRepository.findAll()
                .stream()
                .filter(user ->
                        email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Created staff account was not found"));

        assertEquals("BF05 Test Staff",
                createdUser.getFullName());

        assertEquals(email,
                createdUser.getEmail());

        assertEquals("ACTIVE",
                createdUser.getStatus());

        assertEquals(staffRole.getRoleId(),
                createdUser.getRole().getRoleId());

        assertNotNull(createdUser.getCreatedAt());

        assertNotEquals(rawPassword,
                createdUser.getPassword());

        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        createdUser.getPassword()
                )
        );
    }

    @Test
    void tcSysBF05005_createdStaffAppearsInAccountList() throws Exception {

        String email = "staff.bf05@test.com";
        String rawPassword = "123456";
        Role staffRole = findAssignableStaffRole();

        deleteUserByEmail(email);

        mockMvc.perform(post("/owner/users/create")
                        .with(ownerUser())
                        .with(csrf())
                        .param("fullName", "BF05 Test Staff")
                        .param("email", email)
                        .param("password", rawPassword)
                        .param("phone", "0900000000")
                        .param("roleId", String.valueOf(staffRole.getRoleId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"));

        mockMvc.perform(get("/owner/users")
                        .with(ownerUser()))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/user_list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute(
                        "users",
                        hasItem(
                                hasProperty(
                                        "email",
                                        equalTo(email)
                                )
                        )
                ));
    }

    @Autowired
    private AccountService accountService;

    @Test
    void tcSysBF05006_newStaffCanLoginWithAssignedRole() throws Exception {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName())
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "artisan.bf05@test.com";
        String rawPassword = "123456";

        // Ensure test data exists
        if (!accountRepository.existsByEmail(email)) {
            accountService.createAccount(
                    "BF05 Artisan Test",
                    email,
                    rawPassword,
                    "0900000001",
                    artisanRole.getRoleId()
            );
        }

        mockMvc.perform(formLogin("/login")
                        .user(email)
                        .password(rawPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan"));
    }

    private RequestPostProcessor artisanUser() {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName())
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "artisan.bf05@test.com";

        User artisan = accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    accountService.createAccount(
                            "BF05 Artisan Test",
                            email,
                            "123456",
                            "0900000001",
                            artisanRole.getRoleId()
                    );

                    return accountRepository.findAll()
                            .stream()
                            .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                            .findFirst()
                            .orElseThrow();
                });

        artisan.setStatus("ACTIVE");
        artisan.setRole(artisanRole);
        accountRepository.save(artisan);

        return user(
                new CustomUserDetails(
                        artisan,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ARTISAN")
                        )
                )
        );
    }

    @Test
    void tcSysBF05007_staffCanAccessAuthorizedFunction() throws Exception {

        mockMvc.perform(get("/artisan")
                        .with(artisanUser()))
                .andExpect(status().isOk());
    }

    @Test
    void tcSysBF05008_staffCannotAccessOwnerArea() throws Exception {

        mockMvc.perform(get("/owner/users")
                        .with(artisanUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void tcSysBF05009_ownerCanLockStaffAccount() throws Exception {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "lock.bf05@test.com";

        User staff = accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    accountService.createAccount(
                            "BF05 Lock Test",
                            email,
                            "123456",
                            "0900000002",
                            artisanRole.getRoleId()
                    );

                    return accountRepository.findAll()
                            .stream()
                            .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                            .findFirst()
                            .orElseThrow();
                });

        staff.setStatus("ACTIVE");
        accountRepository.save(staff);

        mockMvc.perform(post("/owner/users/lock")
                        .with(ownerUser())
                        .with(csrf())
                        .param("userId",
                                String.valueOf(staff.getUserId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash()
                        .attribute("success",
                                "Da khoa tai khoan!"));

        User lockedStaff = accountRepository
                .findById(staff.getUserId())
                .orElseThrow();

        assertEquals("LOCKED",
                lockedStaff.getStatus());
    }

    @Test
    void tcSysBF05010_lockedStaffCannotLogin() throws Exception {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "locked.login.bf05@test.com";
        String rawPassword = "123456";

        User staff = accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    accountService.createAccount(
                            "BF05 Locked Login Test",
                            email,
                            rawPassword,
                            "0900000003",
                            artisanRole.getRoleId()
                    );

                    return accountRepository.findAll()
                            .stream()
                            .filter(user ->
                                    email.equalsIgnoreCase(user.getEmail()))
                            .findFirst()
                            .orElseThrow();
                });

        staff.setStatus("LOCKED");
        accountRepository.save(staff);

        mockMvc.perform(formLogin("/login")
                        .user(email)
                        .password(rawPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void tcSysBF05011_ownerCanUnlockStaffAccount() throws Exception {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "unlock.bf05@test.com";

        User staff = accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseGet(() -> {

                    accountService.createAccount(
                            "BF05 Unlock Test",
                            email,
                            "123456",
                            "0900000011",
                            artisanRole.getRoleId()
                    );

                    return accountRepository.findAll()
                            .stream()
                            .filter(user ->
                                    email.equalsIgnoreCase(user.getEmail()))
                            .findFirst()
                            .orElseThrow();
                });

        // Arrange: account must be LOCKED
        staff.setStatus("LOCKED");
        accountRepository.save(staff);

        mockMvc.perform(post("/owner/users/unlock")
                        .with(ownerUser())
                        .with(csrf())
                        .param(
                                "userId",
                                String.valueOf(staff.getUserId())
                        ))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash()
                        .attribute(
                                "success",
                                "Da mo khoa tai khoan!"
                        ));

        User unlockedStaff = accountRepository
                .findById(staff.getUserId())
                .orElseThrow();

        assertEquals(
                "ACTIVE",
                unlockedStaff.getStatus()
        );
    }

    @Test
    void tcSysBF05012_unlockedStaffCanLoginAgain() throws Exception {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "unlocked.login.bf05@test.com";
        String rawPassword = "123456";

        User staff = accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseGet(() -> {

                    accountService.createAccount(
                            "BF05 Unlocked Login Test",
                            email,
                            rawPassword,
                            "0900000012",
                            artisanRole.getRoleId()
                    );

                    return accountRepository.findAll()
                            .stream()
                            .filter(user ->
                                    email.equalsIgnoreCase(user.getEmail()))
                            .findFirst()
                            .orElseThrow();
                });

        // Arrange: ensure account is ACTIVE
        staff.setStatus("ACTIVE");
        accountRepository.save(staff);

        mockMvc.perform(formLogin("/login")
                        .user(email)
                        .password(rawPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artisan"));
    }

    @Test
    void tcSysBF05013_cannotCreateStaffWithDuplicateEmail() throws Exception {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "duplicate.bf05@test.com";

        // Arrange: ensure email already exists
        if (!accountRepository.existsByEmail(email)) {

            accountService.createAccount(
                    "Existing Staff",
                    email,
                    "123456",
                    "0900000013",
                    artisanRole.getRoleId()
            );
        }

        long countBefore = accountRepository.findAll()
                .stream()
                .filter(user ->
                        email.equalsIgnoreCase(user.getEmail()))
                .count();

        mockMvc.perform(post("/owner/users/create")
                        .with(ownerUser())
                        .with(csrf())
                        .param("fullName", "Duplicate Staff")
                        .param("email", email)
                        .param("password", "123456")
                        .param("phone", "0900000099")
                        .param(
                                "roleId",
                                String.valueOf(artisanRole.getRoleId())
                        ))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users/create"))
                .andExpect(flash()
                        .attribute(
                                "error",
                                "Email da ton tai trong he thong!"
                        ));

        long countAfter = accountRepository.findAll()
                .stream()
                .filter(user ->
                        email.equalsIgnoreCase(user.getEmail()))
                .count();

        assertEquals(countBefore, countAfter);
    }

    @Test
    void tcSysBF05014_cannotCreateStaffWithInvalidRole() throws Exception {

        String email = "invalid.role.bf05@test.com";

        int invalidRoleId = 999999;

        // Make sure test email does not already exist
        accountRepository.findAll()
                .stream()
                .filter(user ->
                        email.equalsIgnoreCase(user.getEmail()))
                .forEach(accountRepository::delete);

        mockMvc.perform(post("/owner/users/create")
                        .with(ownerUser())
                        .with(csrf())
                        .param("fullName", "Invalid Role Staff")
                        .param("email", email)
                        .param("password", "123456")
                        .param("phone", "0900000014")
                        .param(
                                "roleId",
                                String.valueOf(invalidRoleId)
                        ))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users/create"))
                .andExpect(flash()
                        .attribute(
                                "error",
                                "Role khong ton tai!"
                        ));

        assertFalse(
                accountRepository.existsByEmail(email)
        );
    }

    @Test
    void tcSysBF05015_cannotCreateStaffWithShortPassword() throws Exception {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "short.password.bf05@test.com";

        accountRepository.findAll()
                .stream()
                .filter(user ->
                        email.equalsIgnoreCase(user.getEmail()))
                .forEach(accountRepository::delete);

        mockMvc.perform(post("/owner/users/create")
                        .with(ownerUser())
                        .with(csrf())
                        .param("fullName", "Short Password Staff")
                        .param("email", email)
                        .param("password", "12345")
                        .param("phone", "0900000015")
                        .param(
                                "roleId",
                                String.valueOf(artisanRole.getRoleId())
                        ))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users/create"))
                .andExpect(flash()
                        .attribute(
                                "error",
                                "Mật khẩu phải có ít nhất 6 ký tự!"
                        ));

        assertFalse(
                accountRepository.existsByEmail(email)
        );
    }

    @Test
    void tcSysBF05016_cannotLockOwnerAccount() throws Exception {

        Role ownerRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "OWNER".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_OWNER".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("OWNER role not found"));

        String email = "owner.lock.test@bonsai.com";

        User owner = accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    User newOwner = User.builder()
                            .fullName("Owner Lock Test")
                            .email(email)
                            .password(passwordEncoder.encode("123456"))
                            .phone("0900000016")
                            .role(ownerRole)
                            .status("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .build();

                    return accountRepository.save(newOwner);
                });

        owner.setStatus("ACTIVE");
        accountRepository.save(owner);

        mockMvc.perform(post("/owner/users/lock")
                        .with(ownerUser())
                        .with(csrf())
                        .param("userId", String.valueOf(owner.getUserId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash().attribute(
                        "error",
                        "Khong the khoa tai khoan Owner!"
                ));

        User unchangedOwner = accountRepository
                .findById(owner.getUserId())
                .orElseThrow();

        assertEquals("ACTIVE", unchangedOwner.getStatus());
    }

    @Test
    void tcSysBF05017_cannotLockAlreadyLockedStaffAccount() throws Exception {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "already.locked.bf05@test.com";

        User staff = accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseGet(() -> {

                    accountService.createAccount(
                            "Already Locked Staff",
                            email,
                            "123456",
                            "0900000017",
                            artisanRole.getRoleId()
                    );

                    return accountRepository.findAll()
                            .stream()
                            .filter(user ->
                                    email.equalsIgnoreCase(user.getEmail()))
                            .findFirst()
                            .orElseThrow();
                });

        staff.setStatus("LOCKED");
        accountRepository.save(staff);

        mockMvc.perform(post("/owner/users/lock")
                        .with(ownerUser())
                        .with(csrf())
                        .param("userId",
                                String.valueOf(staff.getUserId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash().attribute(
                        "error",
                        "Tai khoan da bi khoa roi!"
                ));

        User unchangedStaff = accountRepository
                .findById(staff.getUserId())
                .orElseThrow();

        assertEquals("LOCKED", unchangedStaff.getStatus());
    }

    @Test
    void tcSysBF05018_cannotUnlockAlreadyActiveAccount() throws Exception {

        Role artisanRole = roleRepository.findAll()
                .stream()
                .filter(role ->
                        "ARTISAN".equalsIgnoreCase(role.getRoleName())
                                || "ROLE_ARTISAN".equalsIgnoreCase(role.getRoleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("ARTISAN role not found"));

        String email = "already.active.bf05@test.com";

        User staff = accountRepository.findAll()
                .stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst()
                .orElseGet(() -> {

                    accountService.createAccount(
                            "Already Active Staff",
                            email,
                            "123456",
                            "0900000018",
                            artisanRole.getRoleId()
                    );

                    return accountRepository.findAll()
                            .stream()
                            .filter(user ->
                                    email.equalsIgnoreCase(user.getEmail()))
                            .findFirst()
                            .orElseThrow();
                });

        staff.setStatus("ACTIVE");
        accountRepository.save(staff);

        mockMvc.perform(post("/owner/users/unlock")
                        .with(ownerUser())
                        .with(csrf())
                        .param("userId",
                                String.valueOf(staff.getUserId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash().attribute(
                        "error",
                        "Tai khoan dang hoat dong roi!"
                ));

        User unchangedStaff = accountRepository
                .findById(staff.getUserId())
                .orElseThrow();

        assertEquals("ACTIVE", unchangedStaff.getStatus());
    }

    @Test
    void tcSysBF05019_cannotLockNonExistingAccount() throws Exception {

        int invalidUserId = 999999;

        while (accountRepository.existsById(invalidUserId)) {
            invalidUserId++;
        }

        mockMvc.perform(post("/owner/users/lock")
                        .with(ownerUser())
                        .with(csrf())
                        .param("userId",
                                String.valueOf(invalidUserId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/users"))
                .andExpect(flash().attribute(
                        "error",
                        "Khong tim thay tai khoan!"
                ));
    }

    @Test
    void tcSysBF05020_nonOwnerCannotManageStaffAccounts() throws Exception {

        mockMvc.perform(get("/owner/users")
                        .with(artisanUser()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/owner/users/lock")
                        .with(artisanUser())
                        .with(csrf())
                        .param("userId", "1"))
                .andExpect(status().isForbidden());
    }

}
