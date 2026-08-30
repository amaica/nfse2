package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.service.CnpjConsultaPublicaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/cnpj")
public class PublicCnpjController {

    private final CnpjConsultaPublicaService cnpjConsultaPublicaService;

    public PublicCnpjController(CnpjConsultaPublicaService cnpjConsultaPublicaService) {
        this.cnpjConsultaPublicaService = cnpjConsultaPublicaService;
    }

    @GetMapping("/{cnpj}")
    public Map<String, Object> consultar(@PathVariable String cnpj) {
        return cnpjConsultaPublicaService.consultarMapa(cnpj);
    }
}
