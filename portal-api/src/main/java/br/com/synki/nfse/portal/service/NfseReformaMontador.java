package br.com.synki.nfse.portal.service;

import br.com.synki.nfse.portal.web.dto.EmissaoCompletaRequest;
import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBS;
import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSFinNFSe;
import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSIndDest;
import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSInfoTributos;
import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS;
import io.github.t3wv.nfse.nacional.classes.nfsenacional.NFSeSefinNacionalInfoIBSCBSInfoValoresIBSCBS;

import java.time.LocalDate;

/**
 * Montagem do grupo IBSCBS na DPS (NFS-e nacional) — Reforma Tributária / RTC.
 * A SEFIN calcula alíquotas/valores na NFSe autorizada; a DPS envia CST + cClassTrib + cIndOp.
 */
public final class NfseReformaMontador {

    public static final LocalDate OBRIGATORIEDADE_IBS_CBS = LocalDate.of(2026, 8, 3);
    private static final String CST_PADRAO = "000";
    private static final String CLASS_TRIB_PADRAO = "000001";
    /** Anexo VII IndOp — operação padrão de fornecimento de serviço. */
    private static final String C_IND_OP_PADRAO = "100301";

    private NfseReformaMontador() {}

    public static boolean deveIncluir(EmissaoCompletaRequest.IbsCbs req) {
        if (req != null && Boolean.FALSE.equals(req.habilitar())) {
            return false;
        }
        if (req != null && Boolean.TRUE.equals(req.habilitar())) {
            return true;
        }
        // A partir da obrigatoriedade, inclui mesmo sem flag explícita
        return !LocalDate.now().isBefore(OBRIGATORIEDADE_IBS_CBS);
    }

    public static NFSeSefinNacionalInfoIBSCBS montar(EmissaoCompletaRequest.IbsCbs req) {
        if (!deveIncluir(req)) {
            return null;
        }
        String cst = primeiro(req != null ? req.cst() : null, CST_PADRAO);
        String classTrib = primeiro(req != null ? req.classificacaoTributaria() : null, CLASS_TRIB_PADRAO);
        String cIndOp = primeiro(req != null ? req.classificacaoOperacao() : null, C_IND_OP_PADRAO);

        var g = new NFSeSefinNacionalInfoIBSCBSInfoTributosGIBSCBS()
                .setCST(pad3(cst))
                .setcClassTrib(pad6(classTrib));

        var trib = new NFSeSefinNacionalInfoIBSCBSInfoTributos().setgIBSCBS(g);
        var valores = new NFSeSefinNacionalInfoIBSCBSInfoValoresIBSCBS().setTrib(trib);

        return new NFSeSefinNacionalInfoIBSCBS()
                .setFinNFSe(NFSeSefinNacionalInfoIBSCBSFinNFSe.REGULAR)
                .setcIndOp(cIndOp)
                .setIndDest(NFSeSefinNacionalInfoIBSCBSIndDest.TOMADOR)
                .setValores(valores);
    }

    private static String pad3(String v) {
        var d = v.replaceAll("\\D", "");
        if (d.length() >= 3) return d.substring(0, 3);
        return ("000" + d).substring(("000" + d).length() - 3);
    }

    private static String pad6(String v) {
        var d = v.replaceAll("\\D", "");
        if (d.length() >= 6) return d.substring(0, 6);
        return ("000000" + d).substring(("000000" + d).length() - 6);
    }

    private static String primeiro(String a, String b) {
        return a != null && !a.isBlank() ? a.trim() : b;
    }
}
