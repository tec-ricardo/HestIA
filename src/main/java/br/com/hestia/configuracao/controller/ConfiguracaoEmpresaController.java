package br.com.hestia.configuracao.controller;

import br.com.hestia.configuracao.dto.*;
import br.com.hestia.configuracao.service.ConfiguracaoEmpresaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/empresas/{empresaId}/configuracoes")
public class ConfiguracaoEmpresaController {
    private final ConfiguracaoEmpresaService service;
    public ConfiguracaoEmpresaController(ConfiguracaoEmpresaService service) { this.service = service; }
    @GetMapping public ConfiguracaoEmpresaResponseDTO consultar(@PathVariable Long empresaId) { return service.consultar(empresaId); }
    @PutMapping public ConfiguracaoEmpresaResponseDTO alterar(@PathVariable Long empresaId,
                                                               @Valid @RequestBody ConfiguracaoEmpresaDTO dto) {
        return service.alterar(empresaId, dto);
    }
}
