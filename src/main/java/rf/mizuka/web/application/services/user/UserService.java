package rf.mizuka.web.application.services.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rf.mizuka.web.application.controllers.auth.UserExistException;
import rf.mizuka.web.application.database.entities.user.User;
import rf.mizuka.web.application.database.repository.user.UserRepository;

@Service
public class UserService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository)
    {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Transactional
    public void registerUser(String username, String rawPassword)
            throws UserExistException
    {
        Boolean isExist = userRepository.existsByUsername(username);

        if (isExist == null || isExist)
        {
            throw new UserExistException(username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));

        userRepository.save(user);
    }
}