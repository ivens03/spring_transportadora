package br.com.fametro.spring.transportadora.usuario.model;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * Dados da empresa cliente (perfil CLIENTE), nas colunas da própria tabela usuario (V6).
 * O endereço é o destino padrão das entregas para essa empresa.
 *
 * @param cnpj     só os 14 caracteres, sem pontuação (pode ter letras: CNPJ alfanumérico)
 * @param telefone só dígitos, com DDD
 * @param cep      8 dígitos
 */
@Embeddable
public record DadosEmpresa(String cnpj, String telefone, String cep, String endereco, String bairro, String cidade,
                           String uf, BigDecimal latitude, BigDecimal longitude) {
}
