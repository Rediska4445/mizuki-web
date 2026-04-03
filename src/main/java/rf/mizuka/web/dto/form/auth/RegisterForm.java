package rf.mizuka.web.dto.form.auth;

public class RegisterForm
{
    protected String username;
    protected String password;
    private String email;
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public RegisterForm setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public RegisterForm setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public RegisterForm setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public RegisterForm setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
        return this;
    }
}