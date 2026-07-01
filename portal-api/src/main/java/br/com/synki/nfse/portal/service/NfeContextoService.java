package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.domain.ConfiguracaoDocumento;
import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import br.com.synki.nfse.portal.repository.ConfiguracaoDocumentoRepository;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import com.fincatto.documentofiscal.DFModelo;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NfeContextoService {

    private final EmpresaRepository empresaRepository;
    private final NfeLibService nfeLibService;
    private final CertificadoLeituraService certificadoLeituraService;
    private final ConfiguracaoDocumentoRepository documentoRepository;
    private final EmpresaEnderecoRepository enderecoRepository;

    public NfeContextoService(
            EmpresaRepository empresaRepository,
            NfeLibService nfeLibService,
            CertificadoLeituraService certificadoLeituraService,
            ConfiguracaoDocumentoRepository documentoRepository,
            EmpresaEnderecoRepository enderecoRepository) {
        this.empresaRepository = empresaRepository;
        this.nfeLibService = nfeLibService;
        this.certificadoLeituraService = certificadoLeituraService;
        this.documentoRepository = documentoRepository;
        this.enderecoRepository = enderecoRepository;
    }

    public Map<String, Object> contexto(Long empresaId, DFModelo modelo) throws Exception {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalStateException("Empresa nao encontrada"));
        var certificadoOk = nfeLibService.temCertificado(empresaId);
        var meta = certificadoLeituraService.lerMetadados(empresaId);
        var tipo = modelo == DFModelo.NFCE ? ConfiguracaoDocumento.TIPO_NFCE : ConfiguracaoDocumento.TIPO_NFE;
        var doc = documentoRepository.findByEmpresaIdAndTipo(empresaId, tipo).orElse(null);

        String emitenteDocumento = empresa.getCnpj() != null ? empresa.getCnpj().replaceAll("\\D", "") : "";
        if (emitenteDocumento.isEmpty()) {
            emitenteDocumento = meta.map(CertificadoLeituraService.Metadados::cpfTitular).orElse(null);
        }
        boolean emitentePf = emitenteDocumento.length() == 11
                || meta.map(CertificadoLeituraService.Metadados::eCpf).orElse(false);
        String emitenteNome = meta.map(CertificadoLeituraService.Metadados::titular).orElse(empresa.getNome());

        var enderecos = enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId).stream()
                .map(this::mapEndereco)
                .toList();

        var body = new LinkedHashMap<String, Object>();
        body.put("modelo", modelo.getCodigo());
        body.put("empresaNome", empresa.getNome());
        body.put("empresaCnpj", empresa.getCnpj());
        body.put("emitenteNome", emitenteNome);
        body.put("emitenteDocumento", emitenteDocumento);
        body.put("emitentePessoaFisica", emitentePf);
        meta.ifPresent(m -> body.put("certificadoECpf", m.eCpf()));
        body.put("ambiente", nfeLibService.ambiente(empresaId).getCodigo());
        body.put("ufEmitente", nfeLibService.ufEmitente(empresaId, null).name());
        body.put("certificadoCadastrado", certificadoOk);
        body.put("documentoHabilitado", doc != null && doc.isHabilitado());
        body.put("serie", doc != null ? doc.getSerie() : "1");
        body.put("ultimoNumero", doc != null ? doc.getUltimoNumero() : 0);
        body.put("proximoNumero", doc != null ? doc.getUltimoNumero() + 1 : 1);
        body.put("enderecos", enderecos);
        body.put("optanteSimples", empresa.isOptanteSimples());
        body.put("podeEmitir", certificadoOk && (emitenteDocumento.length() == 11 || emitenteDocumento.length() == 14));
        if (!certificadoOk) {
            body.put("aviso", "Cadastre o certificado A1 (.pfx) antes de emitir.");
        } else if (emitentePf) {
            body.put("aviso", "Emitente CPF: use serie NF-e entre 920 e 969 (ex.: 921 por IE).");
        }
        return body;
    }

    private Map<String, Object> mapEndereco(EmpresaEndereco e) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", e.getId());
        m.put("apelido", e.getApelido());
        m.put("municipio", e.getMunicipio());
        m.put("uf", e.getUf());
        m.put("codigoMunicipioIbge", e.getCodigoMunicipioIbge());
        m.put("inscricaoEstadual", e.getInscricaoEstadual());
        m.put("serieNfe", e.getSerieNfe());
        m.put("ultimoNumeroNfe", e.getUltimoNumeroNfe());
        m.put("proximoNumeroNfe", e.getUltimoNumeroNfe() + 1);
        m.put("principal", e.isPrincipal());
        return m;
    }
}
