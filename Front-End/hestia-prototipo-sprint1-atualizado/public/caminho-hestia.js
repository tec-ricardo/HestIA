/* HestIA Pass: quatro niveis, XP e status lidos da pagina existente. */
(() => {
  'use strict';
  const $ = (selector, root = document) => root.querySelector(selector);
  const ns = 'http://www.w3.org/2000/svg';
  const clamp = (value, min, max) => Math.min(max, Math.max(min, value));
  const parseXP = value => {
    const clean = String(value ?? '').replace(/\s*XP\b/gi, '').trim();
    const number = Number(clean.replace(/\./g, '').replace(',', '.').replace(/[^\d.-]/g, ''));
    return Number.isFinite(number) ? number : 0;
  };
  let previousFocus = null;
  let openState = false;
  let lastSignature = '';
  const svgEl = (tag, attrs = {}) => {
    const element = document.createElementNS(ns, tag);
    Object.entries(attrs).forEach(([key, value]) => element.setAttribute(key, String(value)));
    return element;
  };
  function getData() {
    const root = $('.main-content.pass-page');
    if (!root) return null;
    const xp = Math.max(0, parseXP(root.dataset.hestiaPassXp));
    // Os quatro niveis e limites sao lidos dos mesmos cards do HestIA Pass.
    const levels = [...root.querySelectorAll('.pass-level')].map((card, i) => ({
      index: Number($('.pass-level-info span', card)?.textContent.match(/\d+/)?.[0]) || i + 1,
      name: $('.pass-level-info strong', card)?.textContent.trim() || `Nivel ${i + 1}`,
      xp: parseXP($('.pass-level-info small', card)?.textContent),
      status: card.classList.contains('pass-level-concluido') ? 'concluido' :
        card.classList.contains('pass-level-atual') ? 'atual' : 'bloqueado'
    })).sort((a, b) => a.xp - b.xp);
    if (!levels.length) return null;
    return { xp, levels };
  }
  function render() {
    const overlay = $('#hestiaPathOverlay');
    const position = $('#hestiaPhotoPosition');
    const data = getData();
    if (!overlay || !position || !data) return;
    const { xp, levels } = data;
    const max = Math.max(levels[levels.length - 1].xp, 1);
    const progress = clamp(xp / max, 0, 1);
    const current = [...levels].reverse().find(level => xp >= level.xp) || levels[0];

    // Pontos medidos sobre a foto: 0, 300, 750, 2500 XP.
    // Coordenadas percentuais relativas ao cenário, não à tela.
    const anchors = [
      { xp: 0, x: 16.5, y: 81 },
      { xp: 300, x: 37, y: 65 },
      { xp: 750, x: 60, y: 54 },
      { xp: 2500, x: 86, y: 34 }
    ];
    let from = anchors[0], to = anchors[anchors.length - 1];
    for (let i = 0; i < anchors.length - 1; i++) {
      if (xp <= anchors[i + 1].xp) { from = anchors[i]; to = anchors[i + 1]; break; }
    }
    const t = clamp((xp - from.xp) / Math.max(1, to.xp - from.xp), 0, 1);
    position.style.left = `${from.x + (to.x - from.x) * t}%`;
    position.style.top = `${from.y + (to.y - from.y) * t}%`;
    const message = `Você está aqui: ${xp.toLocaleString('pt-BR')} XP`;
    position.setAttribute('aria-label', message);
    const tooltip = $('#hestiaPhotoPositionText');
    if (tooltip) tooltip.textContent = `${xp.toLocaleString('pt-BR')} XP`;

    $('#hestiaPathLevel').textContent = `${current.index} · ${current.name}`;
    $('#hestiaPathXP').textContent = `${xp.toLocaleString('pt-BR')} / ${max.toLocaleString('pt-BR')} XP`;
    $('#hestiaPathRemaining').textContent = xp >= max ? 'Templo alcançado!' : `${(max - xp).toLocaleString('pt-BR')} XP`;
    $('#hestiaPathPercent').textContent = `${Math.round(progress * 100)}%`;
    overlay.style.setProperty('--hestia-progress', `${Math.round(progress * 100)}%`);
  }
  function open() {
    const overlay = $('#hestiaPathOverlay');
    if (!overlay) return;
    previousFocus = document.activeElement;
    openState = true;
    render();
    overlay.hidden = false;
    overlay.setAttribute('aria-hidden', 'false');
    document.body.classList.add('hestia-path-active');
    requestAnimationFrame(() => overlay.classList.add('is-open'));
    $('#hestiaPathClose')?.focus();
  }
  function close() {
    const overlay = $('#hestiaPathOverlay');
    openState = false;
    if (!overlay) return;
    overlay.classList.remove('is-open');
    overlay.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('hestia-path-active');
    setTimeout(() => {
      if (!overlay.classList.contains('is-open')) overlay.hidden = true;
    }, 350);
    previousFocus?.focus?.();
  }
  document.addEventListener('click', event => {
    if (event.target.closest('#hestiaPathOpen')) open();
    if (event.target.closest('#hestiaPathClose') || event.target === $('#hestiaPathOverlay')) close();
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && openState) close();
    if (event.key === 'Tab' && openState) {
      event.preventDefault();
      $('#hestiaPathClose')?.focus();
    }
  });
  // O EJS ja consulta /api/passe-hestia/status a cada 2 s e substitui <main>.
  // Ao mudar o XP, o mapa acompanha o novo <main> sem criar API ou XP paralelo.
  const observer = new MutationObserver(() => {
    if (!openState) return;
    const root = $('.main-content.pass-page');
    const overlay = $('#hestiaPathOverlay');
    if (!root || !overlay) return;
    const signature = `${root.dataset.hestiaPassXp}|${root.querySelectorAll('.pass-level').length}`;
    if (signature === lastSignature && overlay.classList.contains('is-open')) return;
    lastSignature = signature;
    overlay.hidden = false;
    overlay.setAttribute('aria-hidden', 'false');
    overlay.classList.add('is-open');
    document.body.classList.add('hestia-path-active');
    render();
  });
  const startObserver = () => observer.observe(document.body, { childList: true, subtree: true });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', startObserver);
  else startObserver();
})();