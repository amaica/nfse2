package br.com.synki.nfse.portal.web;

import br.com.synki.nfse.portal.service.EmpresaLogoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/empresas/{empresaId}/logo")
public class LogoAdminController {

    private final EmpresaLogoService empresaLogoService;

    public LogoAdminController(EmpresaLogoService empresaLogoService) {
        this.empresaLogoService = empresaLogoService;
    }

    @GetMapping
    public Object status(@PathVariable Long empresaId) {
        return Map.of("cadastrado", empresaLogoService.existe(empresaId));
    }

    @PostMapping(consumes = "multipart/form-data")
    public Object upload(
            @PathVariable Long empresaId,
            @RequestParam("arquivo") MultipartFile arquivo) throws Exception {
        empresaLogoService.salvar(empresaId, arquivo);
        return Map.of("ok", true, "empresaId", empresaId);
    }

    @DeleteMapping
    public Object remover(@PathVariable Long empresaId) throws Exception {
        empresaLogoService.excluir(empresaId);
        return Map.of("ok", true);
    }
}
