package co.uk.karel.dev.cnicht.auth.service;

import co.uk.karel.dev.cnicht.auth.entity.UserAccount;
import co.uk.karel.dev.cnicht.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userAccountRepository, passwordEncoder);
    }

    @Test
    void whenEmailDoesNotExists_RegisterUserSuccessfully() {
        String email = "new@example.com";
        String rawPassword = "password123";
        String encodedPassword = "encoded_password";

        when(userAccountRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount result = registrationService.register(email, rawPassword);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getPasswordHash()).isEqualTo(encodedPassword);

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getEmail()).isEqualTo(email);
        assertThat(accountCaptor.getValue().getPasswordHash()).isEqualTo(encodedPassword);
    }

    @Test
    void whenEmailAlreadyExists_ThrowsEmailAlreadyExistsException() {
        String email = "existing@example.com";
        String rawPassword = "password123";

        when(userAccountRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(email, rawPassword))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(email);
    }
}
