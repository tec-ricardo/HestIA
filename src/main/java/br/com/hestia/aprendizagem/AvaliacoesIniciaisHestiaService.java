package br.com.hestia.aprendizagem;

import br.com.hestia.usuario.model.Usuario;
import br.com.hestia.microtreinamento.model.Microtreinamento;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import java.time.LocalDateTime;
import java.util.*;
import static br.com.hestia.aprendizagem.AcessoCapacitacao.hash;
import static br.com.hestia.aprendizagem.ContratoAvaliacao.*;

/** Publica avaliações iniciais compatíveis com a versão atual do conteúdo, sem apagar histórico. */
@Service
public class AvaliacoesIniciaisHestiaService {
    @org.springframework.beans.factory.annotation.Value("${hestia.catalogo.demonstracao:false}")
    private boolean demonstracao;
    private static final Logger log = LoggerFactory.getLogger(AvaliacoesIniciaisHestiaService.class);
    private final AvaliacaoAprendizagemService a;
    private final TransactionTemplate transacao;
    public AvaliacoesIniciaisHestiaService(AvaliacaoAprendizagemService a, PlatformTransactionManager gerenciador) {
        this.a = a;
        this.transacao = new TransactionTemplate(gerenciador);
        this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Publica os 16 questionários em transações separadas para preservar os já publicados. */
    public void garantir(Usuario usuario) {
        if(!demonstracao)return;
        for (long cursoId = 1; cursoId <= 16; cursoId++) {
            garantirCurso(usuario, cursoId);
        }
    }

    public void garantirCurso(Usuario usuario, Long cursoId) {
        if(!demonstracao)return;
        try {
            transacao.executeWithoutResult(status -> publicarSeAusente(usuario, cursoId));
        } catch (RuntimeException erro) {
            log.error("Falha ao publicar prova do curso {}. Verifique o PostgreSQL.", cursoId, erro);
            throw new IllegalStateException("Falha ao preparar a avaliação do curso " + cursoId
                    + ": " + erro.getMessage(), erro);
        }
    }

    private void publicarSeAusente(Usuario usuario, Long cursoId) {
        Long empresa = a.acesso().empresa(usuario);
        Microtreinamento curso = a.conteudo(cursoId);
        String sha = hash(curso.getConteudo());
        Long existentes = a.jdbc().queryForObject(
                "SELECT count(*) FROM aprendizagem_catalogo WHERE empresa_id=? AND treinamento_id=? AND conteudo_sha256=?",
                Long.class, empresa, cursoId, sha);
        if (existentes != null && existentes > 0) return;
        Integer versao = a.jdbc().queryForObject(
                "SELECT COALESCE(MAX(versao),0) FROM aprendizagem_catalogo WHERE empresa_id=? AND treinamento_id=?",
                Integer.class, empresa, cursoId);
        List<Questao> perguntas = questoes(cursoId);
        AvaliacaoAprendizagemService.validarQuestoes(perguntas);
        a.jdbc().update(
                "INSERT INTO aprendizagem_catalogo(empresa_id,treinamento_id,versao,conteudo_sha256,tema,questionario,publicado_por,publicado_em) VALUES(?,?,?,?,?,?,?,?)",
                empresa, cursoId, (versao == null ? 0 : versao) + 1, sha,
                curso.getTema().name(), a.escrever(perguntas), usuario.getId(), LocalDateTime.now());
        log.info("Prova publicada: empresa={} curso={} versão={}", empresa, cursoId, (versao == null ? 0 : versao) + 1);
    }

    private static List<Questao> questoes(Long cursoId) {
        return switch (cursoId.intValue()) {
            case 1 -> List.of(
                    new Questao("H01Q1","Em proteção de dados em ia, qual é a melhor decisão? Uma pessoa quer enviar uma lista com CPF e telefone de clientes para uma IA pública.",Map.of("A", "Não enviar; usar dados mínimos e ambiente autorizado", "B", "Enviar porque a IA é popular", "C", "Enviar somente com a planilha protegida por senha"),"A","Dados pessoais precisam de base legal, finalidade e controles antes do compartilhamento."),
                    new Questao("H01Q2","Considere esta situação: Um relatório contém nomes removidos, mas inclui cidade, cargo e idade. Qual ação é adequada?",Map.of("A", "Reavaliar o risco de reidentificação", "B", "Considerar automaticamente anônimo", "C", "Publicar sem revisão"),"A","Remover nomes não garante anonimização."),
                    new Questao("H01Q3","Qual atitude demonstra aplicação prática do curso \"Proteção de Dados em IA\"?",Map.of("A", "Não enviar; usar dados mínimos e ambiente autorizado", "B", "Enviar somente com a planilha protegida por senha", "C", "Enviar porque a IA é popular"),"A","Dados pessoais precisam de base legal, finalidade e controles antes do compartilhamento."),
                    new Questao("H01Q4","Qual conduta deve ser evitada ao aplicar \"Proteção de Dados em IA\"?",Map.of("A", "Enviar porque a IA é popular", "B", "Não enviar; usar dados mínimos e ambiente autorizado", "C", "Reavaliar o risco de reidentificação"),"A","Dados pessoais precisam de base legal, finalidade e controles antes do compartilhamento."),
                    new Questao("H01Q5","Qual decisão protege melhor a organização no cenário a seguir? Um relatório contém nomes removidos, mas inclui cidade, cargo e idade.",Map.of("A", "Reavaliar o risco de reidentificação", "B", "Publicar sem revisão", "C", "Considerar automaticamente anônimo"),"A","Remover nomes não garante anonimização.")
            );
            case 2 -> List.of(
                    new Questao("H02Q1","Em ferramentas de ia autorizadas, qual é a melhor decisão? A equipe encontra um chatbot novo e quer testá-lo com documentos internos.",Map.of("A", "Solicitar avaliação e autorização antes do uso", "B", "Criar contas pessoais para todos", "C", "Testar com dados reais e pedir autorização depois"),"A","Ferramentas novas devem passar por avaliação de segurança e privacidade."),
                    new Questao("H02Q2","Considere esta situação: Uma extensão pede acesso irrestrito aos arquivos corporativos. Qual ação é adequada?",Map.of("A", "Revisar permissões e consultar a área responsável", "B", "Aceitar para economizar tempo", "C", "Compartilhar credenciais com a equipe"),"A","Privilégio mínimo reduz riscos."),
                    new Questao("H02Q3","Qual atitude demonstra aplicação prática do curso \"Ferramentas de IA Autorizadas\"?",Map.of("A", "Solicitar avaliação e autorização antes do uso", "B", "Testar com dados reais e pedir autorização depois", "C", "Criar contas pessoais para todos"),"A","Ferramentas novas devem passar por avaliação de segurança e privacidade."),
                    new Questao("H02Q4","Qual conduta deve ser evitada ao aplicar \"Ferramentas de IA Autorizadas\"?",Map.of("A", "Criar contas pessoais para todos", "B", "Solicitar avaliação e autorização antes do uso", "C", "Revisar permissões e consultar a área responsável"),"A","Ferramentas novas devem passar por avaliação de segurança e privacidade."),
                    new Questao("H02Q5","Qual decisão protege melhor a organização no cenário a seguir? Uma extensão pede acesso irrestrito aos arquivos corporativos.",Map.of("A", "Revisar permissões e consultar a área responsável", "B", "Compartilhar credenciais com a equipe", "C", "Aceitar para economizar tempo"),"A","Privilégio mínimo reduz riscos.")
            );
            case 3 -> List.of(
                    new Questao("H03Q1","Em políticas internas para uso de ia, qual é a melhor decisão? Um gestor pede para usar IA em atividade expressamente proibida.",Map.of("A", "Explicar a restrição e buscar alternativa autorizada", "B", "Obedecer sem registrar", "C", "Usar conta pessoal"),"A","Políticas internas também se aplicam a demandas urgentes."),
                    new Questao("H03Q2","Considere esta situação: A política exige registrar a ferramenta e a finalidade. Qual ação é adequada?",Map.of("A", "Registrar os campos exigidos com precisão", "B", "Registrar apenas quando houver erro", "C", "Omitir para preservar agilidade"),"A","Rastreabilidade faz parte da governança."),
                    new Questao("H03Q3","Qual atitude demonstra aplicação prática do curso \"Políticas Internas para Uso de IA\"?",Map.of("A", "Explicar a restrição e buscar alternativa autorizada", "B", "Usar conta pessoal", "C", "Obedecer sem registrar"),"A","Políticas internas também se aplicam a demandas urgentes."),
                    new Questao("H03Q4","Qual conduta deve ser evitada ao aplicar \"Políticas Internas para Uso de IA\"?",Map.of("A", "Obedecer sem registrar", "B", "Explicar a restrição e buscar alternativa autorizada", "C", "Registrar os campos exigidos com precisão"),"A","Políticas internas também se aplicam a demandas urgentes."),
                    new Questao("H03Q5","Qual decisão protege melhor a organização no cenário a seguir? A política exige registrar a ferramenta e a finalidade.",Map.of("A", "Registrar os campos exigidos com precisão", "B", "Omitir para preservar agilidade", "C", "Registrar apenas quando houver erro"),"A","Rastreabilidade faz parte da governança.")
            );
            case 4 -> List.of(
                    new Questao("H04Q1","Em uso responsável da inteligência artificial, qual é a melhor decisão? Uma IA recomenda treinamentos com base em histórico incompleto.",Map.of("A", "Revisar dados, impactos e permitir contestação", "B", "Aplicar automaticamente a todos", "C", "Excluir pessoas sem histórico"),"A","A recomendação precisa ser justa e revisável."),
                    new Questao("H04Q2","Considere esta situação: Uma decisão afeta pessoas e tem alto impacto. Qual ação é adequada?",Map.of("A", "Aplicar supervisão humana proporcional ao risco", "B", "Confiar na fluência do modelo", "C", "Ocultar o uso de IA"),"A","Responsabilidade não é transferida ao modelo."),
                    new Questao("H04Q3","Qual atitude demonstra aplicação prática do curso \"Uso Responsável da Inteligência Artificial\"?",Map.of("A", "Revisar dados, impactos e permitir contestação", "B", "Excluir pessoas sem histórico", "C", "Aplicar automaticamente a todos"),"A","A recomendação precisa ser justa e revisável."),
                    new Questao("H04Q4","Qual conduta deve ser evitada ao aplicar \"Uso Responsável da Inteligência Artificial\"?",Map.of("A", "Aplicar automaticamente a todos", "B", "Revisar dados, impactos e permitir contestação", "C", "Aplicar supervisão humana proporcional ao risco"),"A","A recomendação precisa ser justa e revisável."),
                    new Questao("H04Q5","Qual decisão protege melhor a organização no cenário a seguir? Uma decisão afeta pessoas e tem alto impacto.",Map.of("A", "Aplicar supervisão humana proporcional ao risco", "B", "Ocultar o uso de IA", "C", "Confiar na fluência do modelo"),"A","Responsabilidade não é transferida ao modelo.")
            );
            case 5 -> List.of(
                    new Questao("H05Q1","Em validação humana de respostas de ia, qual é a melhor decisão? Uma IA afirma que houve aumento de produtividade de 35%.",Map.of("A", "Conferir dados, cálculo e período na fonte", "B", "Publicar porque o texto parece técnico", "C", "Arredondar para 40%"),"A","Números gerados por IA exigem validação."),
                    new Questao("H05Q2","Considere esta situação: A resposta cita uma norma inexistente. Qual ação é adequada?",Map.of("A", "Verificar fontes oficiais e corrigir a informação", "B", "Manter a referência para dar credibilidade", "C", "Pedir à IA para repetir a citação"),"A","Uma referência plausível pode ser inventada."),
                    new Questao("H05Q3","Qual atitude demonstra aplicação prática do curso \"Validação Humana de Respostas de IA\"?",Map.of("A", "Conferir dados, cálculo e período na fonte", "B", "Arredondar para 40%", "C", "Publicar porque o texto parece técnico"),"A","Números gerados por IA exigem validação."),
                    new Questao("H05Q4","Qual conduta deve ser evitada ao aplicar \"Validação Humana de Respostas de IA\"?",Map.of("A", "Publicar porque o texto parece técnico", "B", "Conferir dados, cálculo e período na fonte", "C", "Verificar fontes oficiais e corrigir a informação"),"A","Números gerados por IA exigem validação."),
                    new Questao("H05Q5","Qual decisão protege melhor a organização no cenário a seguir? A resposta cita uma norma inexistente.",Map.of("A", "Verificar fontes oficiais e corrigir a informação", "B", "Pedir à IA para repetir a citação", "C", "Manter a referência para dar credibilidade"),"A","Uma referência plausível pode ser inventada.")
            );
            case 6 -> List.of(
                    new Questao("H06Q1","Em registro e auditoria do uso de ia, qual é a melhor decisão? Uma equipe usa IA para resumir um relatório corporativo.",Map.of("A", "Registrar finalidade, ferramenta e revisão conforme política", "B", "Guardar senhas no registro", "C", "Não registrar porque foi um resumo"),"A","Registros sustentam auditoria e prestação de contas."),
                    new Questao("H06Q2","Considere esta situação: Um log contém um token secreto. Qual ação é adequada?",Map.of("A", "Remover o segredo e usar referência segura", "B", "Anexar o log integral ao chamado público", "C", "Enviar o token por chat"),"A","Rastreabilidade não justifica expor segredos."),
                    new Questao("H06Q3","Qual atitude demonstra aplicação prática do curso \"Registro e Auditoria do Uso de IA\"?",Map.of("A", "Registrar finalidade, ferramenta e revisão conforme política", "B", "Não registrar porque foi um resumo", "C", "Guardar senhas no registro"),"A","Registros sustentam auditoria e prestação de contas."),
                    new Questao("H06Q4","Qual conduta deve ser evitada ao aplicar \"Registro e Auditoria do Uso de IA\"?",Map.of("A", "Guardar senhas no registro", "B", "Registrar finalidade, ferramenta e revisão conforme política", "C", "Remover o segredo e usar referência segura"),"A","Registros sustentam auditoria e prestação de contas."),
                    new Questao("H06Q5","Qual decisão protege melhor a organização no cenário a seguir? Um log contém um token secreto.",Map.of("A", "Remover o segredo e usar referência segura", "B", "Enviar o token por chat", "C", "Anexar o log integral ao chamado público"),"A","Rastreabilidade não justifica expor segredos.")
            );
            case 7 -> List.of(
                    new Questao("H07Q1","Em lgpd aplicada ao uso de inteligência artificial, qual é a melhor decisão? Uma empresa quer usar dados pessoais para uma finalidade nova com IA.",Map.of("A", "Avaliar base legal, finalidade e necessidade", "B", "Usar livremente porque já possui os dados", "C", "Publicar os dados primeiro"),"A","O tratamento precisa atender à LGPD."),
                    new Questao("H07Q2","Considere esta situação: Uma pessoa solicita informação sobre o uso dos seus dados. Qual ação é adequada?",Map.of("A", "Encaminhar pelo processo de direitos dos titulares", "B", "Ignorar porque envolve IA", "C", "Excluir os registros de auditoria"),"A","Direitos dos titulares continuam aplicáveis."),
                    new Questao("H07Q3","Qual atitude demonstra aplicação prática do curso \"LGPD Aplicada ao Uso de Inteligência Artificial\"?",Map.of("A", "Avaliar base legal, finalidade e necessidade", "B", "Publicar os dados primeiro", "C", "Usar livremente porque já possui os dados"),"A","O tratamento precisa atender à LGPD."),
                    new Questao("H07Q4","Qual conduta deve ser evitada ao aplicar \"LGPD Aplicada ao Uso de Inteligência Artificial\"?",Map.of("A", "Usar livremente porque já possui os dados", "B", "Avaliar base legal, finalidade e necessidade", "C", "Encaminhar pelo processo de direitos dos titulares"),"A","O tratamento precisa atender à LGPD."),
                    new Questao("H07Q5","Qual decisão protege melhor a organização no cenário a seguir? Uma pessoa solicita informação sobre o uso dos seus dados.",Map.of("A", "Encaminhar pelo processo de direitos dos titulares", "B", "Excluir os registros de auditoria", "C", "Ignorar porque envolve IA"),"A","Direitos dos titulares continuam aplicáveis.")
            );
            case 8 -> List.of(
                    new Questao("H08Q1","Em dados sensíveis em prompts, qual é a melhor decisão? Um prompt contém informações de saúde de colaboradores.",Map.of("A", "Retirar dados identificáveis e consultar autorização", "B", "Enviar para melhorar a resposta", "C", "Trocar só o primeiro nome"),"A","Dados sensíveis exigem proteção reforçada."),
                    new Questao("H08Q2","Considere esta situação: Um exemplo usa identificadores fictícios e dados sintéticos. Qual ação é adequada?",Map.of("A", "Preferir o exemplo sintético em ambiente autorizado", "B", "Acrescentar CPFs reais", "C", "Copiar a base integral"),"A","Dados sintéticos reduzem exposição."),
                    new Questao("H08Q3","Qual atitude demonstra aplicação prática do curso \"Dados sensíveis em prompts\"?",Map.of("A", "Retirar dados identificáveis e consultar autorização", "B", "Trocar só o primeiro nome", "C", "Enviar para melhorar a resposta"),"A","Dados sensíveis exigem proteção reforçada."),
                    new Questao("H08Q4","Qual conduta deve ser evitada ao aplicar \"Dados sensíveis em prompts\"?",Map.of("A", "Enviar para melhorar a resposta", "B", "Retirar dados identificáveis e consultar autorização", "C", "Preferir o exemplo sintético em ambiente autorizado"),"A","Dados sensíveis exigem proteção reforçada."),
                    new Questao("H08Q5","Qual decisão protege melhor a organização no cenário a seguir? Um exemplo usa identificadores fictícios e dados sintéticos.",Map.of("A", "Preferir o exemplo sintético em ambiente autorizado", "B", "Copiar a base integral", "C", "Acrescentar CPFs reais"),"A","Dados sintéticos reduzem exposição.")
            );
            case 9 -> List.of(
                    new Questao("H09Q1","Em alucinações e verificação de respostas, qual é a melhor decisão? A IA inventa um link para uma política interna.",Map.of("A", "Consultar o repositório oficial", "B", "Citar o link sem abrir", "C", "Pedir mais confiança na resposta"),"A","Fluência não prova veracidade."),
                    new Questao("H09Q2","Considere esta situação: Um resumo contém afirmações sem fonte. Qual ação é adequada?",Map.of("A", "Separar fatos verificados de hipóteses", "B", "Tratar tudo como confirmado", "C", "Eliminar a revisão humana"),"A","Evidências precisam ser rastreáveis."),
                    new Questao("H09Q3","Qual atitude demonstra aplicação prática do curso \"Alucinações e verificação de respostas\"?",Map.of("A", "Consultar o repositório oficial", "B", "Pedir mais confiança na resposta", "C", "Citar o link sem abrir"),"A","Fluência não prova veracidade."),
                    new Questao("H09Q4","Qual conduta deve ser evitada ao aplicar \"Alucinações e verificação de respostas\"?",Map.of("A", "Citar o link sem abrir", "B", "Consultar o repositório oficial", "C", "Separar fatos verificados de hipóteses"),"A","Fluência não prova veracidade."),
                    new Questao("H09Q5","Qual decisão protege melhor a organização no cenário a seguir? Um resumo contém afirmações sem fonte.",Map.of("A", "Separar fatos verificados de hipóteses", "B", "Eliminar a revisão humana", "C", "Tratar tudo como confirmado"),"A","Evidências precisam ser rastreáveis.")
            );
            case 10 -> List.of(
                    new Questao("H10Q1","Em engenharia de prompts responsável, qual é a melhor decisão? Uma instrução pede um relatório sem definir público ou formato.",Map.of("A", "Explicitar objetivo, contexto permitido e formato", "B", "Adicionar dados confidenciais para detalhar", "C", "Deixar todas as escolhas implícitas"),"A","Prompts claros reduzem ambiguidades."),
                    new Questao("H10Q2","Considere esta situação: O modelo não possui fontes para responder com segurança. Qual ação é adequada?",Map.of("A", "Solicitar que sinalize lacunas e incertezas", "B", "Exigir referências inventadas", "C", "Omitir limitações"),"A","É correto reconhecer incerteza."),
                    new Questao("H10Q3","Qual atitude demonstra aplicação prática do curso \"Engenharia de prompts responsável\"?",Map.of("A", "Explicitar objetivo, contexto permitido e formato", "B", "Deixar todas as escolhas implícitas", "C", "Adicionar dados confidenciais para detalhar"),"A","Prompts claros reduzem ambiguidades."),
                    new Questao("H10Q4","Qual conduta deve ser evitada ao aplicar \"Engenharia de prompts responsável\"?",Map.of("A", "Adicionar dados confidenciais para detalhar", "B", "Explicitar objetivo, contexto permitido e formato", "C", "Solicitar que sinalize lacunas e incertezas"),"A","Prompts claros reduzem ambiguidades."),
                    new Questao("H10Q5","Qual decisão protege melhor a organização no cenário a seguir? O modelo não possui fontes para responder com segurança.",Map.of("A", "Solicitar que sinalize lacunas e incertezas", "B", "Omitir limitações", "C", "Exigir referências inventadas"),"A","É correto reconhecer incerteza.")
            );
            case 11 -> List.of(
                    new Questao("H11Q1","Em prevenção de vazamento de informações, qual é a melhor decisão? Um desenvolvedor colou uma chave de API em um assistente.",Map.of("A", "Revogar a chave e reportar o incidente", "B", "Apagar a mensagem e não contar", "C", "Reutilizar a chave em outro sistema"),"A","Segredos expostos devem ser revogados."),
                    new Questao("H11Q2","Considere esta situação: Um plugin pede permissão de leitura de todo o drive. Qual ação é adequada?",Map.of("A", "Avaliar necessidade e aplicar acesso mínimo", "B", "Aceitar todas as permissões", "C", "Compartilhar acesso de administrador"),"A","Conectores devem usar privilégio mínimo."),
                    new Questao("H11Q3","Qual atitude demonstra aplicação prática do curso \"Prevenção de vazamento de informações\"?",Map.of("A", "Revogar a chave e reportar o incidente", "B", "Reutilizar a chave em outro sistema", "C", "Apagar a mensagem e não contar"),"A","Segredos expostos devem ser revogados."),
                    new Questao("H11Q4","Qual conduta deve ser evitada ao aplicar \"Prevenção de vazamento de informações\"?",Map.of("A", "Apagar a mensagem e não contar", "B", "Revogar a chave e reportar o incidente", "C", "Avaliar necessidade e aplicar acesso mínimo"),"A","Segredos expostos devem ser revogados."),
                    new Questao("H11Q5","Qual decisão protege melhor a organização no cenário a seguir? Um plugin pede permissão de leitura de todo o drive.",Map.of("A", "Avaliar necessidade e aplicar acesso mínimo", "B", "Compartilhar acesso de administrador", "C", "Aceitar todas as permissões"),"A","Conectores devem usar privilégio mínimo.")
            );
            case 12 -> List.of(
                    new Questao("H12Q1","Em viés algorítmico e decisões justas, qual é a melhor decisão? Um recomendador oferece menos cursos a colaboradores de um grupo.",Map.of("A", "Investigar dados, cobertura e critérios", "B", "Concluir que a IA é neutra", "C", "Ignorar diferenças entre grupos"),"A","Desigualdades podem refletir vieses."),
                    new Questao("H12Q2","Considere esta situação: Um colaborador contesta uma recomendação. Qual ação é adequada?",Map.of("A", "Oferecer revisão humana e registrar o caso", "B", "Negar qualquer contestação", "C", "Excluir o histórico da pessoa"),"A","Decisões precisam ser contestáveis."),
                    new Questao("H12Q3","Qual atitude demonstra aplicação prática do curso \"Viés algorítmico e decisões justas\"?",Map.of("A", "Investigar dados, cobertura e critérios", "B", "Ignorar diferenças entre grupos", "C", "Concluir que a IA é neutra"),"A","Desigualdades podem refletir vieses."),
                    new Questao("H12Q4","Qual conduta deve ser evitada ao aplicar \"Viés algorítmico e decisões justas\"?",Map.of("A", "Concluir que a IA é neutra", "B", "Investigar dados, cobertura e critérios", "C", "Oferecer revisão humana e registrar o caso"),"A","Desigualdades podem refletir vieses."),
                    new Questao("H12Q5","Qual decisão protege melhor a organização no cenário a seguir? Um colaborador contesta uma recomendação.",Map.of("A", "Oferecer revisão humana e registrar o caso", "B", "Excluir o histórico da pessoa", "C", "Negar qualquer contestação"),"A","Decisões precisam ser contestáveis.")
            );
            case 13 -> List.of(
                    new Questao("H13Q1","Em resposta a incidentes de ia, qual é a melhor decisão? Uma resposta de IA expõe informação restrita.",Map.of("A", "Conter a exposição e acionar o canal de incidentes", "B", "Divulgar o conteúdo para pedir ajuda", "C", "Esperar o problema desaparecer"),"A","Contenção e comunicação são prioridades."),
                    new Questao("H13Q2","Considere esta situação: Após um incidente, a equipe quer evitar recorrência. Qual ação é adequada?",Map.of("A", "Analisar causa e definir ações verificáveis", "B", "Encerrar sem documentação", "C", "Culpar automaticamente o usuário"),"A","Aprendizado exige análise e acompanhamento."),
                    new Questao("H13Q3","Qual atitude demonstra aplicação prática do curso \"Resposta a incidentes de IA\"?",Map.of("A", "Conter a exposição e acionar o canal de incidentes", "B", "Esperar o problema desaparecer", "C", "Divulgar o conteúdo para pedir ajuda"),"A","Contenção e comunicação são prioridades."),
                    new Questao("H13Q4","Qual conduta deve ser evitada ao aplicar \"Resposta a incidentes de IA\"?",Map.of("A", "Divulgar o conteúdo para pedir ajuda", "B", "Conter a exposição e acionar o canal de incidentes", "C", "Analisar causa e definir ações verificáveis"),"A","Contenção e comunicação são prioridades."),
                    new Questao("H13Q5","Qual decisão protege melhor a organização no cenário a seguir? Após um incidente, a equipe quer evitar recorrência.",Map.of("A", "Analisar causa e definir ações verificáveis", "B", "Culpar automaticamente o usuário", "C", "Encerrar sem documentação"),"A","Aprendizado exige análise e acompanhamento.")
            );
            case 14 -> List.of(
                    new Questao("H14Q1","Em rastreabilidade e evidências de auditoria, qual é a melhor decisão? Uma recomendação foi tomada sem registrar sua fonte.",Map.of("A", "Documentar origem, critérios e revisão", "B", "Criar justificativa fictícia", "C", "Excluir a decisão do histórico"),"A","Evidências permitem reconstruir decisões."),
                    new Questao("H14Q2","Considere esta situação: A equipe coleta logs com dados desnecessários. Qual ação é adequada?",Map.of("A", "Aplicar minimização e controle de acesso", "B", "Guardar tudo indefinidamente", "C", "Publicar logs em pasta aberta"),"A","Auditoria deve respeitar privacidade."),
                    new Questao("H14Q3","Qual atitude demonstra aplicação prática do curso \"Rastreabilidade e evidências de auditoria\"?",Map.of("A", "Documentar origem, critérios e revisão", "B", "Excluir a decisão do histórico", "C", "Criar justificativa fictícia"),"A","Evidências permitem reconstruir decisões."),
                    new Questao("H14Q4","Qual conduta deve ser evitada ao aplicar \"Rastreabilidade e evidências de auditoria\"?",Map.of("A", "Criar justificativa fictícia", "B", "Documentar origem, critérios e revisão", "C", "Aplicar minimização e controle de acesso"),"A","Evidências permitem reconstruir decisões."),
                    new Questao("H14Q5","Qual decisão protege melhor a organização no cenário a seguir? A equipe coleta logs com dados desnecessários.",Map.of("A", "Aplicar minimização e controle de acesso", "B", "Publicar logs em pasta aberta", "C", "Guardar tudo indefinidamente"),"A","Auditoria deve respeitar privacidade.")
            );
            case 15 -> List.of(
                    new Questao("H15Q1","Em avaliação de riscos de ferramentas de ia, qual é a melhor decisão? Um plugin solicita acesso a arquivos corporativos sem justificativa.",Map.of("A", "Avaliar impacto, probabilidade e necessidade", "B", "Aprovar por conveniência", "C", "Ignorar as permissões"),"A","Adoção deve ser baseada em avaliação de risco."),
                    new Questao("H15Q2","Considere esta situação: Um risco relevante foi identificado. Qual ação é adequada?",Map.of("A", "Definir controle, responsável e reavaliação", "B", "Deixar o risco sem dono", "C", "Registrar apenas depois do incidente"),"A","Riscos precisam de tratamento documentado."),
                    new Questao("H15Q3","Qual atitude demonstra aplicação prática do curso \"Avaliação de riscos de ferramentas de IA\"?",Map.of("A", "Avaliar impacto, probabilidade e necessidade", "B", "Ignorar as permissões", "C", "Aprovar por conveniência"),"A","Adoção deve ser baseada em avaliação de risco."),
                    new Questao("H15Q4","Qual conduta deve ser evitada ao aplicar \"Avaliação de riscos de ferramentas de IA\"?",Map.of("A", "Aprovar por conveniência", "B", "Avaliar impacto, probabilidade e necessidade", "C", "Definir controle, responsável e reavaliação"),"A","Adoção deve ser baseada em avaliação de risco."),
                    new Questao("H15Q5","Qual decisão protege melhor a organização no cenário a seguir? Um risco relevante foi identificado.",Map.of("A", "Definir controle, responsável e reavaliação", "B", "Registrar apenas depois do incidente", "C", "Deixar o risco sem dono"),"A","Riscos precisam de tratamento documentado.")
            );
            case 16 -> List.of(
                    new Questao("H16Q1","Em liderança em ia responsável, qual é a melhor decisão? Uma liderança quer automatizar respostas a clientes sem revisão.",Map.of("A", "Definir supervisão e critérios de qualidade", "B", "Automatizar tudo imediatamente", "C", "Ocultar a IA dos clientes"),"A","Liderança deve promover controles adequados."),
                    new Questao("H16Q2","Considere esta situação: A equipe teme relatar erros de IA. Qual ação é adequada?",Map.of("A", "Incentivar reporte e aprendizado sem ocultação", "B", "Punir toda comunicação de falha", "C", "Desestimular revisão"),"A","Cultura de segurança depende de transparência."),
                    new Questao("H16Q3","Qual atitude demonstra aplicação prática do curso \"Liderança em IA responsável\"?",Map.of("A", "Definir supervisão e critérios de qualidade", "B", "Ocultar a IA dos clientes", "C", "Automatizar tudo imediatamente"),"A","Liderança deve promover controles adequados."),
                    new Questao("H16Q4","Qual conduta deve ser evitada ao aplicar \"Liderança em IA responsável\"?",Map.of("A", "Automatizar tudo imediatamente", "B", "Definir supervisão e critérios de qualidade", "C", "Incentivar reporte e aprendizado sem ocultação"),"A","Liderança deve promover controles adequados."),
                    new Questao("H16Q5","Qual decisão protege melhor a organização no cenário a seguir? A equipe teme relatar erros de IA.",Map.of("A", "Incentivar reporte e aprendizado sem ocultação", "B", "Desestimular revisão", "C", "Punir toda comunicação de falha"),"A","Cultura de segurança depende de transparência.")
            );
            default -> List.of();
        };
    }
}