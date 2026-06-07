/* ============================================================
   Simulador de Rede - ISUTC : logica do cliente (JS puro)
   Comunica com o backend Spring Boot via REST (fetch).
   ============================================================ */
'use strict';

const SVGNS = 'http://www.w3.org/2000/svg';
const R = 28;              // raio/meia-altura do corpo do dispositivo
const API = '/api';

/* ---------------- Estado ---------------- */
const state = {
  nos: [],                 // {id, tipo, ip, x, y}
  arestas: [],             // {origem, destino, peso}
  selecionado: null,
  modoLigacao: false,
  ligacaoOrigem: null,
  destaqueCaminho: [],
  destaqueAlcancaveis: null,
};

/* ---------------- Elementos ---------------- */
const $ = (s) => document.querySelector(s);
const canvas = $('#canvas');
const hint = $('#canvasHint');
const canvasWrap = $('.canvas-wrap');

/* =====================================================
   API helpers
   ===================================================== */
async function api(metodo, caminho, corpo) {
  const opts = { method: metodo, headers: {} };
  if (corpo !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(corpo);
  }
  const resp = await fetch(API + caminho, opts);
  if (!resp.ok) {
    let msg = 'Erro ' + resp.status;
    try { const j = await resp.json(); if (j.mensagem) msg = j.mensagem; } catch (_) {}
    throw new Error(msg);
  }
  if (resp.status === 204) return null;
  const txt = await resp.text();
  return txt ? JSON.parse(txt) : null;
}

/* =====================================================
   Carregar / refrescar topologia
   ===================================================== */
async function refrescar() {
  const topo = await api('GET', '/topologia');
  state.nos = topo.nos || [];
  state.arestas = topo.arestas || [];
  if (state.selecionado && !state.nos.find(n => n.id === state.selecionado)) {
    state.selecionado = null;
  }
  desenhar();
  preencherSelects();
  if (state.selecionado && noPorId(state.selecionado)) preencherLigacoes(state.selecionado);
  await atualizarStatus();
}

async function atualizarStatus() {
  const txt = $('#statusTexto');
  const n = state.nos.length;
  const m = state.arestas.length;
  if (n === 0) { txt.textContent = 'Rede vazia'; return; }
  try {
    const c = await api('GET', '/algoritmos/conectividade');
    txt.textContent = `${n} disp. / ${m} ligacoes / ` +
      (c.conectado ? 'conectada' : `${c.totalComponentes} componentes`);
  } catch (_) {
    txt.textContent = `${n} dispositivos / ${m} ligacoes`;
  }
}

/* =====================================================
   Desenho do SVG
   ===================================================== */
function noPorId(id) { return state.nos.find(n => n.id === id); }

function desenhar() {
  canvas.innerHTML = '';
  hint.style.display = state.nos.length ? 'none' : 'flex';

  const caminhoSet = new Set(state.destaqueCaminho);
  const paresCaminho = new Set();
  for (let i = 0; i < state.destaqueCaminho.length - 1; i++) {
    paresCaminho.add(parKey(state.destaqueCaminho[i], state.destaqueCaminho[i + 1]));
  }

  // ---- arestas (por baixo) ----
  for (const a of state.arestas) {
    const o = noPorId(a.origem), d = noPorId(a.destino);
    if (!o || !d) continue;
    const naCaminho = paresCaminho.has(parKey(a.origem, a.destino));

    const grupo = el('g', { class: 'edge-group', 'data-o': a.origem, 'data-d': a.destino });
    const linha = el('line', { class: 'edge' + (naCaminho ? ' path' : '') });
    const hit   = el('line', { class: 'edge-hit' });
    hit.addEventListener('click', (e) => { e.stopPropagation(); removerAresta(a); });
    grupo.appendChild(linha); grupo.appendChild(hit);

    // latencia/distancia so existe entre routers -> so esses mostram etiqueta
    if (o.tipo === 'router' && d.tipo === 'router') {
      const texto = formatarPeso(a.peso);
      const larg = 13 + texto.length * 7;
      const bg  = el('rect', { class: 'edge-label-bg', width: larg, height: 18, rx: 3 });
      const lab = el('text', { class: 'edge-label', 'text-anchor': 'middle' });
      lab.textContent = texto;
      grupo.appendChild(bg); grupo.appendChild(lab);
    }

    canvas.appendChild(grupo);
    posicionarAresta(grupo);
  }

  // ---- nos ----
  for (const n of state.nos) canvas.appendChild(criarNo(n, caminhoSet));
}

