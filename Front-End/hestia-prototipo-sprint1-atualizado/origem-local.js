const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const snapshot=require('./dados/base-original-c01.json');
let cache;
function carregarOrigem() {
    const padrao=path.resolve(__dirname,'../../dados-locais/ai_usage_synthetic.db');
    const arquivo=process.env.HESTIA_BASE_ORIGINAL_DB||(fs.existsSync(padrao)?padrao:null);
    if(!arquivo)return {...snapshot,modoConsulta:'SNAPSHOT_CALCULADO'};
    const stat=fs.statSync(arquivo),chave=arquivo+':'+stat.mtimeMs+':'+stat.size;
    if(cache?.chave===chave)return cache.dados;
    const {DatabaseSync}=require('node:sqlite');
    const {gerar}=require('../../scripts/calcular-base-original.cjs');
    const db=new DatabaseSync(arquivo,{readOnly:true});
    try {
        const dados=gerar(db,{empresa:'C01',inicio:snapshot.periodo.inicio,fim:snapshot.periodo.fim,
            cambio:snapshot.premissas.cambioBRLPorUSD,moedaOrcamento:snapshot.premissas.moedaOrcamento,
            metaDias:snapshot.premissas.metaDiasSimulada});
        dados.origem={arquivo:path.basename(arquivo),sha256:crypto.createHash('sha256').update(fs.readFileSync(arquivo)).digest('hex')};
        dados.modoConsulta='SQLITE_SOMENTE_LEITURA';
        cache={chave,dados};return dados;
    } finally {db.close();}
}
module.exports={carregarOrigem};
