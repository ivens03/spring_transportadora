package br.com.fametro.spring.transportadora.usuario.config;

import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import br.com.fametro.spring.transportadora.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Na subida, garante que existe pelo menos um ADMINISTRATIVO para começar a cadastrar usuários. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdministradorInicialConfig implements ApplicationRunner {

    private final UsuarioService usuarioService;
    private final RotaCertaProperties props;

    @Override
    public void run(ApplicationArguments args) {
        var admin = props.bootstrap().admin();
        if (admin.senha() == null || admin.senha().isBlank()) {
            log.warn("rotacerta.bootstrap.admin.senha vazio: nenhum administrativo inicial foi criado.");
            return;
        }
        String matricula = usuarioService.garantirAdministradorInicial(admin.nome(), admin.email(), admin.senha());
        if (matricula != null) {
            log.info("Administrativo inicial criado: matrícula {} / e-mail {}", matricula, admin.email());
        }
    }
}