function posicionarAresta(grupo) {
  const o = noPorId(grupo.getAttribute('data-o'));
  const d = noPorId(grupo.getAttribute('data-d'));
  if (!o || !d) return;
  grupo.querySelectorAll('line').forEach(l => {
    l.setAttribute('x1', o.x); l.setAttribute('y1', o.y);
    l.setAttribute('x2', d.x); l.setAttribute('y2', d.y);
  });
  const mx = (o.x + d.x) / 2, my = (o.y + d.y) / 2;
  const bg = grupo.querySelector('.edge-label-bg');
  const lab = grupo.querySelector('.edge-label');
  if (bg && lab) {
    const larg = parseFloat(bg.getAttribute('width'));
    bg.setAttribute('x', mx - larg / 2);
    bg.setAttribute('y', my - 9);
    lab.setAttribute('x', mx);
    lab.setAttribute('y', my + 4);
  }
}

function criarNo(n, caminhoSet) {
  const g = el('g', { class: classeNo(n), transform: `translate(${n.x},${n.y})`, 'data-id': n.id });
  g.style.color = corDoTipo(n.tipo);

  let corpo;
  if (n.tipo === 'switch') {
    corpo = el('rect', { x: -R, y: -R * 0.6, width: R * 2, height: R * 1.2, class: 'node-body' });
  } else if (n.tipo === 'pc') {
    corpo = el('rect', { x: -R, y: -R, width: R * 2, height: R * 2, class: 'node-body' });
  } else {
    corpo = el('circle', { cx: 0, cy: 0, r: R, class: 'node-body' });
  }
  g.appendChild(corpo);
  g.appendChild(glifo(n.tipo));

  const lab = el('text', { class: 'node-label', y: R + 15 });
  lab.textContent = n.id;
  g.appendChild(lab);
  if (n.ip) {
    const ip = el('text', { class: 'node-ip', y: R + 28 });
    ip.textContent = n.ip;
    g.appendChild(ip);
  }

  if (n.id === state.selecionado) g.classList.add('selected');
  if (n.id === state.ligacaoOrigem) g.classList.add('connect-src');
  if (caminhoSet.has(n.id)) g.classList.add('on-path');
  if (state.destaqueAlcancaveis) {
    if (state.destaqueAlcancaveis.has(n.id)) g.classList.add('reachable');
    else g.classList.add('dimmed');
  }

  ligarInteracao(g, n);
  return g;
}

function glifo(tipo) {
  const grupo = el('g', { class: 'node-glyph' });
  if (tipo === 'router') {
    grupo.appendChild(el('path', { d: 'M -9 -4 A 9 9 0 0 1 9 -4', fill: 'none' }));
    grupo.appendChild(el('path', { d: 'M 9 4 A 9 9 0 0 1 -9 4', fill: 'none' }));
    grupo.appendChild(el('path', { d: 'M 9 -8 L 9 -4 L 13 -4', fill: 'none' }));
    grupo.appendChild(el('path', { d: 'M -9 8 L -9 4 L -13 4', fill: 'none' }));
  } else if (tipo === 'switch') {
    grupo.appendChild(el('path', { d: 'M -12 -4 L 12 -4 M 8 -8 L 12 -4 L 8 0', fill: 'none' }));
    grupo.appendChild(el('path', { d: 'M 12 5 L -12 5 M -8 1 L -12 5 L -8 9', fill: 'none' }));
  } else {
    grupo.appendChild(el('rect', { x: -12, y: -11, width: 24, height: 16, fill: 'none' }));
    grupo.appendChild(el('path', { d: 'M -6 9 L 6 9 M 0 5 L 0 9', fill: 'none' }));
  }
  return grupo;
}

