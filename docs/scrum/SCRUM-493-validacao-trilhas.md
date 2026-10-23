# SCRUM-493 — Validação de acionamento e conclusão das trilhas

## Resultado

O motor de trilhas corretivas e adaptativas foi validado contra as regras da SCRUM-488. A validação encontrou e corrigiu a reincidência posterior à capacitação: depois de uma trilha concluída, a primeira ocorrência nova do mesmo tema agora cria um ciclo vinculado ao anterior.

## Comportamentos validados

- Duas ocorrências de um erro sujeito à recorrência não abrem trilha.
- A terceira ocorrência distinta dentro da janela abre uma trilha corretiva.
- Reprocessar os mesmos eventos não duplica evidências nem ciclos.
- Eventos adicionais do mesmo tema são associados ao ciclo que ainda está ativo.
- Uma ocorrência posterior à conclusão cria novo ciclo e preserva o vínculo com o ciclo anterior.
- Eventos anteriores à conclusão não reabrem a trilha.
- Tema sem conteúdo e avaliação compatíveis registra pendência de catálogo e não cria trilha vazia.
- Depois da publicação do catálogo compatível, o evento pendente pode originar a trilha.
- Reprovação em avaliação abre trilha adaptativa.
- A trilha só é concluída após a conclusão e a aprovação exigidas para seus conteúdos.
- O histórico de aprendizagem e o histórico de ciclos permanecem preservados.

## Correção aplicada

Antes, erros como ausência de validação humana exigiam outras três ocorrências mesmo depois de o funcionário concluir a capacitação. Isso contrariava a regra de reincidência da SCRUM-488.

O motor agora diferencia:

- primeiro acionamento por recorrência: exige três ocorrências do mesmo tipo na janela;
- ciclo ainda ativo: agrega evidências novas sem duplicar a trilha;
- reincidência após conclusão: uma ocorrência nova e posterior à conclusão abre outro ciclo;
- reprocessamento de evento antigo: não abre ciclo.

O novo ciclo registra `anterior_id` e `corte_aprendizagem`, permitindo reconstruir por que ele foi aberto sem apagar a capacitação anterior.

## Como conferir manualmente

1. Publique a avaliação do microtreinamento correspondente ao tema.
2. Registre duas ocorrências de ausência de validação humana e processe o usuário: nenhuma trilha deve aparecer.
3. Registre a terceira ocorrência: uma trilha corretiva deve aparecer.
4. Processe novamente sem novos eventos: deve continuar existindo apenas um ciclo.
5. Conclua e seja aprovado em todas as etapas obrigatórias.
6. Registre uma ocorrência nova do mesmo tipo, posterior à conclusão.
7. Processe novamente: deve aparecer um segundo ciclo ligado ao primeiro.
8. Para um tema sem avaliação publicada, confira a pendência de catálogo e a ausência de trilha vazia.

## Evidência automatizada

`AprendizagemSprintIntegrationTest` cobre recorrência, ausência de duplicidade, reincidência vinculada, pendência de catálogo, recuperação após configuração e conclusão integrada à aprendizagem.
