package br.com.hestia.microtreinamento.service;

import br.com.hestia.microtreinamento.dto.OcorrenciaErroDTO;
import br.com.hestia.microtreinamento.model.TipoErro;
import br.com.hestia.microtreinamento.repository.ConclusaoMicrotreinamentoRepository;
import br.com.hestia.reutilizavel.service.AcessoRespostaService;
import br.com.hestia.usuario.model.PerfilUsuario;
import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecorrenciaCapacitacaoService {

    private final ConclusaoMicrotreinamentoRepository conclusoes;
    private final UsuarioRepository usuarios;
    private final AcessoRespostaService acesso;
    private final RecorrenciaErroService recorrencia;

    private final MicrotreinamentoFicticioService catalogo =
            new MicrotreinamentoFicticioService();

    public RecorrenciaCapacitacaoService(
            ConclusaoMicrotreinamentoRepository conclusoes,
            UsuarioRepository usuarios,
            AcessoRespostaService acesso,
            RecorrenciaErroService recorrencia
    ) {
        this.conclusoes = conclusoes;
        this.usuarios = usuarios;
        this.acesso = acesso;
        this.recorrencia = recorrencia;
    }

    @Transactional(readOnly = true)
    public Resultado consultar(
            Usuario autenticado,
            Long conclusaoId,
            TipoErro tipoErro,
            LocalDateTime observadoAte,
            List<OcorrenciaErroDTO> ocorrencias
    ) {
        Long empresaId = acesso.validarEmpresa(autenticado, null);

        if (conclusaoId == null || conclusaoId <= 0
                || tipoErro == null || observadoAte == null
                || ocorrencias == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Parâmetros de acompanhamento inválidos."
            );
        }

        var conclusao = conclusoes.findById(conclusaoId)
                .orElseThrow(this::indisponivel);

        if (conclusao.getUsuarioId() == null) {
            throw indisponivel();
        }

        var participante = usuarios.findById(conclusao.getUsuarioId())
                .filter(usuario -> usuario.getEmpresa() != null
                        && empresaId.equals(
                        usuario.getEmpresa().getId()
                ))
                .orElseThrow(this::indisponivel);

        if (autenticado.getPerfil() == PerfilUsuario.FUNCIONARIO
                && !autenticado.getId().equals(participante.getId())) {
            throw indisponivel();
        }

        if (!conclusao.isConcluido()
                || conclusao.getDataConclusao() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A capacitação ainda não foi concluída."
            );
        }

        var treinamento = catalogo.criarTreinamentos().stream()
                .filter(item -> item.getId().equals(
                        conclusao.getMicrotreinamentoId()
                ))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Microtreinamento não localizado no catálogo."
                ));

        if (treinamento.getTema() != tipoErro.getTemaTreinamento()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O tipo de erro não corresponde ao tema da capacitação."
            );
        }

        if (observadoAte.isBefore(conclusao.getDataConclusao())
                || observadoAte.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data de observação deve estar entre a conclusão e o momento atual."
            );
        }

        RecorrenciaErroService.Resultado calculo;

        try {
            calculo = recorrencia.calcular(
                    empresaId,
                    participante.getId(),
                    tipoErro,
                    conclusao.getDataConclusao(),
                    observadoAte,
                    ocorrencias
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }

        return new Resultado(
                conclusaoId,
                treinamento.getId(),
                treinamento.getTitulo(),
                calculo
        );
    }

    private ResponseStatusException indisponivel() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Conclusão não encontrada ou indisponível para este acesso."
        );
    }

    public record Resultado(
            Long conclusaoId,
            Long microtreinamentoId,
            String tituloMicrotreinamento,
            RecorrenciaErroService.Resultado recorrencia
    ) {
    }
}