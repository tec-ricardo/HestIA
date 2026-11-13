param(
 [Parameter(Mandatory=$true)][string]$Commit,
 [Parameter(Mandatory=$true)][string]$EnvFile,
 [ValidatePattern('^[a-z][a-z0-9_-]{1,40}$')][string]$Projeto='hestia',
 [switch]$ValidarSomente
)
$ErrorActionPreference='Stop'
function Conferir { if($LASTEXITCODE -ne 0){throw 'Falha na verificação/publicação; a release não foi aprovada.'} }
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$ambiente=(Resolve-Path -LiteralPath $EnvFile).Path
Push-Location -LiteralPath $raiz
$refAnterior=$env:HESTIA_VCS_REF
try {
 $esperado=git rev-parse --verify "$Commit^{commit}";Conferir
 $atual=git rev-parse HEAD;Conferir
 if($esperado -ne $atual){throw 'O checkout atual não corresponde ao commit solicitado; prepare a versão antes de publicar.'}
 $alteracoes=git status --porcelain
 if($alteracoes -and !$ValidarSomente){throw 'Existem alterações sem commit; a publicação exige uma versão identificável e revisada.'}
 $env:HESTIA_VCS_REF=$atual
 docker compose --env-file $ambiente -p $Projeto -f compose.release.yaml config --quiet
 Conferir
 $configTexto=docker compose --env-file $ambiente -p $Projeto -f compose.release.yaml config --format json
 Conferir
 $config=($configTexto -join "`n") | ConvertFrom-Json
 $urlPublica=$null
 if(![Uri]::TryCreate([string]$config.services.backend.environment.PUBLIC_APP_URL,[UriKind]::Absolute,[ref]$urlPublica) -or $urlPublica.Scheme -ne 'https'){
  throw 'PUBLIC_APP_URL deve ser uma URL HTTPS absoluta; configure também o proxy/certificado do host.'
 }
 if($ValidarSomente){Write-Output 'Configuração da release válida. Nenhum serviço foi publicado.';return}
 # Preserva um backup antes de substituir um ambiente já existente.
 $origem=docker compose --env-file $ambiente -p $Projeto -f compose.release.yaml ps -q postgres
 Conferir
 if($origem){
  $pasta=Join-Path (Split-Path -Parent $ambiente) 'backups'
  $destino=Join-Path $pasta ('antes-release-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N')+'.dump')
  & (Join-Path $PSScriptRoot 'validar-restauracao.ps1') -ContainerOrigem $origem -Banco hestia -Usuario hestia -Destino $destino
  if(!$?){throw 'Backup/restauração anterior à atualização não aprovados.'}
 }
 docker compose --env-file $ambiente -p $Projeto -f compose.release.yaml build --pull
 Conferir
 docker compose --env-file $ambiente -p $Projeto -f compose.release.yaml up -d --wait --wait-timeout 240
 Conferir
 Write-Output ('Serviços saudáveis no host atual para o commit '+$atual+'. Conferir também a URL HTTPS pública e as jornadas antes do aceite.')
} finally {
 if($null -eq $refAnterior){Remove-Item Env:HESTIA_VCS_REF -ErrorAction SilentlyContinue}else{$env:HESTIA_VCS_REF=$refAnterior}
 Pop-Location
}
