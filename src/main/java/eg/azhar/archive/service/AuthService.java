package eg.azhar.archive.service;

import eg.azhar.archive.dto.UserDto;
import eg.azhar.archive.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public Optional<UserDto> authenticate(String username, String password) {
        return users.findByUsername(username.trim())
                .filter(u -> u.isEnabled() && encoder.matches(password, u.getPasswordHash()))
                .map(u -> new UserDto(u.getUsername(), u.getDisplayName()));
    }
}
