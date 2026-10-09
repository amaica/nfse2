package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.Certificado;
import br.com.synki.nfse.portal.repository.CertificadoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CertificadoLeituraServiceTest {

    private final CertificadoRepository repository = mock(CertificadoRepository.class);
    private final CertificadoLeituraService service = new CertificadoLeituraService(repository);

    @Test
    void mensagemVencidoIncluiAData() {
        var texto = CertificadoLeituraService.mensagemVencido(LocalDate.of(2026, 8, 27));

        assertThat(texto).isEqualTo(
                "O certificado digital A1 deste emitente venceu em 27/08/2026. "
                        + "A SEFAZ recusa a nota. Cadastre um certificado novo.");
    }

    @Test
    void semCertificadoAvisaParaCadastrar() {
        when(repository.findFirstByEmpresaIdOrderByCreatedAtDesc(122L)).thenReturn(Optional.empty());

        assertThat(service.avisoEmissao(122L)).contains(
                "Certificado A1 não cadastrado. Envie o arquivo .pfx do emitente.");
    }

    @Test
    void arquivoInvalidoNaoViraErroGenerico() {
        when(repository.findFirstByEmpresaIdOrderByCreatedAtDesc(122L))
                .thenReturn(Optional.of(Certificado.of(122L, new byte[] {1, 2, 3}, "senha")));

        assertThatThrownBy(() -> service.exigirValido(122L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Não foi possível abrir o certificado A1. Confira o arquivo e a senha.");
    }
}
