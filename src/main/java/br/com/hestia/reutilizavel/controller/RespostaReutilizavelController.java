package br.com.hestia.reutilizavel.controller;

import br.com.hestia.rag.dto.AtualizacaoConteudoDTO;
import br.com.hestia.rag.service.ManutencaoConteudoService;
import br.com.hestia.reutilizavel.dto.EconomiaReutilizacaoDTO;
import br.com.hestia.reutilizavel.dto.IndicadoresQualidadeDTO;
import br.com.hestia.reutilizavel.dto.RespostaReutilizavelDTO;
import br.com.hestia.reutilizavel.dto.SustentabilidadeDTO;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.reutilizavel.service.RespostaReutilizavelService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/respostas-reutilizaveis")
public class RespostaReutilizavelController {

    private final RespostaReutilizavelService service;
    private final AcessoRespostaService acesso;
    private final ManutencaoConteudoService manutencao;

    public RespostaReutilizavelController(
            RespostaReutilizavelService service,
            AcessoRespostaService acesso,
            ManutencaoConteudoService manutencao
    ) {
        this.service = service;
        this.acesso = acesso;
        this.manutencao = manutencao;
    }

    @PutMapping("/{id}/perfis")
    public ResponseEntity<Void> autorizarPerfis(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long id,
            @RequestBody Set<PerfilUsuario> perfis
    ) {
        acesso.autorizarPerfis(usuario, id, perfis);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<RespostaReutilizavel> cadastrar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @Valid @RequestBody RespostaReutilizavelDTO dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(manutencao.cadastrar(dto, usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RespostaReutilizavel> atualizar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long id,
            @Valid @RequestBody AtualizacaoConteudoDTO dto
    ) {
        return ResponseEntity.ok(
                manutencao.atualizar(id, dto, usuario)
        );
    }

    @GetMapping("/{id}/revisao")
    public ResponseEntity<br.com.hestia.reutilizavel.dto.RevisaoRespostaDTO> consultarRevisao(
            @RequestAttribute("usuarioAutenticado") Usuario usuario, @PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(manutencao.consultarRevisao(id, usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retirar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long id,
            @Valid @RequestBody br.com.hestia.reutilizavel.dto.RetiradaRespostaDTO dto
    ) {
        manutencao.retirar(id, usuario, dto.justificativa());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<RespostaReutilizavel>> listarTodas(
            @RequestAttribute("usuarioAutenticado") Usuario usuario
    ) {
        return ResponseEntity.ok(
                acesso.recuperar(
                        usuario, null, null, null,
                        false, Pageable.unpaged()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespostaReutilizavel> buscarPorId(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(acesso.buscarPorId(usuario, id));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<RespostaReutilizavel>> listarPorEmpresa(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long empresaId
    ) {
        return ResponseEntity.ok(
                acesso.recuperar(
                        usuario, empresaId, null, null,
                        false, Pageable.unpaged()
                )
        );
    }

    @GetMapping("/empresa/{empresaId}/reutilizaveis")
    public ResponseEntity<List<RespostaReutilizavel>> listarReutilizaveis(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long empresaId
    ) {
        return ResponseEntity.ok(
                acesso.recuperar(
                        usuario, empresaId, null, null,
                        false, Pageable.unpaged()
                )
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
                        usuario, empresaId, prompt
                )
        );
    }

    @PatchMapping("/{id}/reutilizar")
    public ResponseEntity<RespostaReutilizavel> registrarReutilizacao(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                service.registrarReutilizacao(
                        id, usuario
                )
        );
    }

    @GetMapping("/empresa/{empresaId}/economia")
    public ResponseEntity<EconomiaReutilizacaoDTO> consultarEconomia(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long empresaId
    ) {
        return ResponseEntity.ok(
                service.consultarEconomia(
                        acesso.validarEmpresa(usuario, empresaId)
                )
        );
    }

    @GetMapping("/empresa/{empresaId}/qualidade")
    public ResponseEntity<IndicadoresQualidadeDTO> consultarQualidade(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long empresaId
    ) {
        return ResponseEntity.ok(
                service.consultarQualidade(
                        acesso.validarEmpresa(usuario, empresaId)
                )
        );
    }

    @GetMapping("/empresa/{empresaId}/sustentabilidade")
    public ResponseEntity<SustentabilidadeDTO> consultarSustentabilidade(
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