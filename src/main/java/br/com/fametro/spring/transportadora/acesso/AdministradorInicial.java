package br.com.fametro.spring.transportadora.acesso;

import br.com.fametro.spring.transportadora.config.RotaCertaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Na subida, garante que existe pelo menos um ADMINISTRATIVO para começar a criar matrículas. */
@Component
class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final MatriculaService matriculaService;
    private final RotaCertaProperties props;

    AdministradorInicial(MatriculaService matriculaService, RotaCertaProperties props) {
        this.matriculaService = matriculaService;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        var admin = props.bootstrap().admin();
        if (admin.senha() == null || admin.senha().isBlank()) {
            log.warn("rotacerta.bootstrap.admin.senha vazio: nenhum administrativo inicial foi criado.");
            return;
        }
        String matricula = matriculaService.garantirAdministradorInicial(admin.nome(), admin.email(), admin.senha());
        if (matricula != null) {
            log.info("Administrativo inicial criado: matrícula {} / e-mail {}", matricula, admin.email());
        }
    }
}
