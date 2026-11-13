const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),cp=require('node:child_process');
function montar(raiz,saida){
 raiz=path.resolve(raiz);saida=path.resolve(saida);
 const rel=path.relative(raiz,saida);if(!rel.startsWith('..')&&!path.isAbsolute(rel))throw Error('Saída precisa ficar fora do repositório para não incluir o próprio pacote.');
 if(fs.existsSync(saida))throw Error('Destino já existe; escolha uma nova pasta.');
 const arquivos=[];const tipos=new Set(['.md','.json','.pdf','.pptx','.cjs','.ps1','.txt','.mmd','.svg','.png','.jpg']);
 function percorrer(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isSymbolicLink())continue;if(e.isDirectory())percorrer(p);else if(tipos.has(path.extname(e.name).toLowerCase()))arquivos.push(path.relative(raiz,p));}}
 percorrer(path.join(raiz,'docs'));
 for(const f of ['README.md','compose.release.yaml','scripts/publicar-release.ps1','scripts/validar-restauracao.ps1'])if(fs.existsSync(path.join(raiz,f)))arquivos.push(f);
 const unicos=[...new Set(arquivos)].sort();let total=0;const itens=[];
 for(const f of unicos){const b=fs.readFileSync(path.join(raiz,f));total+=b.length;if(total>150*1024*1024)throw Error('Pacote excede 150 MB; revisar seleção.');if(['.md','.json','.cjs','.ps1','.txt'].includes(path.extname(f).toLowerCase())&&/sk-(?:proj-)?[A-Za-z0-9_-]{30,}|Bearer\s+eyJ[A-Za-z0-9_.-]+/.test(b.toString('utf8')))throw Error('Possível credencial no arquivo '+f);itens.push({arquivo:f.replaceAll('\\','/'),bytes:b.length,sha256:crypto.createHash('sha256').update(b).digest('hex')});}
 fs.mkdirSync(saida,{recursive:true});
 for(const f of unicos){const dest=path.join(saida,f);fs.mkdirSync(path.dirname(dest),{recursive:true});fs.copyFileSync(path.join(raiz,f),dest);}
 const git=(args)=>cp.execFileSync('git',['-C',raiz,...args],{encoding:'utf8'}).trim();
 const manifesto={estado:'PREPARACAO_NAO_SUBMETIDA',geradoEm:new Date().toISOString(),repositorio:'https://github.com/tec-ricardo/HestIA',branch:git(['branch','--show-current']),commitBase:git(['rev-parse','HEAD']),alteracoesSemCommit:!!git(['status','--porcelain']),aviso:'Pacote documental de preparação; código completo está no repositório. Não comprova publicação, homologação final ou submissão. Regenerar da release aceita.',quantidadeArquivos:itens.length,bytes:total,arquivos:itens};
 fs.writeFileSync(path.join(saida,'MANIFESTO-ENTREGA.json'),JSON.stringify(manifesto,null,2)+'\n');return manifesto;
}
if(require.main===module){try{if(!process.argv[2])throw Error('Informe pasta de saída nova, fora do repositório.');const r=montar(path.resolve(__dirname,'..'),process.argv[2]);console.log(JSON.stringify({arquivos:r.quantidadeArquivos,bytes:r.bytes,estado:r.estado}));}catch(e){console.error(e.message);process.exit(1)}}
module.exports={montar};
