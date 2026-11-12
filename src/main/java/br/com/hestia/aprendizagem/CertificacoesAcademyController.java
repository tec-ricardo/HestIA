package br.com.hestia.aprendizagem;

import br.com.hestia.usuario.model.Usuario;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Configuração ADMINISTRATIVA das quatro certificações da Academy.
 * Reutiliza as tabelas, a verificação e a concessão já existentes.
 * Nunca concede certificado sem prova aprovada.
 */
@RestController
@RequestMapping("/aprendizagem/academy")
public class CertificacoesAcademyController {
    private final AvaliacaoAprendizagemService a;
    private final AvaliacoesIniciaisHestiaService provas;
    private final CertificacaoService certificacoes;

    public CertificacoesAcademyController(AvaliacaoAprendizagemService a,
                                          AvaliacoesIniciaisHestiaService provas, CertificacaoService certificacoes) {
        this.a = a;
        this.provas = provas;
        this.certificacoes = certificacoes;
    }

    @PostMapping("/certificacoes/inicializar")
    @Transactional
    public Map<String,Object> inicializar(@RequestAttribute("usuarioAutenticado") Usuario usuario) {
        a.acesso().administrador(usuario);
        provas.garantir(usuario);
        Long empresa = a.acesso().empresa(usuario);
        List<String> criadas = new ArrayList<>();
        List<String> atualizadas = new ArrayList<>();
        List<String> existentes = new ArrayList<>();
        String[] nomes = {"Aprendiz", "Consciente", "Guardião", "Embaixador"};

        for (int nivel = 1; nivel <= 4; nivel++) {
            String codigo = "HESTIA_ACADEMY_N" + nivel;
            List<Long> catalogos = new ArrayList<>();
            for (long cursoId = (nivel - 1) * 4L + 1; cursoId <= nivel * 4L; cursoId++) {
                var curso = a.conteudo(cursoId);
                String sha = AcessoCapacitacao.hash(curso.getConteudo());
                var lista = a.catalogos(usuario, cursoId);
                var atual = lista.stream()
                        .filter(x -> sha.equals(String.valueOf(x.get("conteudo_sha256"))))
                        .findFirst().orElseThrow(() ->
                                new IllegalStateException("Prova não publicada para o curso " + curso.getId()));
                catalogos.add(((Number) atual.get("id")).longValue());
            }

            var anteriores = a.jdbc().queryForList(
                    "SELECT versao,requisitos FROM certificacao_definicoes WHERE empresa_id=? AND codigo=? ORDER BY versao DESC LIMIT 1",
                    empresa, codigo);
            int versao = anteriores.isEmpty() ? 1 : ((Number)anteriores.getFirst().get("versao")).intValue() + 1;
            if (!anteriores.isEmpty()) {
                try {
                    JsonNode requisitos = a.json().readTree((String)anteriores.getFirst().get("requisitos"));
                    Set<Long> ids = new HashSet<>();
                    for (JsonNode item : requisitos) {
                        ids.add(item.isIntegralNumber() ? item.asLong() : item.path("catalogoId").asLong());
                    }
                    if (ids.equals(new HashSet<>(catalogos))) {
                        existentes.add(codigo);
                        continue;
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Não foi possível ler requisitos de " + codigo, e);
                }
            }

            certificacoes.publicar(usuario, new CertificacaoService.Publicacao(
                    codigo, "HestIA Academy — " + nomes[nivel - 1], nomes[nivel - 1],
                    versao, List.copyOf(catalogos)));
            if (anteriores.isEmpty()) criadas.add(codigo); else atualizadas.add(codigo);
        }
        return Map.of("criadas", criadas, "atualizadas", atualizadas,
                "jaExistiam", existentes, "observacao",
                "A concessão ocorre somente após aprovação nos quatro cursos exigidos por certificação.");
    }
}