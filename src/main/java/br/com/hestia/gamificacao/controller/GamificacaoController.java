package br.com.hestia.gamificacao.controller;
import br.com.hestia.gamificacao.dto.*;
import br.com.hestia.gamificacao.service.GamificacaoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/gamificacao")
public class GamificacaoController {
    private final GamificacaoService service;
    public GamificacaoController(GamificacaoService service) { this.service = service; }
    @PostMapping("/acoes") public PasseHestIAResponseDTO registrar(@Valid @RequestBody RegistroAcaoDTO dto) { return service.registrarAcao(dto); }
    @GetMapping("/usuarios/{usuarioId}/passe") public PasseHestIAResponseDTO passe(@PathVariable Long usuarioId) { return service.passe(usuarioId); }
}
