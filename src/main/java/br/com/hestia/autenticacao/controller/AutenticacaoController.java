package br.com.hestia.autenticacao.controller;

import br.com.hestia.autenticacao.dto.LoginDTO;
import br.com.hestia.autenticacao.dto.SessaoResponseDTO;
import br.com.hestia.autenticacao.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {
    private final AutenticacaoService service;
    public AutenticacaoController(AutenticacaoService service) { this.service = service; }

    @PostMapping("/login")
    public SessaoResponseDTO login(@Valid @RequestBody LoginDTO dto) { return service.autenticar(dto); }

    @GetMapping("/sessao")
    public SessaoResponseDTO sessao(@RequestHeader("Authorization") String authorization) {
        var sessao = service.validar(bearer(authorization));
        return service.resposta(null, sessao);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorization) {
        service.logout(bearer(authorization));
    }

    private String bearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        return authorization.substring(7).trim();
    }
}
