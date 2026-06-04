package projekt.pk.edu.pl.bookstore.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationResponse;
import projekt.pk.edu.pl.bookstore.entity.Role;
import projekt.pk.edu.pl.bookstore.exception.EmailAlreadyUsedException;
import projekt.pk.edu.pl.bookstore.repository.RoleRepository;
import projekt.pk.edu.pl.bookstore.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void shouldRegisterUserWithNormalizedEmailAndReaderRole() {
        RegistrationRequest request = new RegistrationRequest(" Jan ", " Kowalski ", " USER@Example.com ", "superhaslo123");
        Role role = new Role();
        role.setName("READER");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("READER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("superhaslo123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(projekt.pk.edu.pl.bookstore.entity.User.class)))
                .thenAnswer(invocation -> {
                    projekt.pk.edu.pl.bookstore.entity.User user = invocation.getArgument(0);
                    user.setId(5L);
                    return user;
                });

        RegistrationResponse response = registrationService.register(request);

        ArgumentCaptor<projekt.pk.edu.pl.bookstore.entity.User> captor = ArgumentCaptor.forClass(projekt.pk.edu.pl.bookstore.entity.User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        projekt.pk.edu.pl.bookstore.entity.User savedUser = captor.getValue();

        assertThat(savedUser.getFirstName()).isEqualTo("Jan");
        assertThat(savedUser.getLastName()).isEqualTo("Kowalski");
        assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(role);
        assertThat(savedUser.getIsActive()).isTrue();

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo("READER");
    }

    @Test
    void shouldRejectDuplicateEmailBeforeSave() {
        RegistrationRequest request = new RegistrationRequest("Jan", "Kowalski", "user@example.com", "superhaslo123");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(new projekt.pk.edu.pl.bookstore.entity.User()));

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessage("Email is already in use: user@example.com");

        verify(userRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldThrowWhenDefaultRoleIsMissing() {
        RegistrationRequest request = new RegistrationRequest("Jan", "Kowalski", "user@example.com", "superhaslo123");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("READER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Default role READER is missing");
    }

    @Test
    void shouldTranslateDataIntegrityViolationToDuplicateEmail() {
        RegistrationRequest request = new RegistrationRequest("Jan", "Kowalski", "user@example.com", "superhaslo123");
        Role role = new Role();
        role.setName("READER");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("READER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("superhaslo123")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(projekt.pk.edu.pl.bookstore.entity.User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessage("Email is already in use: user@example.com");
    }
}
