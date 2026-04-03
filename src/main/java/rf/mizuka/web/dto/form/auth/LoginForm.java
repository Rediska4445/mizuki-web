package rf.mizuka.web.dto.form.auth;

import lombok.Data;

public class LoginForm {
    protected String username;
    protected String password;

    public String getUsername() {
        return username;
    }

    public LoginForm setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public LoginForm setPassword(String password) {
        this.password = password;
        return this;
    }
}
