package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoNfse;
import br.com.synki.nfse.portal.web.dto.EmissaoCompletaRequest;
import io.github.t3wv.nfse.nacional.classes.nfsenacional.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DpsMontadorService {

    private static final AtomicLong NUMERO = new AtomicLong(System.currentTimeMillis() % 1_000_000);
    private static final ZoneId BR = ZoneId.of("-03:00");

    private final CertificadoLeituraService certificadoLeituraService;

    public DpsMontadorService(CertificadoLeituraService certificadoLeituraService) {
        this.certificadoLeituraService = certificadoLeituraService;
    }

    public NFSeSefinNacionalDPS montar(Long empresaId, ConfiguracaoNfse cfg, EmissaoCompletaRequest req) {
        var meta = certificadoLeituraService.lerMetadados(empresaId)
                .orElseThrow(() -> new IllegalStateException("Certificado A1 nao cadastrado"));

        var ambiente = cfg.isProducao()
                ? NFSeSefinNacionalTipoAmbiente.PRODUCAO
                : NFSeSefinNacionalTipoAmbiente.HOMOLOGACAO;

        var id = req.identificacao();
        long numeroDps = id.numeroRps() != null ? id.numeroRps() : NUMERO.incrementAndGet();
        LocalDate competencia = LocalDate.parse(id.competencia());
        ZonedDateTime dhEmi = parseDataHora(id.dataEmissao());

        var tribIss = enumCodigo(NFSeSefinNacionalTribMunicipalTributacaoISSQN.class, req.regime().tributacaoIssqn(), NFSeSefinNacionalTribMunicipalTributacaoISSQN.OPERACAO_TRIBUTAVEL);
        var retIss = enumCodigo(NFSeSefinNacionalTribMunicipalTipoRetencaoISSQN.class, req.regime().issRetido(), NFSeSefinNacionalTribMunicipalTipoRetencaoISSQN.NAO_RETIDO);
        var regEsp = enumCodigo(NFSeSefinNacionalRegimeTributarioRegimeEspecialTributacao.class, req.regime().regimeEspecialTributacao(), NFSeSefinNacionalRegimeTributarioRegimeEspecialTributacao.NENHUM);
        var simp = enumCodigo(NFSeSefinNacionalRegimeTributarioSituacaoSimplesNacional.class, req.regime().simplesNacional(), NFSeSefinNacionalRegimeTributarioSituacaoSimplesNacional.NAO_OPTANTE);

        var tomadorDoc = apenasDigitos(req.tomador().documento());
        var tomador = new NFSeSefinNacionalInfoPessoa()
                .setNome(req.tomador().razaoSocial().trim())
                .setEmail(blankToNull(req.tomador().email()))
                .setTelefone(blankToNull(req.tomador().telefone()))
                .setInscricaoMunicipal(blankToNull(req.tomador().inscricaoMunicipal()));
        if (tomadorDoc.length() == 11) {
            tomador.setCPF(tomadorDoc);
        } else if (tomadorDoc.length() == 14) {
            tomador.setCNPJ(tomadorDoc);
        } else {
            throw new IllegalArgumentException("Documento do tomador invalido");
        }

        if (req.enderecoTomador() != null && temEndereco(req.enderecoTomador())) {
            tomador.setEndereco(montarEndereco(req.enderecoTomador()));
        }

        var codigoNacional = normalizarCodigoServico(req.servico().itemListaServico());
        var munPrestacao = primeiroNaoVazio(req.servico().municipioPrestacao(), req.servico().localPrestacao(), cfg.getCodigoMunicipioIbge());

        var serv = new NFSeSefinNacionalServ()
                .setLocalPrestacao(new NFSeSefinNacionalLocPrest().setCodigoMunicipio(munPrestacao))
                .setCServ(new NFSeSefinNacionalCServ()
                        .setCodigoNacionalTributacaoISSQN(codigoNacional)
                        .setCodigoMunicipalTributacaoISSQN(blankToNull(req.servico().codigoTributacaoMunicipio()))
                        .setDescricaoServico(req.servico().descricaoServico().trim())
                        .setCodigoNBS(blankToNull(req.servico().nbs()) != null ? req.servico().nbs() : "114061100")
                        .setCodigoInternoContribuinte(blankToNull(req.servico().cnae())));

        var valores = req.valores();
        var vServPrest = new NFSeSefinNacionalVServPrest().setValorServicos(valores.valorServicos());

        var infoValores = new NFSeSefinNacionalInfoValores().setValoresServicoPrestado(vServPrest);

        if (nz(valores.descontoIncondicionado()).signum() > 0 || nz(valores.descontoCondicionado()).signum() > 0) {
            infoValores.setValoresDescontosCondicionadosEIncondicionados(new NFSeSefinNacionalVDescCondIncond()
                    .setValorDescontoIncondicionado(nz(valores.descontoIncondicionado()))
                    .setValorDescontoCondicionado(nz(valores.descontoCondicionado())));
        }
        if (nz(valores.deducoes()).signum() > 0) {
            infoValores.setValoresDeducaoBaseCalculo(new NFSeSefinNacionalInfoDedRed()
                    .setValorMonetarioPadrao(valores.deducoes().toPlainString()));
        }

        var tribMun = new NFSeSefinNacionalTribMunicipal()
                .setTributacaoISSQN(tribIss)
                .setTipoRetencaoISSQN(retIss);
        if (valores.aliquota() != null) {
            tribMun.setPercentualAliquota(valores.aliquota());
        }

        var cstPis = NFSeSefinNacionalTribOutrosPisCofinsSituacaoTributaria.CONTRIBUICAO_SEM_INCIDENCIA;
        if (req.tributacaoFederal() != null && blankToNull(req.tributacaoFederal().cstPisCofins()) != null) {
            var parsed = NFSeSefinNacionalTribOutrosPisCofinsSituacaoTributaria.valueOfCodigo(
                    req.tributacaoFederal().cstPisCofins());
            if (parsed != null) {
                cstPis = parsed;
            }
        }
        var pisCofins = new NFSeSefinNacionalTribOutrosPisCofins().setCST(cstPis);
        if (req.tributacaoFederal() != null) {
            var tf = req.tributacaoFederal();
            if (tf.baseCalculoPisCofins() != null) {
                pisCofins.setValorBaseCalculoPisCofins(tf.baseCalculoPisCofins());
            }
            if (tf.aliquotaPis() != null) pisCofins.setAliquotaPIS(tf.aliquotaPis());
            if (tf.aliquotaCofins() != null) pisCofins.setAliquotaCOFINS(tf.aliquotaCofins());
            if (tf.valorPis() != null) pisCofins.setValorPIS(tf.valorPis());
            if (tf.valorCofins() != null) pisCofins.setValorCofins(tf.valorCofins());
        }
        var tribFed = new NFSeSefinNacionalTribFederal().setPiscofins(pisCofins);
        if (req.retencoesFederais() != null) {
            var r = req.retencoesFederais();
            tribFed.setValorCP(nz(r.inss()));
            tribFed.setValorIRRF(nz(r.ir()));
            tribFed.setValorCSLL(nz(r.csll()));
        } else if (req.tributacaoFederal() != null && Boolean.TRUE.equals(req.tributacaoFederal().habilitarRetencoes())) {
            var tf = req.tributacaoFederal();
            tribFed.setValorCP(nz(tf.retencaoInss()));
            tribFed.setValorIRRF(nz(tf.retencaoIrrf()));
            tribFed.setValorCSLL(nz(tf.retencaoCsll()));
        }

        infoValores.setTributos(new NFSeSefinNacionalInfoTributacao()
                .setTributosMunicipais(tribMun)
                .setTributosNacionais(tribFed)
                .setTotalTributos(new NFSeSefinNacionalTribTotal().setIndicadorValorTotalTributos("0")));

        var prestador = new NFSeSefinNacionalInfoPrestador()
                .setCNPJ(meta.documento())
                .setNome(meta.titular())
                .setRegimeTributario(new NFSeSefinNacionalRegTrib()
                        .setOpSimplesNacional(simp)
                        .setRegimeEspecialTributacao(regEsp));
        if (req.prestador() != null && blankToNull(req.prestador().inscricaoMunicipal()) != null) {
            prestador.setIM(req.prestador().inscricaoMunicipal());
        }

        if (req.classificacao() != null && blankToNull(req.classificacao().atividadePrincipal()) != null) {
            serv.getCServ().setCodigoMunicipalTributacaoISSQN(req.classificacao().atividadePrincipal());
        }

        var inf = new NFSeSefinNacionalInfDPS()
                .setTipoAmbiente(ambiente)
                .setDataHoraEmissao(dhEmi)
                .setSerie(id.serieRps())
                .setNumeroDPS(numeroDps)
                .setCodigoMunicipioEmissao(cfg.getCodigoMunicipioIbge())
                .setDataInicioPrestacaoServico(competencia)
                .setTipoEmitente(parseTipoEmitente(id.tipoRps()))
                .setPrestador(prestador)
                .setTomador(tomador)
                .setServicoPrestado(serv)
                .setValores(infoValores);

        if (req.intermediario() != null && blankToNull(req.intermediario().documento()) != null) {
            inf.setIntermediario(montarIntermediario(req.intermediario()));
        }

        if (req.construcaoCivil() != null) {
            var cc = req.construcaoCivil();
            if (blankToNull(cc.codigoObra()) != null) {
                serv.setObra(new NFSeSefinNacionalInfoObra().setNumeroIdentificacaoObra(cc.codigoObra().trim()));
            }
            if (blankToNull(cc.art()) != null) {
                var complArt = new NFSeSefinNacionalInfoCompl().setIdDocResponsabilidadeTecnica(cc.art().trim());
                var merged = mergeInfoCompl(serv.getInformacoesComplementares(), complArt);
                if (merged != null) {
                    serv.setInformacoesComplementares(merged);
                }
            }
        }

        if (req.ibsCbs() != null && Boolean.TRUE.equals(req.ibsCbs().habilitar())) {
            var ibs = new NFSeSefinNacionalInfoIBSCBS();
            if (blankToNull(req.ibsCbs().classificacaoOperacao()) != null) {
                ibs.setcIndOp(req.ibsCbs().classificacaoOperacao());
            }
            inf.setInfoIBSCBS(ibs);
        }

        if (req.informacoesAdicionais() != null) {
            var texto = montarTextoInformacoesAdicionais(req.informacoesAdicionais());
            if (texto != null) {
                var compl = new NFSeSefinNacionalInfoCompl().setInformacoesComplementares(texto);
                var merged = mergeInfoCompl(serv.getInformacoesComplementares(), compl);
                if (merged != null) {
                    serv.setInformacoesComplementares(merged);
                }
            }
        }

        return new NFSeSefinNacionalDPS().setInfDPS(inf);
    }

    private static String montarTextoInformacoesAdicionais(EmissaoCompletaRequest.InformacoesAdicionais info) {
        var partes = new java.util.ArrayList<String>();
        if (blankToNull(info.informacoesComplementares()) != null) {
            partes.add(info.informacoesComplementares().trim());
        }
        if (blankToNull(info.observacoes()) != null) {
            partes.add(info.observacoes().trim());
        }
        return partes.isEmpty() ? null : String.join("\n", partes);
    }

    private static NFSeSefinNacionalInfoCompl mergeInfoCompl(
            NFSeSefinNacionalInfoCompl base, NFSeSefinNacionalInfoCompl extra) {
        if (base == null) {
            return infoComplComConteudo(extra) ? extra : null;
        }
        if (extra == null) {
            return infoComplComConteudo(base) ? base : null;
        }
        var merged = new NFSeSefinNacionalInfoCompl();
        merged.setIdDocResponsabilidadeTecnica(
                primeiroNaoVazio(extra.getIdDocResponsabilidadeTecnica(), base.getIdDocResponsabilidadeTecnica()));
        merged.setReferenciaDocumento(primeiroNaoVazio(extra.getReferenciaDocumento(), base.getReferenciaDocumento()));
        var texto = "";
        if (blankToNull(base.getInformacoesComplementares()) != null) {
            texto = base.getInformacoesComplementares();
        }
        if (blankToNull(extra.getInformacoesComplementares()) != null) {
            texto = texto.isEmpty() ? extra.getInformacoesComplementares() : texto + "\n" + extra.getInformacoesComplementares();
        }
        if (!texto.isEmpty()) {
            merged.setInformacoesComplementares(texto);
        }
        return infoComplComConteudo(merged) ? merged : null;
    }

    private static boolean infoComplComConteudo(NFSeSefinNacionalInfoCompl c) {
        if (c == null) {
            return false;
        }
        return blankToNull(c.getIdDocResponsabilidadeTecnica()) != null
                || blankToNull(c.getReferenciaDocumento()) != null
                || blankToNull(c.getInformacoesComplementares()) != null;
    }

    private static NFSeSefinNacionalInfoPessoa montarIntermediario(EmissaoCompletaRequest.Intermediario i) {
        var doc = apenasDigitos(i.documento());
        var p = new NFSeSefinNacionalInfoPessoa()
                .setNome(i.razaoSocial())
                .setInscricaoMunicipal(blankToNull(i.inscricaoMunicipal()));
        if (doc.length() == 11) {
            p.setCPF(doc);
        } else {
            p.setCNPJ(doc);
        }
        return p;
    }

    private static NFSeSefinNacionalEndereco montarEndereco(EmissaoCompletaRequest.EnderecoTomador e) {
        var end = new NFSeSefinNacionalEndereco()
                .setLogradouro(e.logradouro())
                .setNumero(e.numero())
                .setComplemento(blankToNull(e.complemento()))
                .setBairro(e.bairro());
        if (blankToNull(e.codigoMunicipioIbge()) != null || blankToNull(e.cep()) != null) {
            end.setEnderecoNacional(new NFSeSefinNacionalEnderNac()
                    .setCodigoMunicipio(primeiroNaoVazio(e.codigoMunicipioIbge(), ""))
                    .setCEP(apenasDigitos(e.cep())));
        }
        return end;
    }

    private static boolean temEndereco(EmissaoCompletaRequest.EnderecoTomador e) {
        return blankToNull(e.logradouro()) != null;
    }

    private static NFSeSefinNacionalInfDPSTipoEmitente parseTipoEmitente(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return NFSeSefinNacionalInfDPSTipoEmitente.PRESTADOR;
        }
        var t = NFSeSefinNacionalInfDPSTipoEmitente.valueOfCodigo(tipo);
        return t != null ? t : NFSeSefinNacionalInfDPSTipoEmitente.PRESTADOR;
    }

    private static ZonedDateTime parseDataHora(String dataEmissao) {
        if (dataEmissao == null || dataEmissao.isBlank()) {
            return ZonedDateTime.now(BR);
        }
        if (dataEmissao.length() <= 10) {
            return LocalDate.parse(dataEmissao).atStartOfDay(BR);
        }
        return LocalDateTime.parse(dataEmissao, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(BR);
    }

    private static <E extends Enum<E>> E enumCodigo(Class<E> type, String codigo, E padrao) {
        if (codigo == null || codigo.isBlank()) {
            return padrao;
        }
        try {
            for (var m : type.getMethods()) {
                if (m.getName().equals("valueOfCodigo") && m.getParameterCount() == 1) {
                    @SuppressWarnings("unchecked")
                    var v = (E) m.invoke(null, codigo);
                    return v != null ? v : padrao;
                }
            }
        } catch (Exception ignored) {
        }
        return padrao;
    }

    static String normalizarCodigoServico(String codigo) {
        var numerico = codigo.replaceAll("\\D", "");
        if (numerico.length() < 6) {
            throw new IllegalArgumentException("Item lista servico (LC 116) invalido");
        }
        return numerico.length() > 6 ? numerico.substring(0, 6) : numerico;
    }

    private static String apenasDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String primeiroNaoVazio(String... vals) {
        for (var v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
