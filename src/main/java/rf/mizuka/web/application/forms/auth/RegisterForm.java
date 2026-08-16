package rf.mizuka.web.application.forms.auth;

import lombok.Getter;

@Getter
public final class RegisterForm
{
    private String username;
    private String password;
    private String confirmPassword;

    public RegisterForm() {}

    public RegisterForm(String username, String password)
    {
        this.username = username;
        this.password = password;
        this.confirmPassword = password;
    }

    public RegisterForm(String username, String password, String confirmPassword)
    {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public RegisterForm setUsername(String username)
    {
        this.username = username;
        return this;
    }

    public RegisterForm setPassword(String password)
    {
        this.password = password;
        return this;
    }

    public RegisterForm setConfirmPassword(String confirmPassword)
    {
        this.confirmPassword = confirmPassword;
        return this;
    }
}