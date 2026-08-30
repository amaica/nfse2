package br.com.synki.nfse.portal.fiscal.livrocaixa;

import br.com.synki.nfse.portal.domain.Empresa;
import br.com.synki.nfse.portal.domain.EmpresaEndereco;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gera arquivo LCDPR leiaute 1.3 (pipe UTF-8).
 * Referência: Manual RFB + exemplo tiagoadmstz/LCDPR (MIT).
 */
@Service
public class LcdprGeradorService {

    private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter MMYYYY = DateTimeFormatter.ofPattern("MMyyyy");

    public byte[] gerar(Empresa empresa, EmpresaEndereco endereco, LocalDate de, LocalDate ate, List<LancamentoLivroCaixa> lancamentos) {
        if (lancamentos.isEmpty()) {
            throw new IllegalStateException("Nenhum lancamento para gerar LCDPR");
        }
        var doc = apenasDigitos(empresa.getCnpj());
        if (doc.length() != 11 && doc.length() != 14) {
            throw new IllegalStateException("Documento do emitente invalido para LCDPR");
        }
        if (doc.length() == 14) {
            throw new IllegalStateException(
                    "LCDPR exige titular pessoa fisica (CPF 11 digitos). Emitente cadastrado como CNPJ — gere o Livro Caixa CSV e valide com o contador.");
        }

        var linhas = new ArrayList<String>();
        linhas.add(reg0000(doc, empresa.getNome(), de, ate));
        linhas.add("0010|1");
        linhas.add(reg0030(empresa, endereco));

        var codImovel = "001";
        var codConta = "001";
        linhas.add(reg0040(empresa, endereco, codImovel));
        linhas.add("0050|" + codConta + "|BR|001|Conta SyncNota|0001|000000000000");

        BigDecimal saldo = BigDecimal.ZERO;
        int seqDoc = 1;
        for (var lanc : lancamentos) {
            if (lanc.tipoMovimento() == LancamentoLivroCaixa.TipoMovimento.RECEITA) {
                saldo = saldo.add(lanc.valor());
                linhas.add(regQ100Receita(lanc, codImovel, codConta, seqDoc++, saldo));
            } else {
                saldo = saldo.subtract(lanc.valor());
                linhas.add(regQ100Despesa(lanc, codImovel, codConta, seqDoc++, saldo));
            }
        }

        for (var q200 : resumoMensal(lancamentos, de, ate)) {
            linhas.add(q200);
        }

        linhas.add(reg9999(empresa, linhas.size() + 1));
        var conteudo = String.join("\r\n", linhas) + "\r\n";
        return conteudo.getBytes(StandardCharsets.UTF_8);
    }

    private static String reg0000(String cpf, String nome, LocalDate de, LocalDate ate) {
        return "0000|LCDPR|0013|" + cpf + "|" + sanitizar(nome) + "|0|0|"
                + de.format(DDMMYYYY) + "|" + ate.format(DDMMYYYY);
    }

    private static String reg0030(Empresa empresa, EmpresaEndereco endereco) {
        var end = endereco != null ? endereco : new EmpresaEndereco();
        return "0030|"
                + sanitizar(coalesce(end.getLogradouro(), "")) + "|"
                + sanitizar(coalesce(end.getNumero(), "S/N")) + "|"
                + sanitizar(coalesce(end.getComplemento(), "")) + "|"
                + sanitizar(coalesce(end.getBairro(), "")) + "|"
                + sanitizar(coalesce(end.getUf(), "RS")) + "|"
                + padLeft(coalesce(end.getCodigoMunicipioIbge(), "0000000"), 7, '0') + "|"
                + padLeft(apenasDigitos(coalesce(end.getCep(), "00000000")), 8, '0') + "|"
                + padLeft(apenasDigitos(coalesce(empresa.getTelefone(), "0")), 11, '0') + "|"
                + sanitizar(coalesce(empresa.getEmail(), "contato@syncnota.local"));
    }

