package rf.mizuka.web.application.forms.auth;

import lombok.Getter;

@Getter
public final class LoginForm
{
    private String username;
    private String password;

    public LoginForm() {}

    public LoginForm(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    public LoginForm(String username)
    {
        this.username = username;
    }

    public LoginForm setUsername(String username)
    {
        this.username = username;
        return this;
    }

    public LoginForm setPassword(String password)
    {
        this.password = password;
        return this;
    }
}