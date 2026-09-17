package br.com.synki.nfse.portal.service.fiscal;

import com.fincatto.documentofiscal.nfe400.classes.NFNotaInfoImpostoTributacaoIBSCBS;
import com.fincatto.documentofiscal.nfe400.classes.NFNotaInfoImpostoTributacaoIBSCBSClassTrib;
import com.fincatto.documentofiscal.nfe400.classes.NFNotaInfoImpostoTributacaoIS;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catálogo oficial CST / cClassTrib / CST-IS a partir da lib documentofiscal (NT reforma). */
@Service
public class ReformaCatalogoService {

    public List<Map<String, Object>> listarCstIbsCbs() {
        return Arrays.stream(NFNotaInfoImpostoTributacaoIBSCBS.values())
                .map(c -> item(c.getCodigo(), c.getDescricao()))
                .toList();
    }

    public List<Map<String, Object>> listarClassTrib(String cstFiltro) {
        String prefix = cstFiltro == null ? "" : cstFiltro.replaceAll("\\D", "");
        if (prefix.length() > 3) {
            prefix = prefix.substring(0, 3);
        }
        final String p = prefix;
        return Arrays.stream(NFNotaInfoImpostoTributacaoIBSCBSClassTrib.values())
                .filter(c -> p.isBlank() || c.getCodigo().startsWith(p))
                .map(c -> item(c.getCodigo(), c.getDescricao()))
                .toList();
    }

    public List<Map<String, Object>> listarCstIs() {
        return Arrays.stream(NFNotaInfoImpostoTributacaoIS.values())
                .map(c -> item(c.getCodigo(), c.getDescricao()))
                .toList();
    }

    private static Map<String, Object> item(String codigo, String descricao) {
        var m = new LinkedHashMap<String, Object>();
        m.put("codigo", codigo);
        m.put("descricao", descricao);
        m.put("label", codigo + " — " + descricao);
        return m;
    }
}
