package projekt.pk.edu.pl.bookstore.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationResponse;
import projekt.pk.edu.pl.bookstore.entity.Role;
import projekt.pk.edu.pl.bookstore.entity.User;
import projekt.pk.edu.pl.bookstore.exception.EmailAlreadyUsedException;
import projekt.pk.edu.pl.bookstore.repository.RoleRepository;
import projekt.pk.edu.pl.bookstore.repository.UserRepository;

import java.util.Locale;

@Service
public class RegistrationService {

    private static final String DEFAULT_ROLE_NAME = "READER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyUsedException(normalizedEmail);
        }

        Role role = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException("Default role READER is missing"));

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setIsActive(true);

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new RegistrationResponse(
                    savedUser.getId(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    savedUser.getEmail(),
                    role.getName()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyUsedException(normalizedEmail);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
