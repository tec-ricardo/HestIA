/* SCRUM-564: adicionar como public/microtreinamentos-busca.js.
   Não substitui o JS original. Usa a lista e API já existentes. */
(function () {
  if (!document.getElementById('mt-list')) return;
  const fetchOriginal = window.fetch.bind(window);
  window.fetch = function(input, init) {
    return fetchOriginal(input, init).then(async response => {
      const url = typeof input === 'string' ? input : input?.url || '';
      if (!url.includes('/api/microtreinamentos') || /\/api\/microtreinamentos\//.test(url) || !response.ok) return response;
      const clone = response.clone();
      try { const dados = await clone.json(); if (dados.nivelAtual) window.hestiaNivelAtual = dados.nivelAtual; } catch (_) {}
      return response;
    });
  };
  const lista = document.getElementById('mt-list');
  const toolbar = document.querySelector('.mt-toolbar');
  if (!toolbar || typeof mostrarCursos !== 'function') return;
  const painel = document.createElement('div');
  painel.className = 'mt-discovery';
  painel.innerHTML = '<div class="mt-discovery-head"><div><span class="mt-discovery-eyebrow">✦ HestIA Academy</span><h3>Encontre seu próximo aprendizado</h3><p>Busque treinamentos por assunto e descubra o próximo passo da sua jornada.</p></div><span class="mt-discovery-icon" aria-hidden="true">⌕</span></div><div class="mt-discovery-controls"><label class="mt-search-label"><span>Buscar treinamentos</span><input id="mt-search" type="search" placeholder="Ex.: LGPD, auditoria, segurança, IA responsável" autocomplete="off"></label><label class="mt-select-label"><span>Nível</span><select id="mt-level"><option value="">Todos os níveis</option></select></label><label class="mt-select-label"><span>Ordenar</span><select id="mt-sort"><option value="nivel">Nível (1 → 4)</option><option value="relevancia">Mais relevantes</option><option value="titulo">Título (A–Z)</option><option value="duracao">Menor duração</option></select></label></div><div class="mt-discovery-bottom"><span id="mt-result-count" aria-live="polite"></span><button id="mt-clear-search" type="button">Limpar busca</button></div><div class="mt-recommendation" id="mt-recommendation" aria-live="polite"></div>';
  toolbar.parentNode.insertBefore(painel, toolbar);
  const search = document.getElementById('mt-search');
  const level = document.getElementById('mt-level');
  const sort = document.getElementById('mt-sort');
  const clear = document.getElementById('mt-clear-search');
  sort.value = 'nivel';
  const norm = s => String(s == null ? '' : s).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  const esc = s => { const d = document.createElement('div'); d.textContent = String(s == null ? '' : s); return d.innerHTML; };
  const pct = c => c.concluido ? 100 : Math.max(0, Math.min(100, Math.round(100 * Number(c.etapa || 0) / Math.max(1, Number(c.totalEtapas || 1)))));
  const score = c => (c.recomendado ? 30 : 0) + (c.etapa > 0 && !c.concluido ? 25 : 0) + (!c.bloqueado && !c.concluido ? 15 : 0) - (c.bloqueado ? 40 : 0) - (c.concluido ? 10 : 0);
  function render() {
    if (typeof cursos === 'undefined' || !Array.isArray(cursos)) return;
    const all = cursos.map(c => {
      const numero = Number(c.id);
      if (!Number.isInteger(numero) || numero < 1 || numero > 16) return c;
      const nivelNumero = Math.ceil(numero / 4);
      const nomes = ['Aprendiz', 'Consciente', 'Guardião', 'Embaixador'];
      const nivelUsuario = Number(window.hestiaNivelAtual?.numero || 0);
      return { ...c, nivel: `Nível ${nivelNumero} — ${nomes[nivelNumero-1]}`,
        bloqueado: nivelUsuario < nivelNumero || Boolean(c.bloqueado),
        recomendado: nivelUsuario >= nivelNumero && Boolean(c.recomendado) && !c.bloqueado,
        motivo: nivelUsuario < nivelNumero ? `Alcance o nível ${nomes[nivelNumero-1]} no HestIA Pass para liberar.` : c.motivo };
    });
    const oldLevel = level.value;
    const niveis = ['Nível 1 — Aprendiz','Nível 2 — Consciente','Nível 3 — Guardião','Nível 4 — Embaixador'];
    level.innerHTML = '<option value="">Todos os níveis</option>' + niveis.map(n => '<option value="' + esc(n) + '">' + esc(n) + '</option>').join('');
    level.value = niveis.includes(oldLevel) ? oldLevel : '';
    const query = norm(search.value);
    let visiveis = all.filter(c => {
      if (filtroAtual === 'recomendados' && !c.recomendado) return false;
      if (filtroAtual === 'andamento' && !(c.etapa > 0 && !c.concluido)) return false;
      if (filtroAtual === 'concluidos' && !c.concluido) return false;
      if (level.value && c.nivel !== level.value) return false;
      return !query || norm([c.titulo, c.descricao, c.nivel].join(' ')).includes(query);
    });
    visiveis.sort((a, b) => sort.value === 'nivel' ? Number(a.id) - Number(b.id) : sort.value === 'titulo' ? String(a.titulo).localeCompare(String(b.titulo), 'pt-BR') : sort.value === 'duracao' ? Number(a.duracao || 0) - Number(b.duracao || 0) : score(b) - score(a));
    document.getElementById('mt-total').textContent = all.filter(c => !c.bloqueado).length;
    document.getElementById('mt-done').textContent = all.filter(c => c.concluido).length;
    document.getElementById('mt-running').textContent = all.filter(c => c.etapa > 0 && !c.concluido).length;
    document.getElementById('mt-result-count').textContent = visiveis.length + (visiveis.length === 1 ? ' treinamento encontrado' : ' treinamentos encontrados');
    const sugestao = [...all].filter(c => !c.concluido && !c.bloqueado).sort((a,b) => score(b)-score(a))[0];
    const recomendacao = document.getElementById('mt-recommendation');
    recomendacao.innerHTML = sugestao ? '<div><span class="mt-recommendation-tag">✦ Próximo passo sugerido</span><strong>' + esc(sugestao.titulo) + '</strong><small>' + esc(sugestao.nivel) + ' · ' + esc(sugestao.duracao) + ' minutos · ' + pct(sugestao) + '% concluído</small></div><a class="mt-recommendation-link" href="/microtreinamentos/' + encodeURIComponent(sugestao.id) + '">' + (sugestao.etapa > 0 ? 'Continuar curso' : 'Começar curso') + ' →</a>' : '<span>Você concluiu todos os treinamentos disponíveis!</span>';
    lista.innerHTML = visiveis.map(c => {
      const estado = c.bloqueado ? 'Bloqueado' : c.concluido ? 'Concluído' : c.etapa > 0 ? 'Em andamento' : 'Disponível';
      const p = pct(c);
      return '<article class="mt-card mt-card-upgraded"><div class="mt-card-top"><span class="mt-tag">🏅 ' + esc(c.nivel) + '</span><span class="mt-status ' + (c.concluido ? 'is-done' : c.bloqueado ? 'is-locked' : c.etapa > 0 ? 'is-running' : '') + '">' + estado + '</span></div>' + (c.recomendado && !c.concluido ? '<span class="mt-card-recommended">✦ Recomendado para você</span>' : '') + '<h3>' + esc(c.titulo) + '</h3><p>' + esc(c.descricao) + '</p><div class="mt-meta">◷ ' + esc(c.duracao) + ' minutos <span>·</span> ' + esc(c.totalEtapas) + ' aulas</div><div class="mt-progress"><span style="width:' + p + '%"></span></div><div class="mt-card-footer"><small>' + p + '% concluído</small>' + (c.bloqueado ? '<small>' + esc(c.motivo || 'Indisponível') + '</small>' : '<a class="mt-btn mt-btn-primary" href="/microtreinamentos/' + encodeURIComponent(c.id) + '">' + (c.concluido ? 'Revisar curso' : c.etapa > 0 ? 'Continuar curso' : 'Começar curso') + ' →</a>') + '</div></article>';
    }).join('') || '<div class="mt-empty mt-empty-upgraded"><strong>Nenhum treinamento encontrado</strong><p>Tente outro termo ou limpe os filtros.</p></div>';
  }
  mostrarCursos = render;
  search.addEventListener('input', render);
  level.addEventListener('change', render);
  sort.addEventListener('change', render);
  clear.addEventListener('click', () => { search.value = ''; level.value = ''; sort.value = 'nivel'; document.querySelector('[data-filter="todos"]')?.click(); render(); search.focus(); });
  if (typeof cursos !== 'undefined' && cursos.length) render();
})();