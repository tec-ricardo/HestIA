param([string]$Token)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(60)
$base = 'http://localhost:8080'
$titulo = 'Política de Uso Responsável de IA — Demonstração HestIA — v1.1'
$texto = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'SCRUM-543-politica-demo-v1.1.md'), [Text.Encoding]::UTF8)
if ([string]::IsNullOrWhiteSpace($texto)) { throw 'Arquivo da política vazio.' }

function Api($metodo, $rota, $dados = $null) {
    $req = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($metodo), "$base$rota")
    try {
        if ($Token) {
            $req.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)
        }
        if ($null -ne $dados) {
            $json = ConvertTo-Json -InputObject $dados -Depth 6 -Compress
            $req.Content = [System.Net.Http.StringContent]::new($json, [Text.Encoding]::UTF8, 'application/json')
        }
        $resp = $client.SendAsync($req).GetAwaiter().GetResult()
        try {
            $raw = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            if (!$resp.IsSuccessStatusCode) {
                throw "$metodo ${rota}: HTTP $([int]$resp.StatusCode) $raw"
            }
            if ($raw) { ConvertFrom-Json -InputObject $raw }
        } finally { $resp.Dispose() }
    } finally { $req.Dispose() }
}

try {
    if (!$Token) {
        $senha = Read-Host 'Senha de admin.demo@hestia.local' -AsSecureString
        $credencial = [pscredential]::new('admin.demo@hestia.local', $senha)
        try {
            $sessao = Api 'POST' '/auth/login' @{email=$credencial.UserName; senha=$credencial.GetNetworkCredential().Password}
            $Token = $sessao.token
        } finally { Remove-Variable senha,credencial -ErrorAction SilentlyContinue }
    }
    $ctx = Api 'GET' '/api/contexto'
    if ($ctx.perfil -ne 'ADMIN' -or $ctx.empresaId -ne 1) { throw 'Contexto inesperado: esperado ADMIN da empresa demo 1.' }

    $titulos = @()
    for ($pagina = 0; $pagina -lt 100; $pagina++) {
        $retorno = Api 'GET' "/api/admin/respostas?pagina=$pagina&tamanho=100"
        $lote = @(foreach ($entrada in $retorno) { $entrada })
        $titulos += @($lote | Where-Object { $_.prompt_original -ceq $titulo })
        if ($lote.Count -lt 100) { break }
    }
    if ($pagina -ge 100) { throw 'Lista extensa demais para confirmar que não há duplicidade.' }
    if ($titulos.Count -gt 1) { throw 'Há mais de um registro v1.1 com esse título. Confira os IDs antes de continuar.' }
    if ($titulos.Count -eq 1) {
        $id = $titulos[0].id
        $atual = (Api 'GET' "/api/admin/respostas/$id").resposta
        if ($atual.respostaGerada -cne $texto) { throw "O registro $id tem o mesmo título, mas outro conteúdo." }
        if ($atual.statusQualidade -notin @('EM_REVISAO','APROVADA')) { throw "O registro $id não está em revisão nem aprovado." }
    } else {
        $novo = Api 'POST' '/api/respostas-reutilizaveis' @{
            empresaId = [long]$ctx.empresaId
            promptOriginal = $titulo
            respostaGerada = $texto
            categoria = 'GOVERNANCA'
            possuiDadosSensiveis = $false
            reutilizavel = $false
        }
        $id = $novo.id
    }
    if (!$id) { throw 'A API não retornou o ID da fonte.' }
    Write-Host "Fonte demo: ID $id"
    Api 'PUT' "/api/respostas-reutilizaveis/$id/perfis" @('ADMIN','GESTOR','FUNCIONARIO') | Out-Null
    Api 'POST' "/api/admin/respostas/$id/revisoes" @{
        status = 'APROVADA'
        justificativa = 'Política fictícia v1.1 autorizada para a demonstração HestIA; não representa a QSoft.'
    } | Out-Null
    Api 'POST' "/api/respostas-reutilizaveis/$id/indexar" | Out-Null
    Write-Host "Concluído: fonte $id aprovada e indexada pela API local."
} finally {
    $client.Dispose()
}
