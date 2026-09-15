package br.com.hestia.ferramenta.service;

import br.com.hestia.ferramenta.model.FerramentaIA;
import br.com.hestia.ferramenta.model.NivelRiscoIA;
import br.com.hestia.ferramenta.repository.FerramentaIARepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FerramentaIAService {

    private final FerramentaIARepository ferramentaIARepository;

    public FerramentaIAService(FerramentaIARepository ferramentaIARepository) {
        this.ferramentaIARepository = ferramentaIARepository;
    }

    public List<FerramentaIA> listarPorNivelRisco(NivelRiscoIA nivelRisco) {
        return ferramentaIARepository.findByNivelRisco(nivelRisco);
    }
}