# SCRUM-56 — Definir dados necessários para registrar uso de IA

## 1. Objetivo

Definir quais informações devem ser registradas pelo HestIA sempre que um usuário realizar uma utilização de Inteligência Artificial.

Esses dados servirão como base para funcionalidades posteriores do sistema, como histórico de utilização, análise de conformidade, métricas de consumo, análise por departamento e acompanhamento da utilização de IA dentro da organização.

---

## 2. Dados necessários

| Campo | Descrição | Obrigatório |
|---|---|---|
| `id_registro` | Identificador único do registro de utilização | Sim |
| `id_usuario` | Identifica o usuário responsável pela utilização | Sim |
| `id_ferramenta` | Identifica a ferramenta de IA utilizada | Sim |
| `modelo_ia` | Modelo de IA utilizado durante a execução | Sim |
| `data_hora` | Data e hora em que ocorreu a utilização | Sim |
| `finalidade` | Finalidade para a qual a IA foi utilizada | Sim |
| `tokens_entrada` | Quantidade de tokens enviados ao modelo | Quando disponível |
| `tokens_saida` | Quantidade de tokens gerados pelo modelo | Quando disponível |
| `tokens_total` | Quantidade total de tokens consumidos | Quando disponível |
| `custo_estimado` | Estimativa do custo da utilização | Não |
| `status_execucao` | Indica o resultado da execução | Sim |
| `tempo_resposta` | Tempo necessário para processar a solicitação | Não |
| `id_departamento` | Departamento associado ao usuário | Recomendado |

---

## 3. Identificação do uso

Cada utilização de IA deverá possuir um identificador único por meio do campo `id_registro`.

O registro deverá estar associado ao usuário responsável pela utilização através de `id_usuario`.

Também deverá ser possível identificar a ferramenta utilizada através de `id_ferramenta` e o modelo de IA efetivamente utilizado através de `modelo_ia`.

Dessa maneira, o sistema poderá identificar:

- quem utilizou IA;
- qual ferramenta foi utilizada;
- qual modelo foi utilizado;
- quando a utilização ocorreu.

---

## 4. Finalidade da utilização

Cada registro deverá possuir uma `finalidade`, permitindo identificar o motivo pelo qual a Inteligência Artificial foi utilizada.

Exemplos conceituais incluem:

- geração de código;
- geração de conteúdo;
- pesquisa;
- análise de dados;
- resumo;
- automação;
- suporte à decisão.

As categorias oficiais de finalidade não serão definidas nesta tarefa.

A definição e padronização dessas categorias será realizada posteriormente na **SCRUM-167 — Definir categorias de finalidade do uso**.

---

## 5. Consumo de tokens

Quando essas informações forem disponibilizadas pelo provedor ou modelo de IA, deverão ser registrados:

- `tokens_entrada`;
- `tokens_saida`;
- `tokens_total`.

O valor de `tokens_total` deverá preferencialmente ser calculado automaticamente:

`tokens_total = tokens_entrada + tokens_saida`

Essas informações permitirão posteriormente analisar o consumo de IA dentro do HestIA.

---

## 6. Custo estimado

O campo `custo_estimado` poderá armazenar uma estimativa do custo financeiro da utilização.

Esse valor não é considerado obrigatório, pois sua disponibilidade e cálculo podem depender do modelo de IA, provedor utilizado e política de preços vigente.

Quando possível, o custo deverá ser calculado automaticamente a partir das informações de consumo da execução.

---

## 7. Status da execução

Cada utilização deverá possuir um `status_execucao`, permitindo identificar se a chamada ao serviço de IA foi concluída corretamente.

Conceitualmente, poderão existir situações como:

- execução realizada com sucesso;
- execução com erro;
- execução interrompida.

Os valores definitivos deverão ser padronizados durante a implementação.

---

## 8. Tempo de resposta

Quando disponível, deverá ser registrado o `tempo_resposta` da chamada de IA.

Essa informação poderá ser utilizada para análise de desempenho e eficiência das ferramentas e modelos utilizados pelo HestIA.

---

## 9. Departamento

O sistema deverá permitir relacionar a utilização de IA ao departamento do usuário.

Essa informação será importante para funcionalidades futuras relacionadas a métricas e análises departamentais.

Preferencialmente, o departamento deverá ser obtido através do relacionamento existente com o usuário, evitando duplicação desnecessária de informações.

---

## 10. Armazenamento de prompts e respostas

O conteúdo integral do prompt e da resposta não será considerado obrigatório no registro básico de utilização.

O armazenamento desses conteúdos poderá envolver informações pessoais, dados confidenciais da organização, código-fonte ou outras informações sensíveis.

Caso funcionalidades futuras, como reutilização de respostas ou RAG, necessitem armazenar esse conteúdo, deverão ser definidas regras específicas relacionadas a:

- segurança;
- controle de acesso;
- privacidade;
- retenção dos dados;
- exclusão dos dados.

---

## 11. Exemplo conceitual de registro

```json
{
  "id_registro": 10482,
  "id_usuario": 27,
  "id_ferramenta": 3,
  "modelo_ia": "modelo-x",
  "data_hora": "2026-09-17T09:42:18",
  "finalidade": "GERACAO_CODIGO",
  "tokens_entrada": 850,
  "tokens_saida": 1240,
  "tokens_total": 2090,
  "custo_estimado": 0.018,
  "status_execucao": "SUCESSO",
  "tempo_resposta": 3.2,
  "id_departamento": 4
}
```

O exemplo acima representa apenas a estrutura conceitual dos dados e não define a implementação definitiva da entidade.

---

## 12. Decisões tomadas

- Cada utilização de IA deverá possuir um identificador único.
- Todo registro deverá estar associado a um usuário.
- A ferramenta e o modelo de IA utilizados deverão ser identificáveis.
- Data e hora deverão ser registradas automaticamente pelo sistema.
- A utilização deverá possuir uma finalidade.
- Tokens de entrada e saída deverão ser registrados quando disponibilizados.
- O total de tokens deverá preferencialmente ser calculado automaticamente.
- O custo poderá ser estimado quando houver informações suficientes.
- O resultado da execução deverá ser registrado.
- O departamento deverá preferencialmente ser obtido através do usuário.
- Prompt e resposta completos não serão obrigatórios no registro básico.
- O armazenamento de conteúdo sensível deverá possuir regras específicas de segurança e privacidade.

---

## 13. Resultado da tarefa

Com esta definição, estão estabelecidas as informações necessárias para representar uma utilização de Inteligência Artificial dentro do HestIA.

Esta documentação servirá como base para as próximas etapas de modelagem e implementação.

### Próximas tarefas relacionadas

**SCRUM-57 — Modelar entidade Registro de Uso de IA**

Transformar os dados definidos neste documento em uma entidade do modelo de dados do HestIA.

**SCRUM-58 — Modelar relacionamento Usuário–Ferramenta–Registro de Uso**

Definir formalmente os relacionamentos entre o registro de utilização, o usuário e a ferramenta de Inteligência Artificial.
