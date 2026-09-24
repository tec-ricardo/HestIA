package br.com.hestia.registro.controller;

import br.com.hestia.registro.dto.EficienciaUsoDTO;
import br.com.hestia.registro.dto.RegistroUsoIACriacaoDTO;
import br.com.hestia.registro.dto.RegistroUsoIAResponseDTO;
import br.com.hestia.registro.model.FinalidadeUso;
import br.com.hestia.registro.service.RegistroUsoIAService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/registros-uso-ia")
public class RegistroUsoIAController {

    private final RegistroUsoIAService service;

    public RegistroUsoIAController(
            RegistroUsoIAService service) {

        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroUsoIAResponseDTO criar(
            @Valid
            @RequestBody
            RegistroUsoIACriacaoDTO dto) {

        return service.criar(dto);
    }

    @GetMapping("/{id}")
    public RegistroUsoIAResponseDTO buscar(
            @PathVariable Long id) {

        return service.buscar(id);
    }

    @GetMapping
    public List<RegistroUsoIAResponseDTO> listar(
            @RequestParam(required = false)
            Long usuarioId,

            @RequestParam(required = false)
            Long ferramentaId,

            @RequestParam(required = false)
            FinalidadeUso finalidade,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime dataInicial,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime dataFinal) {

        return service.listar(
                usuarioId,
                ferramentaId,
                finalidade,
                dataInicial,
                dataFinal
        );
    }

    @GetMapping("/eficiencia")
    public List<EficienciaUsoDTO> calcularEficiencia() {

        return service.calcularEficiencia();
    }
}