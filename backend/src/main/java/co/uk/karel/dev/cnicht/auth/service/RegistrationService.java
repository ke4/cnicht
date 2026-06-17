package co.uk.karel.dev.cnicht.auth.service;

import co.uk.karel.dev.cnicht.auth.entity.UserAccount;
import co.uk.karel.dev.cnicht.auth.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserAccountRepository userAccountRepository,
                               PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(String email, String rawPassword) {
        if (userAccountRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        UserAccount account = new UserAccount(email, passwordEncoder.encode(rawPassword));
        return userAccountRepository.save(account);
    }
}
