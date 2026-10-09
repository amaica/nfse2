package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import br.com.synki.nfse.portal.domain.fiscal.Pessoa;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.repository.NfeEmissaoRepository;
import br.com.synki.nfse.portal.repository.fiscal.PessoaRepository;
import br.com.synki.nfse.portal.repository.fiscal.ProdutoRepository;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFInfoReferenciada;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNota;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfo;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoDestinatario;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaInfoItem;
import com.fincatto.documentofiscal.nfe400.classes.nota.NFNotaProcessada;
import com.fincatto.documentofiscal.nfe400.classes.NFEndereco;
import com.fincatto.documentofiscal.utils.DFPersister;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NfeCloneService {

    private final NfeEmissaoRepository emissaoRepository;
    private final PessoaRepository pessoaRepository;
    private final ProdutoRepository produtoRepository;
    private final EmpresaEnderecoRepository enderecoRepository;

    public NfeCloneService(
            NfeEmissaoRepository emissaoRepository,
            PessoaRepository pessoaRepository,
            ProdutoRepository produtoRepository,
            EmpresaEnderecoRepository enderecoRepository) {
        this.emissaoRepository = emissaoRepository;
        this.pessoaRepository = pessoaRepository;
        this.produtoRepository = produtoRepository;
        this.enderecoRepository = enderecoRepository;
    }

    public Map<String, Object> clonar(Long empresaId, String chave) {
        var chaveNorm = chave == null ? "" : chave.replace("NFe", "").replaceAll("\\D", "");
        var nota = emissaoRepository.findFirstByEmpresaIdAndChaveOrderByCreatedAtDesc(empresaId, chaveNorm)
                .orElseThrow(() -> new IllegalArgumentException("NF-e não encontrada"));
        if (nota.getXmlProc() == null || nota.getXmlProc().isBlank()) {
            throw new IllegalStateException("Esta nota não tem XML para clonar.");
        }
        var info = lerInfo(nota.getXmlProc());
        return montar(empresaId, info, chaveNorm, nota.getSerie(), nota.getNumero());
    }

    Map<String, Object> montar(Long empresaId, NFNotaInfo info, String chave, String serie, long numero) {
        var body = new LinkedHashMap<String, Object>();
        body.put("origemChave", chave);
        body.put("origemSerie", serie);
        body.put("origemNumero", numero);

        var ide = info.getIdentificacao();
        if (ide != null) {
            put(body, "naturezaOperacao", ide.getNaturezaOperacao());
            if (ide.getFinalidade() != null) {
                body.put("finalidade", ide.getFinalidade().getCodigo());
            }
            if (ide.getOperacaoConsumidorFinal() != null) {
                body.put("consumidorFinal", ide.getOperacaoConsumidorFinal().getCodigo());
            }
            if (ide.getIndicadorPresencaComprador() != null) {
                body.put("indicadorPresenca", ide.getIndicadorPresencaComprador().getCodigo());
            }
            body.put("referencias", referencias(ide.getReferenciadas()));
        }

        var emitente = info.getEmitente();
        if (emitente != null) {
            enderecoPorIe(empresaId, emitente.getInscricaoEstadual()).ifPresent(id -> body.put("enderecoId", id));
        }

        var dest = destinatario(empresaId, info.getDestinatario());
        if (!dest.isEmpty()) {
            body.put("destinatario", dest);
        }
        body.put("itens", itens(empresaId, info.getItens()));

        var pag = info.getPagamento();
        if (pag != null && pag.getDetalhamentoFormasPagamento() != null && !pag.getDetalhamentoFormasPagamento().isEmpty()) {
            var forma = pag.getDetalhamentoFormasPagamento().getFirst();
            var pagamento = new LinkedHashMap<String, Object>();
            if (forma.getMeioPagamento() != null) {
                pagamento.put("meioPagamento", forma.getMeioPagamento().getCodigo());
            }
            if (forma.getIndicadorFormaPagamento() != null) {
                pagamento.put("indicadorPagamento", forma.getIndicadorFormaPagamento().getCodigo());
            }
            if (!pagamento.isEmpty()) {
                body.put("pagamento", pagamento);
            }
        }

        var transporte = transporte(info);
        if (!transporte.isEmpty()) {
            body.put("transporte", transporte);
        }

        var adic = info.getInformacoesAdicionais();
        if (adic != null) {
            put(body, "informacoesAdicionais", adic.getInformacoesComplementaresInteresseContribuinte());
        }
        return body;
    }

    private NFNotaInfo lerInfo(String xml) {
        try {
            if (xml.contains("nfeProc")) {
                return new DFPersister().read(NFNotaProcessada.class, xml).getNota().getInfo();
            }
            return new DFPersister().read(NFNota.class, xml).getInfo();
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível ler o XML desta nota para clonar.");
        }
    }

    private java.util.Optional<Long> enderecoPorIe(Long empresaId, String ie) {
        var alvo = soDigitos(ie);
        if (alvo.isBlank()) {
            return java.util.Optional.empty();
        }
        return enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId).stream()
                .filter(e -> alvo.equals(soDigitos(e.getInscricaoEstadual())))
                .map(EmpresaEndereco::getId)
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    private Map<String, Object> destinatario(Long empresaId, NFNotaInfoDestinatario dest) {
        var m = new LinkedHashMap<String, Object>();
        if (dest == null) {
            return m;
        }
        var doc = dest.getCnpj() != null && !dest.getCnpj().isBlank() ? dest.getCnpj() : dest.getCpf();
        doc = soDigitos(doc);
        put(m, "documento", doc);
        put(m, "email", dest.getEmail());
        put(m, "inscricaoEstadual", dest.getInscricaoEstadual());
        Pessoa pessoa = doc.isBlank()
                ? null
                : pessoaRepository.findFirstByEmpresaIdAndCpfCnpjAndAtivoTrue(empresaId, doc).orElse(null);
        if (pessoa != null) {
            if (pessoa.getId() != null) {
                m.put("pessoaId", pessoa.getId());
            }
            put(m, "nome", pessoa.getNome());
        } else {
            put(m, "nome", dest.getRazaoSocial());
        }
        endereco(m, dest.getEndereco());
        return m;
    }

    private void endereco(Map<String, Object> m, NFEndereco end) {
        if (end == null) {
            return;
        }
        put(m, "logradouro", end.getLogradouro());
        put(m, "numero", end.getNumero());
        put(m, "complemento", end.getComplemento());
        put(m, "bairro", end.getBairro());
        put(m, "municipio", end.getDescricaoMunicipio());
        put(m, "uf", end.getUf());
        put(m, "cep", soDigitos(end.getCep()));
        put(m, "codigoMunicipioIbge", soDigitos(end.getCodigoMunicipio()));
    }

    private List<Map<String, Object>> itens(Long empresaId, List<NFNotaInfoItem> itens) {
        var out = new ArrayList<Map<String, Object>>();
        if (itens == null) {
            return out;
        }
        for (var item : itens) {
            var prod = item.getProduto();
            if (prod == null) {
                continue;
            }
            var m = new LinkedHashMap<String, Object>();
            put(m, "codigo", prod.getCodigo());
            put(m, "descricao", prod.getDescricao());
            put(m, "ncm", prod.getNcm());
            put(m, "cfop", prod.getCfop());
            put(m, "unidade", prod.getUnidadeComercial());
            putNum(m, "quantidade", prod.getQuantidadeComercial());
            putNum(m, "valorUnitario", prod.getValorUnitario());
            putNum(m, "valorDesconto", prod.getValorDesconto());
            if (prod.getCodigo() != null && !prod.getCodigo().isBlank()) {
                produtoRepository.findByEmpresaIdAndCodigo(empresaId, prod.getCodigo().trim())
                        .filter(p -> p.getId() != null)
                        .ifPresent(p -> m.put("produtoId", p.getId()));
            }
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> transporte(NFNotaInfo info) {
        var m = new LinkedHashMap<String, Object>();
        var transp = info.getTransporte();
        if (transp != null) {
            if (transp.getModalidadeFrete() != null) {
                m.put("modalidadeFrete", transp.getModalidadeFrete().getCodigo());
            }
            var t = transp.getTransportador();
            if (t != null) {
                put(m, "transportadorNome", t.getRazaoSocial());
                var doc = t.getCnpj() != null && !t.getCnpj().isBlank() ? t.getCnpj() : t.getCpf();
                put(m, "transportadorDocumento", soDigitos(doc));
                put(m, "transportadorIe", t.getInscricaoEstadual());
                put(m, "transportadorMunicipio", t.getNomeMunicipio());
                put(m, "transportadorUf", t.getUf());
            }
            var veiculo = transp.getVeiculo();
            if (veiculo != null) {
                put(m, "placa", veiculo.getPlacaVeiculo());
                put(m, "placaUf", veiculo.getUf());
                put(m, "rntc", veiculo.getRegistroNacionalTransportadorCarga());
            }
            if (transp.getReboques() != null && !transp.getReboques().isEmpty()) {
                var reboques = new ArrayList<Map<String, Object>>();
                for (var r : transp.getReboques()) {
                    var row = new LinkedHashMap<String, Object>();
                    put(row, "placa", r.getPlacaVeiculo());
                    put(row, "uf", r.getUf());
                    put(row, "rntc", r.getRegistroNacionalTransportadorCarga());
                    if (!row.isEmpty()) {
                        reboques.add(row);
                    }
                }
                if (!reboques.isEmpty()) {
                    m.put("reboques", reboques);
                }
            }
            if (transp.getVolumes() != null && !transp.getVolumes().isEmpty()) {
                var volumes = new ArrayList<Map<String, Object>>();
                for (var v : transp.getVolumes()) {
                    var row = new LinkedHashMap<String, Object>();
                    if (v.getQuantidadeVolumesTransportados() != null) {
                        row.put("quantidade", v.getQuantidadeVolumesTransportados().intValue());
                    }
                    put(row, "especie", v.getEspecieVolumesTransportados());
                    put(row, "marca", v.getMarca());
                    put(row, "numeracao", v.getNumeracaoVolumesTransportados());
                    putNum(row, "pesoLiquido", v.getPesoLiquido());
                    putNum(row, "pesoBruto", v.getPesoBruto());
                    if (!row.isEmpty()) {
                        volumes.add(row);
                    }
                }
                if (!volumes.isEmpty()) {
                    m.put("volumes", volumes);
                }
            }
        }
        if (info.getTotal() != null && info.getTotal().getIcmsTotal() != null) {
            putNum(m, "valorFrete", info.getTotal().getIcmsTotal().getValorTotalFrete());
        }
        return m;
    }

    private List<Map<String, Object>> referencias(List<NFInfoReferenciada> refs) {
        var out = new ArrayList<Map<String, Object>>();
        if (refs == null) {
            return out;
        }
        for (var ref : refs) {
            var m = new LinkedHashMap<String, Object>();
            if (ref.getChaveAcesso() != null && !ref.getChaveAcesso().isBlank()) {
                m.put("tipo", "NFE");
                m.put("chave", soDigitos(ref.getChaveAcesso()));
            } else if (ref.getChaveAcessoCTReferenciada() != null && !ref.getChaveAcessoCTReferenciada().isBlank()) {
                m.put("tipo", "CTE");
                m.put("chave", soDigitos(ref.getChaveAcessoCTReferenciada()));
            } else if (ref.getInfoNFProdutorRuralReferenciada() != null) {
                var nfp = ref.getInfoNFProdutorRuralReferenciada();
                m.put("tipo", "NFP");
                if (nfp.getUfEmitente() != null) {
                    put(m, "codigoUf", nfp.getUfEmitente().getCodigoIbge());
                }
                put(m, "anoMes", nfp.getAnoMesEmissao());
                put(m, "cnpj", nfp.getCnpjEmitente());
                put(m, "cpf", nfp.getCpfEmitente());
                put(m, "inscricaoEstadual", nfp.getIeEmitente());
                put(m, "modelo", nfp.getModeloDocumentoFiscal());
                if (nfp.getSerieDocumentoFiscal() != null) {
                    m.put("serie", String.valueOf(nfp.getSerieDocumentoFiscal()));
                }
                if (nfp.getNumeroDocumentoFiscal() != null) {
                    m.put("numero", String.valueOf(nfp.getNumeroDocumentoFiscal()));
                }
            }
            if (!m.isEmpty()) {
                out.add(m);
            }
        }
        return out;
    }

    private static void put(Map<String, Object> m, String key, String value) {
        if (value != null && !value.isBlank()) {
            m.put(key, value.trim());
        }
    }

    private static void putNum(Map<String, Object> m, String key, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            var n = new BigDecimal(raw.trim());
            if (n.signum() > 0) {
                m.put(key, n);
            }
        } catch (NumberFormatException ignored) {
            // campo opcional do XML
        }
    }

    private static String soDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
