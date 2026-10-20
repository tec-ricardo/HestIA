package br.com.hestia.autenticacao.controller;

import br.com.hestia.autenticacao.dto.RecuperacaoSenhaDTO;
import br.com.hestia.autenticacao.dto.RecuperacaoSenhaResponseDTO;
import br.com.hestia.autenticacao.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/recuperacao")
public class RecuperacaoSenhaController {
    private final RecuperacaoSenhaService service;
    public RecuperacaoSenhaController(RecuperacaoSenhaService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<RecuperacaoSenhaResponseDTO> solicitar(@Valid @RequestBody RecuperacaoSenhaDTO dto) {
        service.solicitar(dto.email());
        return ResponseEntity.ok(new RecuperacaoSenhaResponseDTO(
                "Se o e-mail estiver cadastrado, você receberá as instruções de recuperação."));
    }

    @PostMapping("/redefinir")
    public ResponseEntity<Void> redefinir(@RequestParam @NotBlank String token,
                                          @RequestParam @NotBlank @Size(min = 8, max = 100) String novaSenha) {
        service.redefinir(token, novaSenha);
        return ResponseEntity.noContent().build();
    }
}