package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findByEmailAndAtivoTrue("admin@synki.demo").isEmpty()) {
            usuarioRepository.save(Usuario.create(
                    1L, "Administrador", "admin@synki.demo", passwordEncoder.encode("demo123")));
        }
    }
}
