package cl.duoc.ep3;

import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.model.UserRole;
import cl.duoc.ep3.repository.AccountRepository;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminAccountInitializer(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.email:}") String email,
            @Value("${admin.password:}") String password) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Configure ADMIN_EMAIL e ADMIN_PASSWORD (mínimo 12 caracteres) juntos.");
        }
        // Crea la cuenta solo si el correo aún no existe.
        if (accountRepository.existsByEmailIgnoreCase(email)) return;
        accountRepository.save(new Account(
                email, passwordEncoder.encode(password), "Administrador", "La", "Taverna",
                "111111111", LocalDate.of(1990, 1, 1), "000000000", "Administración",
                false, UserRole.ADMIN));
    }
}
