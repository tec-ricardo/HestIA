function periodoDemoBanco(hoje=new Date()) {
 const inicio=new Date(hoje.getFullYear(),hoje.getMonth()-1,1);
 const fim=new Date(hoje.getFullYear(),hoje.getMonth(),0);
 const iso=d=>[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
 return {inicio:iso(inicio),fim:iso(fim)};
}
const contaDemo=email=>['admin.demo@hestia.local','gestor.demo@hestia.local'].includes(email);
module.exports={periodoDemoBanco,contaDemo};