function classeNo(n) { return 'node node-' + n.tipo; }
function corDoTipo(t) {
  return t === 'router' ? 'var(--router)' : t === 'switch' ? 'var(--switch)' : 'var(--pc)';
}

/* =====================================================
   Interacao: arrastar + clicar (selecionar / ligar)
   ===================================================== */
function ligarInteracao(g, n) {
  let arrastando = false, moveu = false;
  let offX = 0, offY = 0;

  g.addEventListener('pointerdown', (e) => {
    e.stopPropagation();
    arrastando = true; moveu = false;
    g.classList.add('dragging');
    g.setPointerCapture(e.pointerId);
    const p = pontoSVG(e);
    offX = p.x - n.x; offY = p.y - n.y;
  });

  g.addEventListener('pointermove', (e) => {
    if (!arrastando) return;
    const p = pontoSVG(e);
    const nx = p.x - offX, ny = p.y - offY;
    if (Math.abs(nx - n.x) > 3 || Math.abs(ny - n.y) > 3) moveu = true;
    n.x = nx; n.y = ny;
    g.setAttribute('transform', `translate(${nx},${ny})`);
    atualizarArestasDe(n.id);   // <- so move as linhas, NAO recria os nos
  });

  g.addEventListener('pointerup', async (e) => {
    if (!arrastando) return;
    arrastando = false;
    g.classList.remove('dragging');
    g.releasePointerCapture(e.pointerId);
    if (moveu) {
      try { await api('PATCH', '/dispositivos/' + encodeURIComponent(n.id), { x: n.x, y: n.y }); }
      catch (err) { toast(err.message, true); }
    } else {
      aoClicarNo(n);
    }
  });
}

function aoClicarNo(n) {
  if (state.modoLigacao) {
    if (!state.ligacaoOrigem) { state.ligacaoOrigem = n.id; desenhar(); }
    else if (state.ligacaoOrigem === n.id) { state.ligacaoOrigem = null; desenhar(); }
    else {
      const a = noPorId(state.ligacaoOrigem), b = n;
      // latencia/distancia so faz sentido entre routers (backbone WAN);
      // ligacoes que envolvem PC/Switch sao LAN -> peso fixo 1, sem perguntar
      if (a && b && a.tipo === 'router' && b.tipo === 'router') {
        abrirModalPeso(state.ligacaoOrigem, n.id);
      } else {
        criarLigacaoDireta(state.ligacaoOrigem, n.id);
      }
    }
  } else {
    selecionar(n.id);
  }
}

async function criarLigacaoDireta(origem, destino) {
  try {
    await api('POST', '/arestas', { origem, destino, peso: 1 });
    state.ligacaoOrigem = null;
    await refrescar();
    toast('Ligacao criada.');
  } catch (e) {
    toast(e.message, true);
    state.ligacaoOrigem = null; desenhar();
  }
}

/* Atualiza apenas as arestas ligadas ao no movido (sem recriar elementos). */
function atualizarArestasDe(id) {
  canvas.querySelectorAll('g.edge-group').forEach(gr => {
    if (gr.getAttribute('data-o') === id || gr.getAttribute('data-d') === id) {
      posicionarAresta(gr);
    }
  });
}

/* coordenadas do rato no espaco do SVG */
function pontoSVG(evt) {
  const pt = canvas.createSVGPoint();
  pt.x = evt.clientX; pt.y = evt.clientY;
  return pt.matrixTransform(canvas.getScreenCTM().inverse());
}

