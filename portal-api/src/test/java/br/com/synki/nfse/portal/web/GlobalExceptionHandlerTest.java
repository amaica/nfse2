package br.com.synki.nfse.portal.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundMapeiaParaHttp404ComMensagem() {
        var res = handler.notFound(new NoSuchElementException("CFOP nao encontrado"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).containsEntry("erro", "CFOP nao encontrado");
    }

    @Test
    void notFoundSemMensagemUsaTextoPadrao() {
        var res = handler.notFound(new NoSuchElementException());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).containsEntry("erro", "Recurso nao encontrado");
    }

    @Test
    void forbiddenMapeiaParaHttp403() {
        var res = handler.forbidden(new AccessDeniedException("Sem permissao"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody()).containsEntry("erro", "Sem permissao");
    }

    @Test
    void badRequestMapeiaParaHttp400() {
        var res = handler.badRequest(new IllegalArgumentException("Dado invalido"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).containsEntry("erro", "Dado invalido");
    }

    @Test
    void conflictMapeiaParaHttp422() {
        var res = handler.conflict(new IllegalStateException("Estado invalido"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).containsEntry("erro", "Estado invalido");
    }

    @Test
    void genericNuncaVazaDetalheInternoAoCliente() {
        var res = handler.generic(new RuntimeException("detalhe sensivel de infraestrutura"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).containsEntry("erro", "Erro interno do servidor");
        assertThat(res.getBody().values()).noneMatch(v -> v.toString().contains("detalhe sensivel"));
    }

    @Test
    void validationJuntaMensagensDeCampo() {
        var ex = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(org.springframework.validation.BindingResult.class);
        var erroCampo = new org.springframework.validation.FieldError("obj", "nome", "Nome obrigatorio");
        org.mockito.Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);
        org.mockito.Mockito.when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(erroCampo));

        var res = handler.validation(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).containsEntry("erro", "Nome obrigatorio");
    }
}
