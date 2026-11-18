function progressoNivel(xp) {
 if(!Number.isFinite(xp)||xp<0)return null;
 if(xp>=2500)return 100;
 const inicio=xp>=750?750:xp>=300?300:0;
 const fim=xp>=750?2500:xp>=300?750:300;
 return Math.round((xp-inicio)/(fim-inicio)*100);
}
module.exports={progressoNivel};
