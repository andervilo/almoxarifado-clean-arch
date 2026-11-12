package com.almoxarifado.shared.infrastructure.web;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.almoxarifado.shared.domain.RecursoNaoEncontradoException;
import com.almoxarifado.shared.domain.RegraNegocioException;

/**
 * Padroniza os erros da API no formato RFC 9457 (Problem Details),
 * acrescentando {@code codigo}, {@code timestamp} e, em validações, {@code erros[]}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String TYPE_BASE = "https://almoxarifado.local/erros/";

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail handleRegraNegocio(RegraNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage(), ex.getCodigo());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), "RECURSO_NAO_ENCONTRADO");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcorrencia(OptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito de concorrência",
            "O recurso foi alterado por outra operação. Tente novamente.", "CONFLITO_CONCORRENCIA");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAcessoNegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado",
            "Você não tem permissão para executar esta operação.", "ACESSO_NEGADO");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleArgumentoInvalido(IllegalArgumentException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage(), "REQUISICAO_INVALIDA");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleEstadoInvalido(IllegalStateException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Operação não permitida", ex.getMessage(), "OPERACAO_NAO_PERMITIDA");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleInesperado(Exception ex) {
        log.error("Erro inesperado ao processar requisição", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
            "Ocorreu um erro inesperado. Contate o suporte.", "ERRO_INTERNO");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> Map.of(
                "campo", erro.getField(),
                "mensagem", erro.getDefaultMessage() == null ? "inválido" : erro.getDefaultMessage()))
            .toList();

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
            "Um ou mais campos possuem valores inválidos.", "VALIDACAO");
        problema.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(problema);
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalhe, String codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setType(URI.create(TYPE_BASE + codigo.toLowerCase().replace('_', '-')));
        problema.setProperty("codigo", codigo);
        problema.setProperty("timestamp", Instant.now());
        return problema;
    }
}
