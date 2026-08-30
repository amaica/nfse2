package br.com.synki.nfse.portal.fiscal.livrocaixa;

import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.repository.EmpresaEnderecoRepository;
import br.com.synki.nfse.portal.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LivroCaixaService {

    private final XmlNotasColetorService coletor;
    private final LcdprGeradorService lcdprGerador;
    private final EmpresaRepository empresaRepository;
    private final EmpresaEnderecoRepository enderecoRepository;

    public LivroCaixaService(
            XmlNotasColetorService coletor,
            LcdprGeradorService lcdprGerador,
            EmpresaRepository empresaRepository,
            EmpresaEnderecoRepository enderecoRepository) {
        this.coletor = coletor;
        this.lcdprGerador = lcdprGerador;
        this.empresaRepository = empresaRepository;
        this.enderecoRepository = enderecoRepository;
    }

    public Map<String, Object> resumo(Long empresaId, LocalDate de, LocalDate ate, boolean nfse, boolean nfe) throws Exception {
        var lancamentos = coletor.coletar(empresaId, de, ate, nfse, nfe);
        if (lancamentos.isEmpty()) {
            throw new IllegalStateException("Nenhum XML de nota encontrado no periodo. Emita notas ou baixe XMLs antes de gerar o livro caixa.");
        }
        var receitas = BigDecimal.ZERO;
        var despesas = BigDecimal.ZERO;
        var itens = new ArrayList<Map<String, Object>>();
        for (var l : lancamentos) {
            if (l.tipoMovimento() == LancamentoLivroCaixa.TipoMovimento.RECEITA) {
                receitas = receitas.add(l.valor());
            } else {
                despesas = despesas.add(l.valor());
            }
            itens.add(lancamentoMap(l));
        }
        var resultado = receitas.subtract(despesas);
        var body = new LinkedHashMap<String, Object>();
        body.put("de", de.toString());
        body.put("ate", ate.toString());
        body.put("totalNotas", lancamentos.size());
        body.put("receitas", receitas);
        body.put("despesas", despesas);
        body.put("resultado", resultado);
        body.put("itens", itens);
        body.put("fonte", "XMLs de NFS-e/NF-e emitidas e NF-e de entrada baixadas da SEFAZ");
        body.put("lcdprDisponivel", documentoSuportaLcdpr(empresaId));
        return body;
    }

    public byte[] gerarCsv(Long empresaId, LocalDate de, LocalDate ate, boolean nfse, boolean nfe) throws Exception {
        var lancamentos = coletor.coletar(empresaId, de, ate, nfse, nfe);
        if (lancamentos.isEmpty()) {
            throw new IllegalStateException("Nenhum XML encontrado no periodo");
        }
        var sb = new StringBuilder();
        sb.append("data;tipo;numero;chave;historico;contraparte;documento;valor;movimento\r\n");
        for (var l : lancamentos) {
            sb.append(l.data()).append(';')
                    .append(csv(l.origem())).append(';')
                    .append(csv(l.numeroDocumento())).append(';')
                    .append(csv(l.chaveAcesso())).append(';')
                    .append(csv(l.historico())).append(';')
                    .append(csv(l.contraparteNome())).append(';')
                    .append(csv(l.contraparteDoc())).append(';')
                    .append(l.valor().toPlainString()).append(';')
                    .append(l.tipoMovimento().name()).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] gerarLcdpr(Long empresaId, LocalDate de, LocalDate ate, boolean nfse, boolean nfe) throws Exception {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalStateException("Empresa nao encontrada"));
        var endereco = enderecoRepository.findByEmpresaIdAndPrincipalTrue(empresaId).orElse(null);
        var lancamentos = coletor.coletar(empresaId, de, ate, nfse, nfe);
        return lcdprGerador.gerar(empresa, endereco, de, ate, lancamentos);
    }

    public boolean documentoSuportaLcdpr(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .map(Empresa::getCnpj)
                .map(c -> c.replaceAll("\\D", ""))
                .map(d -> d.length() == 11)
                .orElse(false);
    }

    private static Map<String, Object> lancamentoMap(LancamentoLivroCaixa l) {
        var m = new LinkedHashMap<String, Object>();
        m.put("data", l.data().toString());
        m.put("origem", l.origem());
        m.put("numero", l.numeroDocumento());
        m.put("chave", l.chaveAcesso());
        m.put("historico", l.historico());
        m.put("contraparte", l.contraparteNome());
        m.put("documento", l.contraparteDoc());
        m.put("valor", l.valor());
        m.put("movimento", l.tipoMovimento().name());
        return m;
    }

    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        var t = s.replace(";", ",").replace("\r", " ").replace("\n", " ");
        return t.contains("\"") ? "\"" + t.replace("\"", "\"\"") + "\"" : t;
    }
}
