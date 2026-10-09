package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import br.com.synki.nfse.portal.domain.fiscal.Pessoa;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.repository.fiscal.PessoaRepository;
import br.com.synki.nfse.portal.repository.fiscal.ProdutoRepository;
import com.fincatto.documentofiscal.DFUnidadeFederativa;
import com.fincatto.documentofiscal.nfe400.classes.NFEndereco;
import com.fincatto.documentofiscal.nfe400.classes.NFFinalidade;
import com.fincatto.documentofiscal.nfe400.classes.NFIndicadorFormaPagamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFIndicadorPresencaComprador;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFMeioPagamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfo;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoDestinatario;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoEmitente;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoFormaPagamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoIdentificacao;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoInformacoesAdicionais;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItem;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItemProduto;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoPagamento;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFOperacaoConsumidorFinal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NfeCloneServiceTest {

    private final PessoaRepository pessoas = mock(PessoaRepository.class);
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final EmpresaEnderecoRepository enderecos = mock(EmpresaEnderecoRepository.class);
    private final NfeCloneService service = new NfeCloneService(
            mock(NfeEmissaoRepository.class), pessoas, produtos, enderecos);

    @Test
    void copiaDestinatarioItensPagamentoELocalDoEmitente() throws Exception {
        var pessoa = new Pessoa();
        pessoa.setNome("CLOVIS ANTONIO WERLANG");
        pessoa.setCpfCnpj("22659870004");
        var id = Pessoa.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(pessoa, 88L);
        when(pessoas.findFirstByEmpresaIdAndCpfCnpjAndAtivoTrue(122L, "22659870004"))
                .thenReturn(Optional.of(pessoa));
        when(produtos.findByEmpresaIdAndCodigo(122L, "SOJA")).thenReturn(Optional.empty());

        var local = new EmpresaEndereco();
        local.setInscricaoEstadual("2281005172");
        var idEnd = EmpresaEndereco.class.getDeclaredField("id");
        idEnd.setAccessible(true);
        idEnd.set(local, 19L);
        when(enderecos.findByEmpresaIdOrderByPrincipalDescApelidoAsc(122L)).thenReturn(List.of(local));

        var info = new NFNotaInfo();
        var ide = new NFNotaInfoIdentificacao();
        ide.setNaturezaOperacao("VENDA DE MERCADORIA");
        ide.setFinalidade(NFFinalidade.NORMAL);
        ide.setOperacaoConsumidorFinal(NFOperacaoConsumidorFinal.NAO);
        ide.setIndicadorPresencaComprador(NFIndicadorPresencaComprador.OPERACAO_PRESENCIAL);
        info.setIdentificacao(ide);

        var emit = new NFNotaInfoEmitente();
        emit.setInscricaoEstadual("2281005172");
        info.setEmitente(emit);

        var end = new NFEndereco();
        end.setLogradouro("LINHA CRISTAL");
        end.setNumero("100");
        end.setBairro("INTERIOR");
        end.setCodigoMunicipio("4320305");
        end.setDescricaoMunicipio("SELBACH");
        end.setUf(DFUnidadeFederativa.RS);
        end.setCep("99450000");

        var dest = new NFNotaInfoDestinatario();
        dest.setCpf("22659870004");
        dest.setRazaoSocial("NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL");
        dest.setInscricaoEstadual("2281016751");
        dest.setEndereco(end);
        info.setDestinatario(dest);

        var prod = new NFNotaInfoItemProduto();
        prod.setCodigo("SOJA");
        prod.setDescricao("SOJA EM GRAOS");
        prod.setNcm("12019000");
        prod.setCfop("5102");
        prod.setUnidadeComercial("KG");
        prod.setQuantidadeComercial(new BigDecimal("100"));
        prod.setValorUnitario(new BigDecimal("2.50"));
        prod.setValorDesconto(new BigDecimal("10.00"));
        var item = new NFNotaInfoItem();
        item.setProduto(prod);
        info.setItens(List.of(item));

        var forma = new NFNotaInfoFormaPagamento();
        forma.setMeioPagamento(NFMeioPagamento.DINHEIRO);
        forma.setIndicadorFormaPagamento(NFIndicadorFormaPagamento.A_VISTA);
        var pag = new NFNotaInfoPagamento();
        pag.setDetalhamentoFormasPagamento(List.of(forma));
        info.setPagamento(pag);

        var adic = new NFNotaInfoInformacoesAdicionais();
        adic.setInformacoesComplementaresInteresseContribuinte("Clone de teste");
        info.setInformacoesAdicionais(adic);

        var body = service.montar(122L, info, "43100000000000000000000000000000000000000000", "920", 117);

        assertThat(body).containsEntry("origemNumero", 117L);
        assertThat(body).containsEntry("origemSerie", "920");
        assertThat(body).containsEntry("enderecoId", 19L);
        assertThat(body).containsEntry("naturezaOperacao", "VENDA DE MERCADORIA");
        assertThat(body).containsEntry("finalidade", "1");
        assertThat(body).containsEntry("informacoesAdicionais", "Clone de teste");

        @SuppressWarnings("unchecked")
        var destMap = (Map<String, Object>) body.get("destinatario");
        assertThat(destMap).containsEntry("pessoaId", 88L);
        assertThat(destMap).containsEntry("nome", "CLOVIS ANTONIO WERLANG");
        assertThat(destMap).containsEntry("documento", "22659870004");
        assertThat(destMap).containsEntry("municipio", "SELBACH");

        @SuppressWarnings("unchecked")
        var itens = (List<Map<String, Object>>) body.get("itens");
        assertThat(itens).hasSize(1);
        assertThat(itens.getFirst()).containsEntry("codigo", "SOJA");
        assertThat(itens.getFirst()).containsEntry("unidade", "KG");

        @SuppressWarnings("unchecked")
        var pagamento = (Map<String, Object>) body.get("pagamento");
        assertThat(pagamento).containsEntry("meioPagamento", NFMeioPagamento.DINHEIRO.getCodigo());
        assertThat(pagamento).containsEntry("indicadorPagamento", "0");
    }
}
