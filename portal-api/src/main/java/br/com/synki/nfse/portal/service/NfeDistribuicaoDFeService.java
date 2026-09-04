package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.config.NfeDistribuicaoProperties;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.NfeEntrada;
import br.com.synki.nfse.portal.fiscal.livrocaixa.NotaXmlExtrator;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import br.com.synki.nfse.portal.repository.NfeEntradaRepository;
import com.fincatto.documentofiscal.nfe.classes.distribuicao.NFDistribuicaoDocumentoZip;
import com.fincatto.documentofiscal.nfe.classes.distribuicao.NFDistribuicaoIntRetorno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Service
public class NfeDistribuicaoDFeService {

    private static final Logger log = LoggerFactory.getLogger(NfeDistribuicaoDFeService.class);

    private final NfeDistribuicaoProperties props;
    private final EmpresaRepository empresaRepository;
    private final NfeEntradaRepository entradaRepository;
    private final NfeLibService nfeLibService;
    private final NfeEntradaEmailService entradaEmailService;

    public NfeDistribuicaoDFeService(
            NfeDistribuicaoProperties props,
            EmpresaRepository empresaRepository,
            NfeEntradaRepository entradaRepository,
            NfeLibService nfeLibService,
            NfeEntradaEmailService entradaEmailService) {
        this.props = props;
        this.empresaRepository = empresaRepository;
        this.entradaRepository = entradaRepository;
        this.nfeLibService = nfeLibService;
        this.entradaEmailService = entradaEmailService;
    }

    public Map<String, Object> baixarEmpresa(Long empresaId) throws Exception {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada"));
        if (!empresa.isBaixarXml()) {
            throw new IllegalStateException("Emitente nao marcado para baixar XML (Baixar notas DF-e = NAO)");
        }
        if (!nfeLibService.temCertificado(empresaId)) {
            throw new IllegalStateException("Cadastre o certificado A1 do emitente para baixar XMLs");
        }

        var facade = nfeLibService.facadeForEmpresa(empresaId);
        var uf = nfeLibService.ufEmitente(empresaId, null);
        String ultNsu = padNsu(empresa.getUltimoNsu());
        List<Long> idsNovas = new ArrayList<>();
        int paginas = 0;
        String ultimoStat = "";

        while (paginas < props.maxPaginas()) {
            paginas++;
            NFDistribuicaoIntRetorno ret = facade.consultarDistribuicaoDFe(
                    empresa.getCnpj(), uf, null, null, ultNsu);
            ultimoStat = ret.getCodigoStatusReposta();
            if ("656".equals(ultimoStat)) {
                log.warn("SEFAZ consumo indevido ao baixar XML da empresa {} — pausando", empresaId);
                break;
            }
            if (ret.getLote() != null && ret.getLote().getDocZip() != null) {
                for (NFDistribuicaoDocumentoZip doc : ret.getLote().getDocZip()) {
                    idsNovas.addAll(processarDocumento(empresaId, doc, facade, uf, empresa.getCnpj()));
                }
            }
            if (ret.getUltimoNSU() != null && !ret.getUltimoNSU().isBlank()) {
                ultNsu = padNsu(ret.getUltimoNSU());
                empresa.setUltimoNsu(ultNsu);
                empresa.setUltimoNsuBaixadoEm(Instant.now());
                empresaRepository.save(empresa);
            }
            if (!"138".equals(ultimoStat)) {
                break;
            }
            if (ret.getMaximoNSU() != null && padNsu(ultNsu).compareTo(padNsu(ret.getMaximoNSU())) >= 0) {
                break;
            }
        }

        if (!idsNovas.isEmpty()) {
            entradaEmailService.enviarNovasSeConfigurado(empresaId, idsNovas);
        }

        var body = new LinkedHashMap<String, Object>();
        body.put("ok", true);
        body.put("novas", idsNovas.size());
        body.put("idsNovas", idsNovas);
        body.put("paginas", paginas);
        body.put("ultimoNsu", ultNsu);
        body.put("statusSefaz", ultimoStat);
        body.put("motivo", "XMLs de entrada gravados (DF-e destinatario)");
        return body;
    }

