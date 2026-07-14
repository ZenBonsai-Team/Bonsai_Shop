// Toggle hiện/ẩn mật khẩu
const togglePassword = document.getElementById('togglePassword');
const passwordInput = document.getElementById('password');

togglePassword.addEventListener('click', function () {
    const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
    passwordInput.setAttribute('type', type);
    this.textContent = type === 'password' ? '👁️' : '🙈';
});

// Hiệu ứng focus input
document.querySelectorAll('.form-group input').forEach(input => {
    input.addEventListener('focus', function () {
        this.parentElement.style.transform = 'scale(1.01)';
        this.parentElement.style.transition = 'transform 0.2s';
    });
    input.addEventListener('blur', function () {
        this.parentElement.style.transform = 'scale(1)';
    });
});

// Hiệu ứng nút login
const btnLogin = document.querySelector('.btn-signin');
if (btnLogin) {
    btnLogin.addEventListener('click', function() {
        this.textContent = 'Đang đăng nhập...';
        setTimeout(() => { this.textContent = 'Sign in →'; }, 2000);
    });
}