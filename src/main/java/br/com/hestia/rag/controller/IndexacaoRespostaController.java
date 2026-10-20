package br.com.hestia.rag.controller;

import br.com.hestia.rag.service.BuscaVetorialService;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/respostas-reutilizaveis")
public class IndexacaoRespostaController {

    private final BuscaVetorialService busca;
    private final AcessoRespostaService acesso;

    public IndexacaoRespostaController(
            BuscaVetorialService busca,
            AcessoRespostaService acesso
    ) {
        this.busca = busca;
        this.acesso = acesso;
    }

    @PostMapping("/{id}/indexar")
    public ResponseEntity<Void> indexar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @PathVariable Long id
    ) {
        Long empresaId = acesso.validarEmpresa(usuario, null);
        busca.indexarResposta(empresaId, id);

        return ResponseEntity.noContent().build();
    }
}