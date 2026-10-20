
package br.com.hestia.reutilizavel.controller;

import br.com.hestia.reutilizavel.dto.EconomiaReutilizacaoDTO;
import br.com.hestia.reutilizavel.dto.IndicadoresQualidadeDTO;
import br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO;
import br.com.hestia.reutilizavel.dto.SustentabilidadeDTO;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.service.RespostaReutilizavelService;
import jakarta.validation.Valid;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import org.springframework.data.domain.Pageable;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/respostas-reutilizaveis")
public class RespostaReutilizavelController {

    private final RespostaReutilizavelService service;
    private final AcessoRespostaService acesso;

    public RespostaReutilizavelController(
            RespostaReutilizavelService service, AcessoRespostaService acesso
    ) {
        this.service = service;
        this.acesso = acesso;
    }

    @PutMapping("/{id}/perfis")
    public ResponseEntity<Void> autorizarPerfis(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long id,
            @RequestBody Set<br.com.hestia.usuario.model.PerfilUsuario> perfis) {
        acesso.autorizarPerfis(usuario, id, perfis);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<RespostaReutilizavel> cadastrar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @Valid @RequestBody RespostaReutilizavelDTO dto
    ) {

        var resposta = service.cadastrar(dto, usuario);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<RespostaReutilizavel>>
    listarTodas(@RequestAttribute("usuarioAutenticado") Usuario usuario) {

        return ResponseEntity.ok(
                acesso.recuperar(usuario, null, null, null, false, Pageable.unpaged())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespostaReutilizavel>
    buscarPorId(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                acesso.buscarPorId(usuario, id)
        );
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<RespostaReutilizavel>>
    listarPorEmpresa(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                acesso.recuperar(usuario, empresaId, null, null, false, Pageable.unpaged())
        );
    }

    @GetMapping("/empresa/{empresaId}/reutilizaveis")
    public ResponseEntity<List<RespostaReutilizavel>>
    listarReutilizaveis(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                acesso.recuperar(usuario, empresaId, null, null, false, Pageable.unpaged())
        );
    }

    @GetMapping("/empresa/{empresaId}/buscar")
    public ResponseEntity<List<RespostaReutilizavel>>
    buscarRespostasAnteriores(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long empresaId,
            @RequestParam String prompt
    ) {

        return ResponseEntity.ok(
                service.buscarRespostasAnteriores(
                        usuario, empresaId,
                        prompt
                )
        );
    }

    @PatchMapping("/{id}/reutilizar")
    public ResponseEntity<RespostaReutilizavel>
    registrarReutilizacao(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                service.registrarReutilizacao(acesso.buscarPorId(usuario, id).getId())
        );
    }

    @GetMapping("/empresa/{empresaId}/economia")
    public ResponseEntity<EconomiaReutilizacaoDTO>
    consultarEconomia(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                service.consultarEconomia(acesso.validarEmpresa(usuario, empresaId))
        );
    }

    @GetMapping("/empresa/{empresaId}/qualidade")
    public ResponseEntity<IndicadoresQualidadeDTO> consultarQualidade(
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                service.consultarQualidade(empresaId)
        );
    }

    @GetMapping("/empresa/{empresaId}/sustentabilidade")
    public ResponseEntity<SustentabilidadeDTO>
    consultarSustentabilidade(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long empresaId
    ) {

        return ResponseEntity.ok(
                service.consultarSustentabilidade(
                        acesso.validarEmpresa(usuario, empresaId)
                )
        );
    }
}
