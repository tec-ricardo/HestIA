const fs=require('node:fs'),cp=require('node:child_process'),path=require('node:path'),crypto=require('node:crypto');
function publicar({container,banco,usuario,entrada,saida,confirmarDemo,criarAdminDemo=false}){
 if(!confirmarDemo||!container||!banco||!usuario||!saida||!entrada)throw Error('Informe container, banco, usuário, entrada, saída e --confirmar-demo.');
 if(!/^[A-Za-z0-9_.-]+$/.test(container)||!/^[A-Za-z0-9_]+$/.test(banco)||!/^[A-Za-z0-9_]+$/.test(usuario)||!/(demo|validacao)/i.test(container+' '+banco))throw Error('Alvo deve identificar ambiente demo/validação.');
 if(fs.existsSync(saida))throw Error('Saída já existe; preserve o relatório anterior.');
 const raw=fs.readFileSync(entrada,'utf8'),fonte=JSON.parse(raw);
 if(fonte.repositorio!=='tec-ricardo/HestIA'||fonte.versao!=='colaboracao-demo-1.0'||!Array.isArray(fonte.autores)||!fonte.autores.length||!Array.isArray(fonte.prs))throw Error('Fonte não autorizada.');
 const ids=new Set(),logins=new Set();
 for(const a of fonte.autores){if(!Number.isSafeInteger(a.githubId)||a.githubId<=0||!/^[A-Za-z0-9-]{1,39}$/.test(a.login)||ids.has(a.githubId)||logins.has(a.login.toLowerCase()))throw Error('Autor inválido ou duplicado.');ids.add(a.githubId);logins.add(a.login.toLowerCase());}
 const val=fonte.autores.map(a=>"("+a.githubId+",'"+a.login+"')").join(',');
 for(const pr of fonte.prs){if(!Number.isSafeInteger(pr.numero)||pr.numero<=0||!ids.has(pr.autorGithubId)||!/^\d{4}-\d{2}-\d{2}$/.test(pr.dataMergeLocal)||!(pr.quantidadeRevisoes===null||Number.isInteger(pr.quantidadeRevisoes)&&pr.quantidadeRevisoes>=0))throw Error('PR inválido ou sem vínculo.');}
 const adminSql=criarAdminDemo?String.raw`
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM usuarios u JOIN empresas e ON e.id=u.empresa_id WHERE u.email='admin.demo@hestia.local' AND u.perfil='ADMIN' AND u.ativo=true AND e.cnpj='99999999000191') THEN RAISE EXCEPTION 'Administrador de referencia demo ausente'; END IF;
 IF EXISTS(SELECT 1 FROM usuarios u JOIN empresas e ON e.id=u.empresa_id JOIN departamentos d ON d.id=u.departamento_id WHERE u.email='admin.github.demo@hestia.local' AND (e.cnpj<>'99999999000272' OR u.perfil<>'ADMIN' OR d.empresa_id<>e.id OR d.nome<>'Desenvolvimento GitHub (DEMO)')) THEN RAISE EXCEPTION 'Administrador reservado divergente'; END IF;
END $$;
INSERT INTO usuarios(nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id)
 SELECT 'Administrador GitHub (DEMO)','admin.github.demo@hestia.local',origem.senha,'Administrador da demonstracao GitHub','ADMIN',true,CURRENT_TIMESTAMP,e.id,d.id
 FROM usuarios origem JOIN empresas e ON e.cnpj='99999999000272' JOIN departamentos d ON d.empresa_id=e.id AND d.nome='Desenvolvimento GitHub (DEMO)'
 WHERE origem.email='admin.demo@hestia.local' AND NOT EXISTS(SELECT 1 FROM usuarios u WHERE u.email='admin.github.demo@hestia.local');
`:'';
 const sql=String.raw`BEGIN;
SELECT pg_advisory_xact_lock(73539271);
DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM empresas WHERE cnpj='99999999000191' AND nome ILIKE '%Demonstra%') THEN RAISE EXCEPTION 'Carga demo de referência ausente'; END IF;
 IF EXISTS (SELECT 1 FROM empresas WHERE cnpj='99999999000272' AND (nome <> 'Equipe HestIA - GitHub (DEMO)' OR configuracoes_gerais NOT LIKE '%COLABORACAO_DEMO_V1%')) THEN RAISE EXCEPTION 'Identidade reservada ocupada'; END IF;
END $$;
INSERT INTO empresas(nome,cnpj,configuracoes_gerais,orcamento)
 SELECT 'Equipe HestIA - GitHub (DEMO)','99999999000272','COLABORACAO_DEMO_V1: eventos reais GitHub; vinculos ficticios autorizados; sem relacao com QSoft',NULL
 WHERE NOT EXISTS(SELECT 1 FROM empresas WHERE cnpj='99999999000272');
INSERT INTO departamentos(nome,responsavel,estrutura_hierarquica,empresa_id)
 SELECT 'Desenvolvimento GitHub (DEMO)','Equipe HestIA (cadastro didatico)','HESTIA_EQUIPE_DEMO / DESENVOLVIMENTO_DEMO',e.id
 FROM empresas e WHERE e.cnpj='99999999000272' AND NOT EXISTS(SELECT 1 FROM departamentos d WHERE d.empresa_id=e.id AND d.nome='Desenvolvimento GitHub (DEMO)');
CREATE TEMP TABLE autores_demo(github_id BIGINT PRIMARY KEY, login TEXT NOT NULL);
INSERT INTO autores_demo VALUES `+val+String.raw`;
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM autores_demo a JOIN usuarios u ON u.email='github-'||a.github_id||'@demo.hestia.invalid'
 JOIN empresas e ON e.id=u.empresa_id JOIN departamentos d ON d.id=u.departamento_id
 WHERE e.cnpj <> '99999999000272' OR d.empresa_id<>e.id OR d.nome<>'Desenvolvimento GitHub (DEMO)' OR u.ativo=true OR u.nome<>'Autor GitHub '||a.login)
 THEN RAISE EXCEPTION 'Usuario reservado divergente; nenhum cadastro existente sera movido'; END IF;
END $$;
INSERT INTO usuarios(nome,email,senha,cargo,perfil,ativo,data_cadastro,empresa_id,departamento_id)
 SELECT 'Autor GitHub '||a.login,'github-'||a.github_id||'@demo.hestia.invalid','!LOGIN_DESABILITADO_DEMO_GITHUB','Autoria observada no GitHub (DEMO)','FUNCIONARIO',false,CURRENT_TIMESTAMP,e.id,d.id
 FROM autores_demo a JOIN empresas e ON e.cnpj='99999999000272'
 JOIN departamentos d ON d.empresa_id=e.id AND d.nome='Desenvolvimento GitHub (DEMO)'
 WHERE NOT EXISTS(SELECT 1 FROM usuarios u WHERE u.email='github-'||a.github_id||'@demo.hestia.invalid');
`+adminSql+String.raw`
SELECT json_agg(json_build_object('githubId',a.github_id,'login',a.login,'usuarioId',u.id,'empresaId',u.empresa_id,'departamentoId',u.departamento_id,'loginHabilitado',u.ativo) ORDER BY a.github_id)::text
 FROM autores_demo a JOIN usuarios u ON u.email='github-'||a.github_id||'@demo.hestia.invalid';
COMMIT;`;
 const r=cp.spawnSync('docker',['exec','-i',container,'psql','-X','-q','-v','ON_ERROR_STOP=1','-U',usuario,'-d',banco,'-At'],{input:sql,encoding:'utf8',maxBuffer:200000});
 if(r.status!==0)throw Error('Cadastro demo não concluído: '+(r.stderr||'PostgreSQL indisponível').slice(0,500));
 const linha=r.stdout.split(/\r?\n/).find(x=>x.trim().startsWith('['));if(!linha)throw Error('Vínculos não retornados');
 const vinculos=JSON.parse(linha),mapa=new Map(vinculos.map(x=>[x.githubId,x]));
 const prs=fonte.prs.map(pr=>{const a=mapa.get(pr.autorGithubId);if(!a)throw Error('PR sem autor cadastrado');return {repositorio:fonte.repositorio,numero:pr.numero,dataMerge:pr.dataMergeLocal,empresaId:a.empresaId,departamentoId:a.departamentoId,autorId:a.usuarioId,quantidadeRevisoes:pr.quantidadeRevisoes,evidencia:pr.url,autorGithubId:pr.autorGithubId};});
 const validos=prs.filter(x=>x.quantidadeRevisoes!==null),revisados=validos.filter(x=>x.quantidadeRevisoes>0);
 const cobertura=prs.length?validos.length/prs.length:null;
 const resultado={versao:'colaboracao-vinculos-demo-1.0',geradoEm:new Date().toISOString(),ambiente:{container,banco},sha256Fonte:crypto.createHash('sha256').update(raw).digest('hex'),natureza:'EVENTOS_GITHUB_REAIS_COM_VINCULOS_DEMO',fonte:fonte.repositorio,inicio:fonte.inicio,fim:fonte.fim,populacaoPrsCompleta:fonte.populacaoPrsCompleta,vinculos,pullRequests:prs,resumo:{candidatos:prs.length,validos:validos.length,revisados:revisados.length,notaDescritiva:validos.length?100*revisados.length/validos.length:null,cobertura,fragil:!fonte.populacaoPrsCompleta||validos.length<30||cobertura===null||cobertura<0.95},avisos:fonte.avisos};
 fs.mkdirSync(path.dirname(path.resolve(saida)),{recursive:true});fs.writeFileSync(saida,JSON.stringify(resultado,null,2)+'\n');return resultado;
}
if(require.main===module){try{const args=process.argv.slice(2),v=key=>args.indexOf('--'+key)<0?undefined:args[args.indexOf('--'+key)+1];const r=publicar({container:v('container'),banco:v('banco'),usuario:v('usuario'),entrada:v('entrada'),saida:v('saida'),confirmarDemo:args.includes('--confirmar-demo'),criarAdminDemo:args.includes('--criar-admin-demo')});console.log(JSON.stringify({saida:v('saida'),vinculos:r.vinculos.length,resumo:r.resumo}));}catch(e){console.error(e.message);process.exit(1)}}
module.exports={publicar};
