package br.com.synki.nfse.portal.service;

/** Regras de série NF-e para emitente CPF (NT 2018.001 — faixa 920 a 969). */
public final class NfeSerieUtil {

    public static final int SERIE_CPF_MIN = 920;
    public static final int SERIE_CPF_MAX = 969;
    public static final String SERIE_CPF_PADRAO = "921";

    private NfeSerieUtil() {}

    public static boolean isDocumentoCpf(String documento) {
        return apenasDigitos(documento).length() == 11;
    }

    public static String seriePadrao(String documentoEmpresa) {
        return isDocumentoCpf(documentoEmpresa) ? SERIE_CPF_PADRAO : "1";
    }

    public static String normalizarSerieNfe(String serie, boolean emitenteCpf) {
        if (!emitenteCpf) {
            return serie != null && !serie.isBlank() ? serie.trim() : "1";
        }
        int valor;
        try {
            valor = Integer.parseInt(serie != null && !serie.isBlank() ? serie.trim() : SERIE_CPF_PADRAO);
        } catch (NumberFormatException ex) {
            valor = Integer.parseInt(SERIE_CPF_PADRAO);
        }
        if (valor < SERIE_CPF_MIN || valor > SERIE_CPF_MAX) {
            return SERIE_CPF_PADRAO;
        }
        return String.valueOf(valor);
    }

    private static String apenasDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }
}