canvas.addEventListener('click', () => {
  if (state.modoLigacao && state.ligacaoOrigem) { state.ligacaoOrigem = null; desenhar(); }
  else if (state.selecionado) { selecionar(null); }
});

/* =====================================================
   Selecao + configuracao do dispositivo
   ===================================================== */
function selecionar(id) {
  state.selecionado = id;
  const card = $('#cardConfig');
  if (!id) { card.hidden = true; desenhar(); return; }
  const n = noPorId(id);
  if (!n) { card.hidden = true; return; }
  card.hidden = false;
  $('#cfgId').textContent = n.id;
  $('#cfgTipo').textContent = n.tipo;
  $('#cfgIp').value = n.ip || '';
  preencherLigacoes(id);
  desenhar();
}

/* Lista as ligacoes do dispositivo, cada uma com um botao para remover. */
function preencherLigacoes(id) {
  const cont = $('#cfgLigacoes');
  cont.innerHTML = '';
  const ligs = state.arestas.filter(a => a.origem === id || a.destino === id);
  if (ligs.length === 0) {
    cont.innerHTML = '<p class="muted">Sem ligacoes.</p>';
    return;
  }
  const eu = noPorId(id);
  ligs.forEach(a => {
    const outroId = a.origem === id ? a.destino : a.origem;
    const outro = noPorId(outroId);
    const mostraPeso = eu && outro && eu.tipo === 'router' && outro.tipo === 'router';
    const row = document.createElement('div');
    row.className = 'link-row';
    const txt = document.createElement('span');
    txt.textContent = outroId + (mostraPeso ? ` (${formatarPeso(a.peso)})` : '');
    const btn = document.createElement('button');
    btn.className = 'btn link-x';
    btn.textContent = 'X';
    btn.title = 'Remover ligacao';
    btn.onclick = () => removerArestaDireta(a);
    row.appendChild(txt);
    row.appendChild(btn);
    cont.appendChild(row);
  });
}

async function removerArestaDireta(a) {
  try {
    await api('DELETE', `/arestas?origem=${encodeURIComponent(a.origem)}&destino=${encodeURIComponent(a.destino)}`);
    limparDestaques();
    await refrescar();
    toast('Ligacao removida.');
  } catch (e) { toast(e.message, true); }
}

$('#btnSalvarIp').addEventListener('click', async () => {
  if (!state.selecionado) return;
  const ip = $('#cfgIp').value.trim();
  try {
    await api('PATCH', '/dispositivos/' + encodeURIComponent(state.selecionado), { ip });
    toast('IP guardado.');
    await refrescar();
  } catch (e) { toast(e.message, true); }
});

$('#btnRemoverDisp').addEventListener('click', async () => {
  if (!state.selecionado) return;
  try {
    await api('DELETE', '/dispositivos/' + encodeURIComponent(state.selecionado));
    state.selecionado = null;
    $('#cardConfig').hidden = true;
    limparDestaques();
    await refrescar();
  } catch (e) { toast(e.message, true); }
});

/* =====================================================
   Barra: adicionar dispositivos
   ===================================================== */
document.querySelectorAll('[data-add]').forEach(btn => {
  btn.addEventListener('click', async () => {
    const tipo = btn.getAttribute('data-add');
    try {
      const no = await api('POST', '/dispositivos?tipo=' + tipo);
      const k = state.nos.length;
      const x = 90 + (k % 6) * 95;
      const y = 80 + Math.floor(k / 6) * 110;
      await api('PATCH', '/dispositivos/' + encodeURIComponent(no.id), { x, y });
      await refrescar();
      toast(no.id + ' adicionado.');
    } catch (e) { toast(e.message, true); }
  });
});

/* modo ligacao */
$('#btnModoLigacao').addEventListener('click', () => {
  state.modoLigacao = !state.modoLigacao;
  state.ligacaoOrigem = null;
  $('#btnModoLigacao').classList.toggle('active', state.modoLigacao);
  canvasWrap.classList.toggle('connect', state.modoLigacao);
  $('#legendaModo').textContent = state.modoLigacao
    ? 'Modo ligacao: clique em dois dispositivos' : '';
  if (!state.modoLigacao) selecionar(state.selecionado);
  desenhar();
});