    private static String reg0040(Empresa empresa, EmpresaEndereco endereco, String codImovel) {
        var end = endereco != null ? endereco : new EmpresaEndereco();
        return "0040|" + codImovel + "|BR|BRL|||||"
                + sanitizar(coalesce(empresa.getNomeFantasia(), empresa.getNome())) + "|"
                + sanitizar(coalesce(end.getLogradouro(), "")) + "|"
                + sanitizar(coalesce(end.getNumero(), "")) + "|"
                + sanitizar(coalesce(end.getComplemento(), "")) + "|"
                + sanitizar(coalesce(end.getBairro(), "")) + "|"
                + sanitizar(coalesce(end.getUf(), "RS")) + "|"
                + padLeft(coalesce(end.getCodigoMunicipioIbge(), "0000000"), 7, '0') + "|"
                + padLeft(apenasDigitos(coalesce(end.getCep(), "00000000")), 8, '0') + "|"
                + "1|10000";
    }

    private static String regQ100Receita(LancamentoLivroCaixa lanc, String codImovel, String codConta, int numDoc, BigDecimal saldo) {
        return "Q100|"
                + lanc.data().format(DDMMYYYY) + "|"
                + codImovel + "|"
                + codConta + "|"
                + numDoc + "|"
                + "1|"
                + sanitizar(lanc.historico()) + "|"
                + padLeft(apenasDigitos(lanc.contraparteDoc()), 14, '0') + "|"
                + "3|"
                + centavos(lanc.valor()) + "|"
                + centavos(BigDecimal.ZERO) + "|"
                + centavos(saldo) + "|"
                + naturezaSaldo(saldo);
    }

    private static String regQ100Despesa(LancamentoLivroCaixa lanc, String codImovel, String codConta, int numDoc, BigDecimal saldo) {
        return "Q100|"
                + lanc.data().format(DDMMYYYY) + "|"
                + codImovel + "|"
                + codConta + "|"
                + numDoc + "|"
                + "1|"
                + sanitizar(lanc.historico()) + "|"
                + padLeft(apenasDigitos(lanc.contraparteDoc()), 14, '0') + "|"
                + "1|"
                + centavos(BigDecimal.ZERO) + "|"
                + centavos(lanc.valor()) + "|"
                + centavos(saldo) + "|"
                + naturezaSaldo(saldo);
    }

    private static List<String> resumoMensal(List<LancamentoLivroCaixa> lancamentos, LocalDate de, LocalDate ate) {
        Map<YearMonth, BigDecimal[]> mapa = new LinkedHashMap<>();
        var cursor = YearMonth.from(de);
        var fim = YearMonth.from(ate);
        while (!cursor.isAfter(fim)) {
            mapa.put(cursor, new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            cursor = cursor.plusMonths(1);
        }
        for (var lanc : lancamentos) {
            var ym = YearMonth.from(lanc.data());
            var arr = mapa.computeIfAbsent(ym, k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            if (lanc.tipoMovimento() == LancamentoLivroCaixa.TipoMovimento.RECEITA) {
                arr[0] = arr[0].add(lanc.valor());
            } else {
                arr[1] = arr[1].add(lanc.valor());
            }
        }
        var linhas = new ArrayList<String>();
        BigDecimal saldo = BigDecimal.ZERO;
        for (var entry : mapa.entrySet()) {
            saldo = saldo.add(entry.getValue()[0]).subtract(entry.getValue()[1]);
            linhas.add("Q200|"
                    + entry.getKey().format(MMYYYY) + "|"
                    + centavos(entry.getValue()[0]) + "|"
                    + centavos(entry.getValue()[1]) + "|"
                    + centavos(saldo) + "|"
                    + naturezaSaldo(saldo));
        }
        return linhas;
    }

    private static String reg9999(Empresa empresa, int qtdLin) {
        var doc = apenasDigitos(empresa.getCnpj());
        return "9999|"
                + sanitizar(empresa.getNome()) + "|"
                + doc + "|"
                + "|"
                + sanitizar(coalesce(empresa.getEmail(), "")) + "|"
                + padLeft(apenasDigitos(coalesce(empresa.getTelefone(), "0")), 11, '0') + "|"
                + qtdLin;
    }

    private static String centavos(BigDecimal valor) {
        var cents = valor.abs().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        return padLeft(cents.toPlainString(), 15, '0');
    }

    private static String naturezaSaldo(BigDecimal saldo) {
        if (saldo.signum() >= 0) {
            return "P";
        }
        return "N";
    }

    private static String sanitizar(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("|", " ").replace("\r", " ").replace("\n", " ").trim();
    }

    private static String apenasDigitos(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\D", "");
    }

    private static String padLeft(String s, int len, char c) {
        if (s.length() >= len) {
            return s.substring(0, len);
        }
        return String.valueOf(c).repeat(len - s.length()) + s;
    }

    private static String coalesce(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}
