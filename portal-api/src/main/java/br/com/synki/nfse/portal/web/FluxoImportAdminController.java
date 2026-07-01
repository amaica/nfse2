package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.service.FluxoDataImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/fluxo")
public class FluxoImportAdminController {

    private final FluxoDataImportService importService;

    public FluxoImportAdminController(FluxoDataImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import")
    public Map<String, Object> importar(@RequestParam(name = "force", defaultValue = "false") boolean force) {
        return importService.importar(force);
    }
}
