async function verificarProntidao(api,store,timeoutMs=2000){
  const consultarSessao=()=>new Promise(resolve=>{
    if(!store)return resolve('MEMORIA_LOCAL');
    let encerrada=false;const timer=setTimeout(()=>finalizar('DOWN'),timeoutMs);
    function finalizar(valor){if(encerrada)return;encerrada=true;clearTimeout(timer);resolve(valor);}
    try{store.get('__hestia_readiness_sem_usuario__',erro=>finalizar(erro?'DOWN':'UP'));}catch(_){finalizar('DOWN');}
  });
  const consultarApi=async()=>{try{const r=await api.get('/actuator/health',{timeout:timeoutMs});return r.data?.status==='UP'?'UP':'DOWN';}catch(_){return 'DOWN';}};
  const [backend,sessao]=await Promise.all([consultarApi(),consultarSessao()]);
  return {status:backend==='UP'&&sessao!=='DOWN'?'UP':'DOWN',dependencias:{backend,sessao}};
}
module.exports={verificarProntidao};
