package br.com.fametro.spring.transportadora.exception.handler;

import br.com.fametro.spring.transportadora.exception.custom.ConflitoException;
import br.com.fametro.spring.transportadora.exception.custom.CredenciaisInvalidasException;
import br.com.fametro.spring.transportadora.exception.custom.CredencialExpiradaException;
import br.com.fametro.spring.transportadora.exception.custom.RecursoNaoEncontradoException;
import br.com.fametro.spring.transportadora.exception.custom.RegraNegocioException;
import br.com.fametro.spring.transportadora.exception.dto.CampoErroResponse;
import br.com.fametro.spring.transportadora.exception.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** Converte qualquer exceção lançada pelos controllers/services em {@link ErroResponse}. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- exceções da aplicação (pasta exception/custom)

    /** Requisição bem formada, mas que quebra regra de negócio: 422 (o 400 fica para formato inválido). */
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), req);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> credenciaisInvalidas(CredenciaisInvalidasException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    /** Senha/PIN certo, mas vencido (30 dias): 403 com código para o cliente abrir a tela de troca. */
    @ExceptionHandler(CredencialExpiradaException.class)
    public ResponseEntity<ErroResponse> credencialExpirada(CredencialExpiradaException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErroResponse.de(HttpStatus.FORBIDDEN,
                CredencialExpiradaException.CODIGO, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException ex, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    // ---------- exceções do Spring

    /** @Valid falhou: devolve a lista de campos com a mensagem de cada um. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<CampoErroResponse> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoErroResponse(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErroResponse.de(HttpStatus.BAD_REQUEST, "Dados inválidos.", req.getRequestURI(), campos));
    }

    /** JSON malformado ou valor fora do enum (ex.: vinculo "TERCEIRO"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou com valor não permitido.", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getName() + "' com valor inválido.", req);
    }

    /** ?sort= com campo que não existe na entidade. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroResponse> ordenacaoInvalida(PropertyReferenceException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "Não é possível ordenar por '" + ex.getPropertyName() + "'.", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "Rota não encontrada.", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " não suportado nesta rota.", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, SecurityExceptionHandler.MENSAGEM_403, req);
    }

    /** Qualquer outra coisa: loga o detalhe e devolve mensagem genérica (não vaza stack trace). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.", req);
    }

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErroResponse.de(status, mensagem, req.getRequestURI()));
    }
}
