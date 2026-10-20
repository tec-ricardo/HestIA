package br.com.hestia.rag.controller;

import br.com.hestia.rag.dto.BuscaSemelhanteDTO;
import br.com.hestia.rag.service.BuscaVetorialService;
import br.com.hestia.reutilizavel.model.RespostaReutilizavel;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/respostas-reutilizaveis")
public class BuscaSemelhanteController {

    private final BuscaVetorialService busca;
    private final AcessoRespostaService acesso;

    public BuscaSemelhanteController(
            BuscaVetorialService busca,
            AcessoRespostaService acesso
    ) {
        this.busca = busca;
        this.acesso = acesso;
    }

    @PostMapping("/buscar-semelhantes")
    public ResponseEntity<List<RespostaReutilizavel>> buscar(
            @RequestAttribute("usuarioAutenticado") Usuario usuario,
            @Valid @RequestBody BuscaSemelhanteDTO dto
    ) {
        Long empresaId = acesso.validarEmpresa(usuario, null);

        int limite = dto.limite() == null ? 5 : dto.limite();
        double similaridade = dto.similaridadeMinima() == null
                ? 0.70
                : dto.similaridadeMinima();

        return ResponseEntity.ok(
                busca.buscarSemelhantes(
                        empresaId,
                        dto.pergunta(),
                        limite,
                        similaridade
                )
        );
    }
}