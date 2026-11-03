package br.com.fametro.spring.transportadora.acesso.web;

import br.com.fametro.spring.transportadora.acesso.dominio.Departamento;
import br.com.fametro.spring.transportadora.acesso.dominio.Vinculo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * vinculo EXTERNO -> entregador (departamento deve ficar vazio; e-mail opcional).
 * vinculo INTERNO -> funcionário (departamento e e-mail obrigatórios).
 */
public record NovaMatriculaRequest(@NotBlank @Size(max = 150) String nome,
                                   @Email @Size(max = 200) String email,
                                   @NotNull Vinculo vinculo,
                                   Departamento departamento) {
}
