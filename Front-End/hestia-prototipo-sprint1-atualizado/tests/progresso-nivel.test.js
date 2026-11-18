const {test}=require('node:test'),assert=require('node:assert/strict');
const {progressoNivel}=require('../progresso-nivel');
test('progresso acompanha faixa e transicoes',()=>{for(const [xp,n] of [[0,0],[165,55],[300,0],[445,32],[750,0],[1625,50],[2500,100],[3000,100]])assert.equal(progressoNivel(xp),n);for(const xp of [-1,NaN,Infinity])assert.equal(progressoNivel(xp),null);});
