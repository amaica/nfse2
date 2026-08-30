package br.com.synki.nfse.portal.config;

import br.com.synki.nfse.portal.domain.Usuario;
import br.com.synki.nfse.portal.domain.UsuarioEmpresa;
import br.com.synki.nfse.portal.repository.ContaEmpresaRepository;
import br.com.synki.nfse.portal.repository.UsuarioRepository;
import br.com.synki.nfse.portal.service.MembershipService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class DemoDataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final MembershipService membershipService;
    private final ContaEmpresaRepository contaEmpresaRepository;

    public DemoDataInitializer(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            MembershipService membershipService,
            ContaEmpresaRepository contaEmpresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.membershipService = membershipService;
        this.contaEmpresaRepository = contaEmpresaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findByEmailAndAtivoTrue("admin@synki.demo").isEmpty()) {
            var user = usuarioRepository.save(Usuario.create(
                    1L, "Administrador", "admin@synki.demo", passwordEncoder.encode("demo123")));
            contaEmpresaRepository.findByEmpresaId(1L).ifPresent(ce ->
                    membershipService.vincularUsuarioEmpresa(
                            user.getId(), 1L, ce.getContaId(), UsuarioEmpresa.PAPEL_OWNER));
        }
    }
}