    public int baixarTodasMarcadas() {
        int ok = 0;
        for (Empresa empresa : empresaRepository.findByBaixarXmlTrueAndAtivoTrue()) {
            try {
                baixarEmpresa(empresa.getId());
                ok++;
            } catch (Exception ex) {
                log.warn("Falha ao baixar XML do emitente {} ({}): {}",
                        empresa.getId(), empresa.getNome(), ex.getMessage());
            }
        }
        return ok;
    }

    private List<Long> processarDocumento(
            Long empresaId,
            NFDistribuicaoDocumentoZip doc,
            com.fincatto.documentofiscal.nfe400.webservices.WSFacade facade,
            com.fincatto.documentofiscal.DFUnidadeFederativa uf,
            String cpfCnpj) throws Exception {
        String schema = doc.getSchema() == null ? "" : doc.getSchema();
        String xml = decodeGzip(doc.getValue());
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        if (schema.toLowerCase().contains("procnfe")) {
            Long id = gravarProcNfe(empresaId, xml, doc.getNsu(), schema);
            return id != null ? List.of(id) : List.of();
        }
        if (schema.toLowerCase().contains("resnfe")) {
            String chave = NotaXmlExtrator.extrairChaveNfe(xml).orElse(null);
            if (chave == null || entradaRepository.existsByEmpresaIdAndChave(empresaId, chave)) {
                return List.of();
            }
            try {
                var ret = facade.consultarDistribuicaoDFe(cpfCnpj, uf, chave, null, null);
                if (ret.getLote() != null && ret.getLote().getDocZip() != null) {
                    List<Long> gravados = new ArrayList<>();
                    for (NFDistribuicaoDocumentoZip full : ret.getLote().getDocZip()) {
                        String fullSchema = full.getSchema() == null ? "" : full.getSchema();
                        if (!fullSchema.toLowerCase().contains("procnfe")) {
                            continue;
                        }
                        Long id = gravarProcNfe(empresaId, decodeGzip(full.getValue()), full.getNsu(), fullSchema);
                        if (id != null) {
                            gravados.add(id);
                        }
                    }
                    return gravados;
                }
            } catch (Exception ex) {
                log.debug("Nao foi possivel baixar XML completo da chave {}: {}", chave, ex.getMessage());
            }
        }
        return List.of();
    }

    private Long gravarProcNfe(Long empresaId, String xml, String nsu, String schema) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        String chave = NotaXmlExtrator.extrairChaveNfe(xml).orElse(null);
        if (chave == null || entradaRepository.existsByEmpresaIdAndChave(empresaId, chave)) {
            return null;
        }
        var meta = NotaXmlExtrator.metadadosNfeEntrada(xml, chave);
        var entrada = new NfeEntrada();
        entrada.setEmpresaId(empresaId);
        entrada.setChave(chave);
        entrada.setNsu(nsu);
        entrada.setSchemaXml(schema);
        entrada.setCnpjEmitente(meta.cnpjEmitente());
        entrada.setNomeEmitente(meta.nomeEmitente());
        entrada.setNumero(meta.numero());
        entrada.setSerie(meta.serie());
        entrada.setDataEmissao(meta.data());
        entrada.setNatureza(meta.natureza());
        entrada.setValor(meta.valor());
        entrada.setXml(xml);
        return entradaRepository.save(entrada).getId();
    }

    static String decodeGzip(String base64) {
        if (base64 == null || base64.isBlank()) {
            return "";
        }
        try {
            byte[] compressed = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
            try (var gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            log.debug("Falha ao descompactar docZip: {}", ex.getMessage());
            return "";
        }
    }

    static String padNsu(String nsu) {
        String digits = nsu == null ? "" : nsu.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return "000000000000000";
        }
        if (digits.length() >= 15) {
            return digits.substring(digits.length() - 15);
        }
        return "0".repeat(15 - digits.length()) + digits;
    }
}