/* exemplo / limpar / guardar / carregar */
$('#btnExemplo').addEventListener('click', async () => {
  try { await api('POST', '/topologia/exemplo'); limparDestaques(); await refrescar(); toast('Exemplo carregado.'); }
  catch (e) { toast(e.message, true); }
});
$('#btnLimpar').addEventListener('click', async () => {
  if (!confirm('Limpar toda a topologia?')) return;
  try { await api('DELETE', '/topologia'); selecionar(null); limparDestaques(); await refrescar(); }
  catch (e) { toast(e.message, true); }
});
$('#btnGuardar').addEventListener('click', async () => {
  try { const r = await api('POST', '/topologia/guardar'); toast(r.mensagem || 'Guardado.'); }
  catch (e) { toast(e.message, true); }
});
$('#btnCarregar').addEventListener('click', async () => {
  try { await api('POST', '/topologia/carregar'); limparDestaques(); await refrescar(); toast('Topologia carregada.'); }
  catch (e) { toast(e.message, true); }
});

/* =====================================================
   Arestas: criar (via dialogo) e remover
   ===================================================== */
function abrirModalPeso(origem, destino) {
  $('#modalPesoTexto').textContent = `${origem}  <->  ${destino}`;
  $('#inputPeso').value = '1';
  $('#modalPeso').hidden = false;
  setTimeout(() => $('#inputPeso').focus(), 50);

  const overlay = $('#modalPeso');
  const ok = $('#modalPesoOk'), cancel = $('#modalPesoCancel'), x = $('#modalPesoX');
  const fechar = () => {
    $('#modalPeso').hidden = true;
    ok.onclick = null; cancel.onclick = null; x.onclick = null;
    overlay.onclick = null; document.removeEventListener('keydown', onKey);
    state.ligacaoOrigem = null; desenhar();
  };
  const onKey = (e) => {
    if (e.key === 'Escape') fechar();
    if (e.key === 'Enter') ok.onclick();
  };
  ok.onclick = async () => {
    const peso = parseFloat($('#inputPeso').value);
    if (isNaN(peso) || peso < 0) { toast('Peso invalido.', true); return; }
    try {
      await api('POST', '/arestas', { origem, destino, peso });
      fechar(); await refrescar(); toast('Ligacao criada.');
    } catch (e) { toast(e.message, true); }
  };
  cancel.onclick = fechar;
  x.onclick = fechar;
  overlay.onclick = (e) => { if (e.target === overlay) fechar(); };
  document.addEventListener('keydown', onKey);
}

async function removerAresta(a) {
  if (state.modoLigacao) return;
  if (!confirm(`Remover ligacao ${a.origem} - ${a.destino} (${formatarPeso(a.peso)})?`)) return;
  try {
    await api('DELETE', `/arestas?origem=${encodeURIComponent(a.origem)}&destino=${encodeURIComponent(a.destino)}`);
    limparDestaques(); await refrescar();
  } catch (e) { toast(e.message, true); }
}

/* =====================================================
   Algoritmos
   ===================================================== */
function preencherSelects() {
  const origem = $('#selOrigem'), destino = $('#selDestino');
  const vo = origem.value, vd = destino.value;
  const ops = state.nos.map(n => `<option value="${n.id}">${n.id}</option>`).join('');
  origem.innerHTML = ops; destino.innerHTML = ops;
  if (state.nos.find(n => n.id === vo)) origem.value = vo;
  if (state.nos.find(n => n.id === vd)) destino.value = vd;
  else if (state.nos.length > 1) destino.selectedIndex = state.nos.length - 1;
}

document.querySelectorAll('[data-algo]').forEach(btn => {
  btn.addEventListener('click', () => executarAlgoritmo(btn.getAttribute('data-algo')));
});

