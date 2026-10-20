package br.com.hestia.rag.service;

import br.com.hestia.rag.dto.ResultadoReindexacaoDTO;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static br.com.hestia.rag.dto.ResultadoReindexacaoDTO.Status.*;

@Service
public class ReindexacaoService {

    private final BuscaVetorialService busca;
    private final AcessoRespostaService acesso;

    public ReindexacaoService(
            BuscaVetorialService busca,
            AcessoRespostaService acesso
    ) {
        this.busca = busca;
        this.acesso = acesso;
    }

    public List<ResultadoReindexacaoDTO> reindexar(
            Usuario usuario,
            List<Long> respostas
    ) {
        Long empresaId = acesso.validarEmpresa(usuario, null);

        if (respostas == null || respostas.isEmpty()
                || respostas.size() > 20
                || respostas.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe entre 1 e 20 identificadores positivos."
            );
        }

        List<ResultadoReindexacaoDTO> resultados = new ArrayList<>();

        for (Long id : new LinkedHashSet<>(respostas)) {
            try {
                busca.indexarResposta(empresaId, id);

                resultados.add(new ResultadoReindexacaoDTO(id, INDEXADA));
            } catch (ResponseStatusException exception) {
                int codigo = exception.getStatusCode().value();

                if (codigo == 401 || codigo == 403) {
                    throw exception;
                }

                resultados.add(new ResultadoReindexacaoDTO(
                        id,
                        codigo == 409 ? CONFLITO
                                : codigo == 404 ? INDISPONIVEL : FALHA
                ));
            } catch (IllegalArgumentException exception) {
                resultados.add(
                        new ResultadoReindexacaoDTO(id, INDISPONIVEL)
                );
            } catch (RuntimeException exception) {
                resultados.add(new ResultadoReindexacaoDTO(id, FALHA));
            }
        }

        return List.copyOf(resultados);
    }
}