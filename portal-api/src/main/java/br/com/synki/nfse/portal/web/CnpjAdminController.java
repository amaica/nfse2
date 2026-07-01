package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.service.CnpjConsultaPublicaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/cnpj")
public class CnpjAdminController {

    private final CnpjConsultaPublicaService cnpjConsultaPublicaService;

    public CnpjAdminController(CnpjConsultaPublicaService cnpjConsultaPublicaService) {
        this.cnpjConsultaPublicaService = cnpjConsultaPublicaService;
    }

    @GetMapping("/{cnpj}")
    public Object consultar(@PathVariable String cnpj) {
        return Map.of("ok", true, "dados", cnpjConsultaPublicaService.consultarMapa(cnpj));
    }
}