async function executarAlgoritmo(algo) {
  limparDestaques();
  const origem = $('#selOrigem').value;
  const destino = $('#selDestino').value;
  const box = $('#resultado');
  box.className = 'resultado';

  try {
    if (algo === 'conectividade') {
      const r = await api('GET', '/algoritmos/conectividade');
      mostrarConectividade(r);
    } else if (algo === 'dfs') {
      if (!origem) return toast('Escolha a origem.', true);
      const r = await api('GET', '/algoritmos/dfs?origem=' + encodeURIComponent(origem));
      state.destaqueAlcancaveis = new Set(r.alcancaveis);
      mostrarDfs(r);
      desenhar();
    } else {
      if (!origem || !destino) return toast('Escolha origem e destino.', true);
      if (origem === destino) return toast('Origem e destino iguais.', true);
      const r = await api('GET', `/algoritmos/${algo}?origem=${encodeURIComponent(origem)}&destino=${encodeURIComponent(destino)}`);
      state.destaqueCaminho = r.existe ? r.caminho : [];
      mostrarCaminho(algo, r);
      desenhar();
    }
  } catch (e) { toast(e.message, true); }
}

function mostrarCaminho(algo, r) {
  const box = $('#resultado');
  const nome = algo === 'bfs' ? 'BFS' : 'Dijkstra';
  if (!r.existe) {
    box.className = 'resultado fail';
    box.innerHTML = `<p class="res-title">[X] ${nome}</p><p>${r.mensagem}</p>`;
    return;
  }
  const hops = r.caminho.map(id => `<span class="hop">${id}</span>`).join('<span class="arrow">&rarr;</span>');
  const metrica = algo === 'bfs'
    ? `Saltos: <b>${r.saltos}</b>`
    : `Custo total: <b>${formatarPeso(r.custo)}</b>`;
  box.innerHTML =
    `<p class="res-title">${nome}: ${algo === 'bfs' ? 'menos saltos' : 'menor custo'}</p>
     <div class="path">${hops}</div>
     <p class="metric">${metrica}</p>`;
}

function mostrarDfs(r) {
  const box = $('#resultado');
  const lista = r.alcancaveis.map(id => `<span class="hop">${id}</span>`).join(' ');
  box.innerHTML =
    `<p class="res-title">DFS a partir de ${r.origem}</p>
     <p class="metric">Alcancaveis: <b>${r.total}</b> dispositivo(s)</p>
     <div class="path">${lista}</div>`;
}

function mostrarConectividade(r) {
  const box = $('#resultado');
  box.className = 'resultado' + (r.conectado ? '' : ' fail');
  let comps = '';
  if (!r.conectado) {
    comps = '<ul class="comp-list">' + r.componentes.map((c, i) =>
      `<li><b>Componente ${i + 1}</b> (${c.length}): ${c.join(', ')}</li>`).join('') + '</ul>';
  }
  box.innerHTML =
    `<p class="res-title">Teste de conectividade (DFS)</p>
     <p>${r.mensagem}</p>${comps}`;
}

/* =====================================================
   Utilidades
   ===================================================== */
function limparDestaques() { state.destaqueCaminho = []; state.destaqueAlcancaveis = null; }
function parKey(a, b) { return a < b ? a + '|' + b : b + '|' + a; }
function formatarPeso(p) { return Number.isInteger(p) ? String(p) : String(Math.round(p * 100) / 100); }

function el(tag, attrs) {
  const e = document.createElementNS(SVGNS, tag);
  for (const k in attrs) e.setAttribute(k, attrs[k]);
  return e;
}

let toastTimer;
function toast(msg, erro) {
  const t = $('#toast');
  t.textContent = msg;
  t.className = 'toast show' + (erro ? ' error' : '');
  t.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.className = 'toast'; }, 3200);
}

/* arranque */
refrescar().catch(e => toast('Nao foi possivel ligar ao servidor: ' + e.message, true));
