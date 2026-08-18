package com.example.bonsai_shop.customer.service;

import com.example.bonsai_shop.data.dto.CloudinaryUploadResponse;
import com.example.bonsai_shop.entity.PasswordResetOtp;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import com.example.bonsai_shop.customer.repository.RegisterOtpRepository;
import com.example.bonsai_shop.customer.repository.UserRepository;
import com.example.bonsai_shop.data.common.CloudinaryFolder;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.entity.Role;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.customer.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private EmailService emailService;

        @Mock
        private RegisterOtpRepository otpRepository;

        @Spy
        @InjectMocks
        private UserService userService;

        @Mock
        private CloudinaryStorageService cloudinaryStorageService;

        @Test
        void register_WhenInformationIsValid_ShouldCreatePendingUser() {

                // ================= ARRANGE =================

                String fullName = "Nguyen Van A";
                String username = "nguyenvana";
                String email = "test@gmail.com";
                String rawPassword = "123456";
                String phone = "0912345678";

                Role customerRole = new Role();
                customerRole.setRoleName("ROLE_CUSTOMER");

                when(userRepository.existsByEmail(email))
                                .thenReturn(false);

                when(userRepository.existsByUsername(username))
                                .thenReturn(false);

                when(roleRepository.findByRoleName("ROLE_CUSTOMER"))
                                .thenReturn(Optional.of(customerRole));

                when(passwordEncoder.encode(rawPassword))
                                .thenReturn("encoded-password");

                // Không chạy logic thật của sendOtp()
                doNothing()
                                .when(userService)
                                .sendOtp(email);

                // ================= ACT =================

                userService.register(
                                fullName,
                                username,
                                email,
                                rawPassword,
                                phone);

                // ================= ASSERT =================

                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

                verify(userRepository, times(1))
                                .save(userCaptor.capture());

                User savedUser = userCaptor.getValue();

                assertEquals(
                                fullName,
                                savedUser.getFullName());

                assertEquals(
                                username,
                                savedUser.getUsername());

                assertEquals(
                                email,
                                savedUser.getEmail());

                assertEquals(
                                phone,
                                savedUser.getPhone());

                assertEquals(
                                "encoded-password",
                                savedUser.getPassword());

                assertEquals(
                                "PENDING",
                                savedUser.getStatus());

                assertEquals(
                                customerRole,
                                savedUser.getRole());

                verify(passwordEncoder, times(1))
                                .encode(rawPassword);

                verify(userService, times(1))
                                .sendOtp(email);
        }

        @Test
        void register_WhenEmailAlreadyExists_ShouldThrowException() {

                // Arrange
                String email = "existing@gmail.com";

                when(userRepository.existsByEmail(email))
                                .thenReturn(true);

                // Act
                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> userService.register(
                                                "Nguyen Van A",
                                                "nguyenvana",
                                                email,
                                                "123456",
                                                "0912345678"));

                // Assert
                assertEquals(
                                "Email đã được sử dụng!",
                                exception.getMessage());

                verify(userRepository, never())
                                .save(any(User.class));

                verify(passwordEncoder, never())
                                .encode(anyString());

                verify(userService, never())
                                .sendOtp(anyString());
        }

        @Test
        void register_WhenUsernameAlreadyExists_ShouldThrowException() {

                // ================= ARRANGE =================

                String fullName = "Nguyen Van A";
                String username = "existinguser";
                String email = "test@gmail.com";
                String password = "123456";
                String phone = "0912345678";

                // Email chưa tồn tại
                when(userRepository.existsByEmail(email))
                                .thenReturn(false);

                // Username đã tồn tại
                when(userRepository.existsByUsername(username))
                                .thenReturn(true);

                // ================= ACT =================

                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> userService.register(
                                                fullName,
                                                username,
                                                email,
                                                password,
                                                phone));

                // ================= ASSERT =================

                assertEquals(
                                "Tên đăng nhập đã được sử dụng!",
                                exception.getMessage());

                verify(userRepository, never())
                                .save(any(User.class));

                verify(passwordEncoder, never())
                                .encode(anyString());

                verify(roleRepository, never())
                                .findByRoleName(anyString());

                verify(userService, never())
                                .sendOtp(anyString());
        }

        @Test
        void register_WhenValid_ShouldEncryptPasswordBeforeStorage() {

                // ================= ARRANGE =================

                String fullName = "Nguyen Van A";
                String username = "nguyenvana";
                String email = "test@gmail.com";
                String rawPassword = "123456";
                String phone = "0912345678";

                Role customerRole = new Role();
                customerRole.setRoleName("ROLE_CUSTOMER");

                when(userRepository.existsByEmail(email))
                                .thenReturn(false);

                when(userRepository.existsByUsername(username))
                                .thenReturn(false);

                when(roleRepository.findByRoleName("ROLE_CUSTOMER"))
                                .thenReturn(Optional.of(customerRole));

                when(passwordEncoder.encode(rawPassword))
                                .thenReturn("encoded-password");

                doNothing()
                                .when(userService)
                                .sendOtp(email);

                // ================= ACT =================

                userService.register(
                                fullName,
                                username,
                                email,
                                rawPassword,
                                phone);

                // ================= ASSERT =================

                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

                verify(userRepository)
                                .save(captor.capture());

                User savedUser = captor.getValue();

                assertEquals(
                                "encoded-password",
                                savedUser.getPassword());

                assertNotEquals(
                                rawPassword,
                                savedUser.getPassword());

                verify(passwordEncoder, times(1))
                                .encode(rawPassword);
        }

        @Test
        void register_WhenSuccessful_ShouldSetInitialStatusPending() {

                // Arrange
                String email = "test@gmail.com";

                Role customerRole = new Role();
                customerRole.setRoleName("ROLE_CUSTOMER");

                when(userRepository.existsByEmail(email))
                                .thenReturn(false);

                when(userRepository.existsByUsername("nguyenvana"))
                                .thenReturn(false);

                when(roleRepository.findByRoleName("ROLE_CUSTOMER"))
                                .thenReturn(Optional.of(customerRole));

                when(passwordEncoder.encode("123456"))
                                .thenReturn("encoded-password");

                doNothing()
                                .when(userService)
                                .sendOtp(email);

                // Act
                userService.register(
                                "Nguyen Van A",
                                "nguyenvana",
                                email,
                                "123456",
                                "0912345678");

                // Assert
                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

                verify(userRepository)
                                .save(captor.capture());

                assertEquals(
                                "PENDING",
                                captor.getValue().getStatus());
        }

        @Test
        void register_WhenSuccessful_ShouldAssignCustomerRole() {

                // Arrange
                String email = "test@gmail.com";

                Role customerRole = new Role();
                customerRole.setRoleName("ROLE_CUSTOMER");

                when(userRepository.existsByEmail(email))
                                .thenReturn(false);

                when(userRepository.existsByUsername("nguyenvana"))
                                .thenReturn(false);

                when(roleRepository.findByRoleName("ROLE_CUSTOMER"))
                                .thenReturn(Optional.of(customerRole));

                when(passwordEncoder.encode("123456"))
                                .thenReturn("encoded-password");

                doNothing()
                                .when(userService)
                                .sendOtp(email);

                // Act
                userService.register(
                                "Nguyen Van A",
                                "nguyenvana",
                                email,
                                "123456",
                                "0912345678");

                // Assert
                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

                verify(userRepository)
                                .save(captor.capture());

                User savedUser = captor.getValue();

                assertEquals(
                                customerRole,
                                savedUser.getRole());

                assertEquals(
                                "ROLE_CUSTOMER",
                                savedUser.getRole().getRoleName());
        }

        @Test
        void register_WhenSuccessful_ShouldSendOtp() {

                // Arrange
                String email = "test@gmail.com";

                Role customerRole = new Role();
                customerRole.setRoleName("ROLE_CUSTOMER");

                when(userRepository.existsByEmail(email))
                                .thenReturn(false);

                when(userRepository.existsByUsername("nguyenvana"))
                                .thenReturn(false);

                when(roleRepository.findByRoleName("ROLE_CUSTOMER"))
                                .thenReturn(Optional.of(customerRole));

                when(passwordEncoder.encode("123456"))
                                .thenReturn("encoded-password");

                doNothing()
                                .when(userService)
                                .sendOtp(email);

                // Act
                userService.register(
                                "Nguyen Van A",
                                "nguyenvana",
                                email,
                                "123456",
                                "0912345678");

                // Assert
                verify(userService, times(1))
                                .sendOtp(email);
        }

        @Test
        void activateUser_WhenUserExists_ShouldSetStatusActive() {

                // ================= ARRANGE =================

                String email = "test@gmail.com";

                User user = new User();
                user.setEmail(email);
                user.setStatus("PENDING");

                when(userRepository.findByEmail(email))
                                .thenReturn(Optional.of(user));

                // ================= ACT =================

                userService.activateUser(email);

                // ================= ASSERT =================

                assertEquals(
                                "ACTIVE",
                                user.getStatus());

                verify(userRepository, times(1))
                                .findByEmail(email);

                verify(userRepository, times(1))
                                .save(user);
        }

        @Test
        void findByEmail_WhenEmailExists_ShouldReturnUser() {

                // ================= ARRANGE =================

                String email = "test@gmail.com";

                User expectedUser = new User();
                expectedUser.setEmail(email);
                expectedUser.setFullName("Nguyen Van A");

                when(userRepository.findByEmail(email))
                                .thenReturn(Optional.of(expectedUser));

                // ================= ACT =================

                User actualUser = userService.findByEmail(email);

                // ================= ASSERT =================

                assertNotNull(actualUser);

                assertEquals(
                                expectedUser,
                                actualUser);

                assertEquals(
                                email,
                                actualUser.getEmail());

                verify(userRepository, times(1))
                                .findByEmail(email);
        }

        @Test
        void getCurrentUserProfile_WhenUserExists_ShouldReturnProfile() {

                // ================= ARRANGE =================

                String email = "test@gmail.com";

                User user = new User();
                user.setEmail(email);
                user.setFullName("Nguyen Van A");
                user.setUsername("nguyenvana");
                user.setPhone("0912345678");

                when(userRepository.findByEmail(email))
                                .thenReturn(Optional.of(user));

                // ================= ACT =================

                User result = userService.getCurrentUserProfile(email);

                // ================= ASSERT =================

                assertNotNull(result);

                assertEquals(
                                email,
                                result.getEmail());

                assertEquals(
                                "Nguyen Van A",
                                result.getFullName());

                assertEquals(
                                "nguyenvana",
                                result.getUsername());

                assertEquals(
                                "0912345678",
                                result.getPhone());

                verify(userRepository, times(1))
                                .findByEmail(email);
        }

        @Test
        void getCurrentUserProfile_WhenUserDoesNotExist_ShouldThrowException() {

                // ================= ARRANGE =================

                String email = "notfound@gmail.com";

                when(userRepository.findByEmail(email))
                                .thenReturn(Optional.empty());

                // ================= ACT =================

                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> userService.getCurrentUserProfile(email));

                // ================= ASSERT =================

                assertEquals(
                                "Không tìm thấy user!",
                                exception.getMessage());

                verify(userRepository, times(1))
                                .findByEmail(email);
        }

        @Test
        void updateUserProfile_WhenValidInformationWithoutAvatar_ShouldUpdateProfile() {

                // ================= ARRANGE =================

                String email = "test@gmail.com";

                User user = new User();
                user.setEmail(email);
                user.setFullName("Old Name");
                user.setUsername("olduser");
                user.setPhone("0900000000");
                user.setAddress("Old Address");

                when(userRepository.findByEmail(email))
                                .thenReturn(Optional.of(user));

                // ================= ACT =================

                userService.updateUserProfile(
                                email,
                                "New Name",
                                "newuser",
                                "0912345678",
                                "New Address",
                                null);

                // ================= ASSERT =================

                assertEquals("New Name", user.getFullName());
                assertEquals("newuser", user.getUsername());
                assertEquals("0912345678", user.getPhone());
                assertEquals("New Address", user.getAddress());

                verify(userRepository, times(1))
                                .save(user);

                verify(cloudinaryStorageService, never())
                                .uploadImage(
                                                any(MultipartFile.class),
                                                any(CloudinaryFolder.class));

                verify(cloudinaryStorageService, never())
                                .deleteFile(anyString(), anyString());
        }

        @Test
        void updateUserProfile_WhenSomeFieldsAreNull_ShouldRetainOldValues() {

                // Arrange
                String email = "test@gmail.com";

                User user = new User();
                user.setEmail(email);
                user.setFullName("Old Name");
                user.setUsername("olduser");
                user.setPhone("0900000000");
                user.setAddress("Old Address");

                when(userRepository.findByEmail(email))
                                .thenReturn(Optional.of(user));

                // Act
                userService.updateUserProfile(
                                email,
                                "New Name",
                                null,
                                null,
                                "New Address",
                                null);

                // Assert
                assertEquals("New Name", user.getFullName());
                assertEquals("olduser", user.getUsername());
                assertEquals("0900000000", user.getPhone());
                assertEquals("New Address", user.getAddress());

                verify(userRepository, times(1))
                                .save(user);

                verifyNoInteractions(cloudinaryStorageService);
        }

        @Test
        void updateUserProfile_WhenUploadingInitialAvatar_ShouldSaveNewUrlAndPublicId() {

                // Arrange
                String email = "test@gmail.com";

                User user = new User();
                user.setEmail(email);
                user.setAvatar(null);
                user.setAvatarPublicId(null);

                MultipartFile avatarFile = mock(MultipartFile.class);

                CloudinaryUploadResponse uploadResponse = mock(CloudinaryUploadResponse.class);

                when(userRepository.findByEmail(email))
                                .thenReturn(Optional.of(user));

                when(avatarFile.isEmpty())
                                .thenReturn(false);

                when(cloudinaryStorageService.uploadImage(
                                avatarFile,
                                CloudinaryFolder.AVATAR)).thenReturn(uploadResponse);

                when(uploadResponse.getUrl())
                                .thenReturn("https://cloudinary.com/new-avatar.jpg");

                when(uploadResponse.getPublicId())
                                .thenReturn("avatar/new-id");

                // Act
                userService.updateUserProfile(
                                email,
                                null,
                                null,
                                null,
                                null,
                                avatarFile);

                // Assert
                assertEquals(
                                "https://cloudinary.com/new-avatar.jpg",
                                user.getAvatar());

                assertEquals(
                                "avatar/new-id",
                                user.getAvatarPublicId());

                verify(cloudinaryStorageService, times(1))
                                .uploadImage(
                                                avatarFile,
                                                CloudinaryFolder.AVATAR);

                verify(userRepository, times(1))
                                .save(user);
        }

        @Test
        void updateUserProfile_WhenReplacingAvatar_ShouldDeleteOldAvatar() {

                // Arrange
                String email = "test@gmail.com";

                User user = new User();
                user.setEmail(email);
                user.setAvatar("https://cloudinary.com/old.jpg");
                user.setAvatarPublicId("avatar/old-id");

                MultipartFile avatarFile = mock(MultipartFile.class);

                CloudinaryUploadResponse uploadResponse = mock(CloudinaryUploadResponse.class);

                when(userRepository.findByEmail(email))
                                .thenReturn(Optional.of(user));

                when(avatarFile.isEmpty())
                                .thenReturn(false);

                when(cloudinaryStorageService.uploadImage(
                                avatarFile,
                                CloudinaryFolder.AVATAR)).thenReturn(uploadResponse);

                when(uploadResponse.getUrl())
                                .thenReturn("https://cloudinary.com/new.jpg");

                when(uploadResponse.getPublicId())
                                .thenReturn("avatar/new-id");

                // Act
                userService.updateUserProfile(
                                email,
                                null,
                                null,
                                null,
                                null,
                                avatarFile);

                // Assert
                assertEquals(
                                "https://cloudinary.com/new.jpg",
                                user.getAvatar());

                assertEquals(
                                "avatar/new-id",
                                user.getAvatarPublicId());

                verify(cloudinaryStorageService)
                                .deleteFile(
                                                "avatar/old-id",
                                                "image");

                verify(userRepository)
                                .save(user);
        }

        @Test
        void sendOtp_WhenCalled_ShouldGenerateSaveAndSendOtp() {

                // Arrange
                String email = "test@gmail.com";

                // Act
                userService.sendOtp(email);

                // Assert
                verify(otpRepository, times(1))
                                .deleteByEmail(email);

                ArgumentCaptor<PasswordResetOtp> otpCaptor = ArgumentCaptor.forClass(PasswordResetOtp.class);

                verify(otpRepository, times(1))
                                .save(otpCaptor.capture());

                PasswordResetOtp savedOtp = otpCaptor.getValue();

                assertEquals(email, savedOtp.getEmail());

                assertFalse(savedOtp.getIsUsed());

                assertNotNull(savedOtp.getOtpCode());

                verify(emailService, times(1))
                                .sendOtpEmail(
                                                email,
                                                savedOtp.getOtpCode());
        }

        @Test
        void sendOtp_WhenOtpIsGenerated_ShouldHaveSixDigitFormat() {

                // ================= ARRANGE =================

                String email = "test@gmail.com";

                ArgumentCaptor<PasswordResetOtp> otpCaptor = ArgumentCaptor.forClass(PasswordResetOtp.class);

                // ================= ACT =================

                userService.sendOtp(email);

                // ================= ASSERT =================

                verify(otpRepository, times(1))
                                .save(otpCaptor.capture());

                PasswordResetOtp savedOtp = otpCaptor.getValue();

                String otpCode = savedOtp.getOtpCode();

                assertNotNull(otpCode);

                assertTrue(
                                otpCode.matches("\\d{6}"));
        }

        @Test
        void sendOtp_WhenOtpIsGenerated_ShouldBeValidForFiveMinutes() {

                // ================= ARRANGE =================

                String email = "test@gmail.com";

                ArgumentCaptor<PasswordResetOtp> otpCaptor = ArgumentCaptor.forClass(PasswordResetOtp.class);

                // ================= ACT =================

                userService.sendOtp(email);

                // ================= ASSERT =================

                verify(otpRepository, times(1))
                                .save(otpCaptor.capture());

                PasswordResetOtp savedOtp = otpCaptor.getValue();

                assertNotNull(savedOtp.getCreatedAt());
                assertNotNull(savedOtp.getExpiredAt());

                long seconds = java.time.Duration.between(
                                savedOtp.getCreatedAt(),
                                savedOtp.getExpiredAt()).getSeconds();

                assertTrue(
                                seconds >= 299 && seconds <= 300);
        }

        @Test
        void sendOtp_WhenOtpSaveFails_ShouldNotSendEmail() {

                // ================= ARRANGE =================

                String email = "test@gmail.com";

                when(
                                otpRepository.save(
                                                any(PasswordResetOtp.class)))
                                .thenThrow(
                                                new RuntimeException("Database error"));

                // ================= ACT =================

                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> userService.sendOtp(email));

                // ================= ASSERT =================

                assertEquals(
                                "Database error",
                                exception.getMessage());

                verify(otpRepository, times(1))
                                .deleteByEmail(email);

                verify(otpRepository, times(1))
                                .save(any(PasswordResetOtp.class));

                verify(emailService, never())
                                .sendOtpEmail(
                                                anyString(),
                                                anyString());
        }

        @Test
        void verifyOtp_WhenOtpIsValid_ShouldMarkAsUsed() {

                // ================= ARRANGE =================

                String email = "test@gmail.com";
                String otpCode = "123456";

                PasswordResetOtp otp = PasswordResetOtp.builder()
                                .email(email)
                                .otpCode(otpCode)
                                .isUsed(false)
                                .createdAt(LocalDateTime.now())
                                .expiredAt(LocalDateTime.now().plusMinutes(5))
                                .build();

                when(
                                otpRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(
                                                Optional.of(otp));

                // ================= ACT =================

                userService.verifyOtp(
                                email,
                                otpCode);

                // ================= ASSERT =================

                assertTrue(
                                otp.getIsUsed());

                verify(otpRepository, times(1))
                                .findTopByEmailOrderByCreatedAtDesc(email);

                verify(otpRepository, times(1))
                                .save(otp);
        }

        @Test
        void verifyOtp_WhenOtpDoesNotExist_ShouldThrowException() {

                // ================= ARRANGE =================

                String email = "test@gmail.com";
                String otpCode = "123456";

                when(
                                otpRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(
                                                Optional.empty());

                // ================= ACT =================

                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> userService.verifyOtp(
                                                email,
                                                otpCode));

                // ================= ASSERT =================

                assertEquals(
                                "OTP không tồn tại!",
                                exception.getMessage());

                verify(otpRepository, times(1))
                                .findTopByEmailOrderByCreatedAtDesc(email);

                verify(otpRepository, never())
                                .save(any(PasswordResetOtp.class));
        }

}