param(
 [Parameter(Mandatory=$true)][string]$ContainerOrigem,
 [Parameter(Mandatory=$true)][string]$Banco,
 [Parameter(Mandatory=$true)][string]$Usuario,
 [Parameter(Mandatory=$true)][string]$Destino
)
$ErrorActionPreference = 'Stop'
function Confirmar { if ($LASTEXITCODE -ne 0) { throw 'Operação Docker/PostgreSQL falhou; confira o diagnóstico anterior.' } }
$arquivo = [IO.Path]::GetFullPath($Destino)
if (Test-Path -LiteralPath $arquivo) { throw 'O destino já existe; escolha um arquivo novo para preservar o backup anterior.' }
$diretorio = Split-Path -Parent $arquivo
New-Item -ItemType Directory -Force -Path $diretorio | Out-Null
$identificador = [Guid]::NewGuid().ToString('N')
$temporario = '/tmp/hestia-' + $identificador + '.dump'
$restauracao = 'hestia-restore-' + $identificador
$senhaDescartavel = [Guid]::NewGuid().ToString('N')
$consulta = "SELECT 'usuarios',count(*) FROM usuarios UNION ALL SELECT 'registros',count(*) FROM registros_uso_ia UNION ALL SELECT 'catalogo',count(*) FROM catalogo_treinamentos UNION ALL SELECT 'ciencias',count(*) FROM ciencias_politica UNION ALL SELECT 'migracoes',count(*) FROM flyway_schema_history WHERE success=true ORDER BY 1;"
$antes = docker exec $ContainerOrigem psql -U $Usuario -d $Banco -At -c $consulta
Confirmar
docker exec $ContainerOrigem pg_dump -U $Usuario -d $Banco -Fc -f $temporario
Confirmar
docker cp "${ContainerOrigem}:$temporario" $arquivo
Confirmar
try {
 docker run -d --name $restauracao --network none -e "POSTGRES_PASSWORD=$senhaDescartavel" -e POSTGRES_DB=restauracao pgvector/pgvector:pg17 | Out-Null
 Confirmar
 $pronto = $false
 for ($i=0;$i -lt 45;$i++) {
  docker exec $restauracao pg_isready -U postgres -d restauracao *> $null
  if ($LASTEXITCODE -eq 0) { $pronto=$true; break }
  Start-Sleep -Seconds 1
 }
 if (!$pronto) { throw 'O banco descartável não iniciou no prazo.' }
 docker cp $arquivo "${restauracao}:/tmp/backup.dump"
 Confirmar
 docker exec $restauracao pg_restore -U postgres -d restauracao --no-owner --no-acl --exit-on-error /tmp/backup.dump
 Confirmar
 $depois = docker exec $restauracao psql -U postgres -d restauracao -At -c $consulta
 Confirmar
 if (($antes -join "`n") -ne ($depois -join "`n")) { throw 'Contagens divergentes; restauração não aprovada.' }
 [pscustomobject]@{Backup=$arquivo;SHA256=(Get-FileHash -LiteralPath $arquivo -Algorithm SHA256).Hash;Conferencia=$depois;Resultado='Restauração validada em container isolado'} | ConvertTo-Json
} finally {
 # Somente o container descartável criado por esta execução é removido.
 docker rm -f $restauracao *> $null
}
