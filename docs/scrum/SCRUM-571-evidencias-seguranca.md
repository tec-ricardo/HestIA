# SCRUM-571 — Evidências do indicador de Segurança

O indicador operacional usa os oito controles obrigatórios de `seguranca-1.0`. A fonte é um manifesto de configuração do servidor. Somente a equipe autorizada a configurar o ambiente pode fornecer o manifesto; a consulta HTTP não aceita evidências ou alterações de release. O responsável pela revisão confere a referência antes de registrar um resultado conclusivo.

O manifesto seleciona uma release e um ambiente. Cada evidência indica empresa, departamento quando aplicável, release, ambiente, versão do catálogo, controle, situação, referência verificável, responsável e instante de verificação. Use uma referência a um artefato de teste autorizado, sem incluir segredos ou o conteúdo da evidência. Preserve versões anteriores do manifesto para reproduzir apurações.

Estrutura de configuração inicial, sem dados demonstrativos:

```yaml
hestia:
  maturidade:
    seguranca:
      release: ""
      ambiente: ""
      evidencias: []
```

Ao configurar o ambiente, use a release ou o commit avaliado em `release`. Cada elemento de `evidencias` tem os campos `empresa-id`, `departamento-id` (omitido para o escopo da empresa), `release`, `ambiente`, `versao-catalogo`, `controle`, `situacao`, `referencia`, `responsavel` e `verificada-em` (instante ISO 8601, com fuso). Os identificadores de controles e situações são os do código de `CatalogoControlesSeguranca`.

A seleção exige o mesmo escopo, release, ambiente e catálogo. O período corresponde a dias UTC, com início inclusivo e fim exclusivo no primeiro instante do dia seguinte. Por controle, usa-se a avaliação mais recente do período; conflito no mesmo instante mantém o controle não verificado. Evidências futuras não são usadas. Evidência de empresa não substitui evidência de departamento.

A nota é 100 × atendidos / avaliados conclusivamente; a cobertura é avaliados / 8. Sem avaliação conclusiva, a nota é nula. Cobertura incompleta mantém o fator fora do global. A fonte sintética permanece separada.
