package br.com.hestia.ferramenta.model;

import br.com.hestia.empresa.model.Empresa;
import br.com.hestia.empresa.repository.EmpresaRepository;
import br.com.hestia.ferramenta.repository.FerramentaIARepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FerramentaIARepositoryTest {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private FerramentaIARepository ferramentaRepository;

    @Test
    void criaFerramentaComEstadoInicialEIsolamentoPorEmpresa() {
        var empresa = empresaRepository.save(new Empresa(
                null, "ESPM", "61428598000190", null, 1000.0
        ));
        var ferramenta = new FerramentaIA(
                "ChatGPT", "OpenAI", "Assistente corporativo",
                TipoFerramentaIA.CHATBOT, "Apoiar atividades corporativas",
                "https://chatgpt.com", false, empresa
        );

        var salva = ferramentaRepository.saveAndFlush(ferramenta);

        assertThat(salva.getId()).isNotNull();
        assertThat(salva.getStatus()).isEqualTo(StatusFerramentaIA.EM_ANALISE);
        assertThat(salva.getNivelRisco()).isEqualTo(NivelRiscoIA.NAO_AVALIADO);
        assertThat(salva.getDataCadastro()).isNotNull();
        assertThat(ferramentaRepository.findByEmpresaId(empresa.getId()))
                .extracting(FerramentaIA::getNome)
                .containsExactly("ChatGPT");
    }
}
