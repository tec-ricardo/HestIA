WITH parametros AS (
    SELECT :'snapshot'::text AS snapshot, :'empresa'::text AS empresa,
        :'centro'::text AS centro, :'periodo'::text AS periodo,
        upper(:'moeda_orcamento') AS moeda, :'brl_por_usd'::numeric AS taxa
), custos AS (
    SELECT count(*) AS linhas, count(DISTINCT dados->>'billing_id') AS unicas,
           count(*) FILTER (WHERE dados->>'currency' IN ('BRL', 'USD')
                               AND (dados->>'total_cost')::numeric >= 0) AS validas,
        sum((dados->>'total_cost')::numeric) FILTER (WHERE dados->>'currency' = 'BRL') AS brl,
        sum((dados->>'total_cost')::numeric) FILTER (WHERE dados->>'currency' = 'USD') AS usd
    FROM base_sintetica.import_linhas, parametros p
    WHERE tabela = 'ai_billing' AND origem_sha256 = p.snapshot
      AND dados->>'company_id' = p.empresa AND dados->>'cost_center' = p.centro
    AND dados->>'billing_month' = p.periodo
    ), orcamentos AS (
SELECT count(*) AS linhas, max((dados->>'budget_amount')::numeric) AS valor
FROM base_sintetica.import_linhas, parametros p
WHERE tabela = 'ai_budgets' AND origem_sha256 = p.snapshot
  AND dados->>'company_id' = p.empresa AND dados->>'cost_center' = p.centro
  AND make_date((dados->>'year')::int, (dados->>'month')::int, 1) = (p.periodo || '-01')::date
    ), cenario AS (
SELECT p.*, c.brl, c.usd, c.linhas AS cobrancas,
    CASE WHEN o.linhas = 1 AND o.valor > 0 THEN o.valor END AS orcamento,
    CASE WHEN c.linhas > 0 AND c.linhas = c.unicas AND c.linhas = c.validas AND p.taxa > 0 THEN
    CASE p.moeda
    WHEN 'BRL' THEN coalesce(c.brl, 0) + coalesce(c.usd, 0) * p.taxa
    WHEN 'USD' THEN coalesce(c.usd, 0) + coalesce(c.brl, 0) / p.taxa
    END
    END AS custo
FROM parametros p CROSS JOIN custos c CROSS JOIN orcamentos o
    )
SELECT 'SIMULADA' AS natureza, empresa, centro, periodo, cobrancas, brl, usd,
       moeda AS moeda_assumida, taxa AS brl_por_usd, orcamento, round(custo, 2) AS custo_convertido,
       CASE WHEN custo IS NOT NULL AND orcamento IS NOT NULL
                THEN round(greatest(0, least(100, 100 * (2 - custo / orcamento))), 2)
           END AS nota_estimada
FROM cenario;
