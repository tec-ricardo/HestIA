# SCRUM-570 — Estimativa sintética de FinOps

Recorte validado no PostgreSQL: empresa C01, centro de custo CC-C01-D01, junho de 2025. A consulta usa um único snapshot do staging e cruza cobranças e orçamento por empresa, centro e mês.

| Dado | Valor |
| --- | ---: |
| Cobranças distintas | 9 |
| Subtotal em BRL | R$ 7.164,56 |
| Subtotal em USD | US$ 17,53 |
| Orçamento sintético registrado, sem moeda | 6.357,74 |
| Câmbio adotado na simulação | R$ 5,00 por US$ 1,00 |
| Custo convertido, assumindo orçamento em BRL | R$ 7.252,21 |
| Percentual estimado do orçamento | 114,07% |
| Nota FinOps simulada | 85,93 |

A moeda BRL do orçamento e o câmbio são premissas da simulação. O valor do orçamento veio de `ai_budgets`, e os subtotais vieram de `ai_billing`. O custo convertido é 7.164,56 + 17,53 × 5,00. A nota é 100 × (2 − custo / orçamento), limitada ao intervalo de 0 a 100. O resultado descreve a base sintética sob essas premissas. A apuração operacional permanece sem nota.

## Reprodução

Na raiz do HestIA, use o SHA-256 do snapshot que contém as duas tabelas:

```powershell
$snapshot = 'COLE_O_SHA256_DA_CARGA'
Get-Content -Raw .\docs\scrum\finops-estimativa.sql |
    docker compose exec -T postgres psql -U hestia -d hestia -v ON_ERROR_STOP=1 `
        -v "snapshot=$snapshot" -v empresa=C01 -v centro=CC-C01-D01 `
        -v periodo=2025-06 -v moeda_orcamento=BRL -v brl_por_usd=5.00
```

O teste `FinOpsEstimativaSinteticaTest` valida a fórmula com as hipóteses BRL e USD e verifica a ausência de nota quando a moeda não é definida.
