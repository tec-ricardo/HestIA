package br.com.hestia.politica.service;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.politica.dto.PoliticaUsoDTO;
import br.com.hestia.politica.model.PoliticaUso;
import br.com.hestia.politica.repository.PoliticaUsoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PoliticaUsoService {

    private final PoliticaUsoRepository politicaUsoRepository;
    private final EmpresaRepository empresaRepository;

    public PoliticaUsoService(
            PoliticaUsoRepository politicaUsoRepository,
            EmpresaRepository empresaRepository
    ) {
        this.politicaUsoRepository = politicaUsoRepository;
        this.empresaRepository = empresaRepository;
    }

    public PoliticaUso criar(PoliticaUsoDTO dto) {

        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() ->
                        new RuntimeException("Empresa não encontrada.")
                );

        PoliticaUso politica = new PoliticaUso();

        politica.setTitulo(dto.getTitulo());
        politica.setDescricao(dto.getDescricao());
        politica.setConteudo(dto.getConteudo());
        politica.setVersao(dto.getVersao());
        politica.setEmpresa(empresa);

        if (dto.getAtiva() != null) {
            politica.setAtiva(dto.getAtiva());
        }

        return politicaUsoRepository.save(politica);
    }

    public List<PoliticaUso> listarTodas() {
        return politicaUsoRepository.findAll();
    }

    public PoliticaUso buscarPorId(Long id) {
        return politicaUsoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Política de uso não encontrada.")
                );
    }

    public List<PoliticaUso> listarPorEmpresa(Long empresaId) {
        return politicaUsoRepository.findByEmpresaId(empresaId);
    }

    public PoliticaUso atualizar(Long id, PoliticaUsoDTO dto) {

        PoliticaUso politica = buscarPorId(id);

        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() ->
                        new RuntimeException("Empresa não encontrada.")
                );

        politica.setTitulo(dto.getTitulo());
        politica.setDescricao(dto.getDescricao());
        politica.setConteudo(dto.getConteudo());
        politica.setVersao(dto.getVersao());
        politica.setEmpresa(empresa);

        if (dto.getAtiva() != null) {
            politica.setAtiva(dto.getAtiva());
        }

        return politicaUsoRepository.save(politica);
    }

    public void excluir(Long id) {

        PoliticaUso politica = buscarPorId(id);

        politicaUsoRepository.delete(politica);
    }
}