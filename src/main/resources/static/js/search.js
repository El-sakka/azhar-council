/* خيارات البحث: inventory search (whole list), subject browsing, Excel export, print and the record viewer. */
const PAGE_SIZE = 25;
const $ = id => document.getElementById(id);

const state = {
  tree: [],
  byId: new Map(),
  subjectId: null,
  q: '', field: 'all', yearFrom: '', yearTo: '',
  tab: 'files', page: 0,
  viewList: [],      // rows currently shown, used by the viewer's previous/next
  lastResult: null,  // last subject result (for printing)
};

const FILE_COLUMNS = [
  ['رقم الملف', f => val(f.fileNumber)],
  ['رمز الإرجاع', f => val(f.referenceCode), 'ref'],
  ['عنوان الملف', f => val(f.title), 'wide'],
  ['لون الغلاف', f => val(f.coverColor)],
  ['مقاس الملف', f => val(f.fileSize)],
  ['تاريخ بداية الملف', f => val(f.startDate)],
  ['تاريخ نهاية الملف', f => val(f.endDate)],
  ['عدد الأوراق', f => val(f.sheetCount)],
  ['لغة الكتابة', f => val(f.language)],
  ['المحتوى الموضوعي', f => val(f.subjectContent), 'wide'],
  ['الحالة المادية', f => val(f.physicalCondition)],
  ['ملاحظات', f => val(f.notes)],
];
const REGISTER_COLUMNS = [
  ['رقم السجل', r => val(r.registerNumber)],
  ['رمز الإرجاع', r => val(r.referenceCode), 'ref'],
  ['عنوان السجل', r => val(r.title), 'wide'],
  ['اسم المتكاملة الفرعية', r => val(r.subUnitName)],
  ['صادر / وارد', r => val(r.direction)],
  ['تاريخ بداية السجل', r => val(r.startDate)],
  ['تاريخ نهاية السجل', r => val(r.endDate)],
  ['موضوعات السجل', r => val(r.subjectsText), 'wide'],
  ['الأوراق: مكتوب', r => val(r.writtenSheets)],
  ['الأوراق: فارغ', r => val(r.blankSheets)],
  ['لغة الكتابة', r => val(r.language)],
  ['نوع الخط', r => val(r.scriptType)],
  ['الطول (سم)', r => val(r.lengthCm)],
  ['العرض (سم)', r => val(r.widthCm)],
  ['الحالة المادية', r => val(r.physicalCondition)],
];
const entryColumns = type => [
  [type === 'FILE' ? 'رقم الملف' : 'رقم السجل', e => val(e.itemNumber)],
  ['رمز الإرجاع', e => val(e.referenceCode), 'ref'],
  [type === 'FILE' ? 'عنوان الملف' : 'عنوان السجل', e => val(e.title), 'wide'],
  ['التاريخ', e => val(e.dateText)],
  ['المحتوى الموضوعي', e => val(e.subjectContent), 'wide'],
];

/* ---------- query string <-> state ---------- */
function criteriaParams() {
  const p = new URLSearchParams();
  if (state.q) p.set('q', state.q);
  if (state.field !== 'all') p.set('field', state.field);
  if (state.yearFrom) p.set('yearFrom', state.yearFrom);
  if (state.yearTo) p.set('yearTo', state.yearTo);
  return p;
}

function syncUrl() {
  const p = criteriaParams();
  if (state.subjectId) p.set('subject', state.subjectId);
  if (!state.subjectId && state.tab !== 'files') p.set('tab', state.tab);
  if (!state.subjectId && state.page) p.set('page', state.page);
  history.replaceState(null, '', location.pathname + (p.toString() ? '?' + p : ''));
}

function readUrl() {
  const p = new URLSearchParams(location.search);
  state.q = p.get('q') || '';
  state.field = p.get('field') || 'all';
  state.yearFrom = p.get('yearFrom') || '';
  state.yearTo = p.get('yearTo') || '';
  state.tab = p.get('tab') === 'registers' ? 'registers' : 'files';
  state.page = Math.max(0, parseInt(p.get('page') || '0', 10) || 0);
  const s = parseInt(p.get('subject') || '', 10);
  state.subjectId = state.byId.has(s) ? s : null;
}

/* ---------- subject combo boxes ---------- */
function indexTree(nodes, parent = null) {
  nodes.forEach(n => {
    state.byId.set(n.id, { ...n, parent });
    indexTree(n.children, n.id);
  });
}

function option(n) {
  return `<option value="${n.id}">${esc(n.code)} - ${esc(n.name)} (${n.fileCount} ملف، ${n.registerCount} سجل)</option>`;
}

function fillSelect(select, nodes, allLabel) {
  select.innerHTML = `<option value="">${allLabel}</option>` + nodes.map(option).join('');
  select.disabled = nodes.length === 0;
}

/** Sets the three selects so that they show the given subject. */
function showSubjectInSelects(subjectId) {
  const chain = [];
  for (let id = subjectId; id; id = state.byId.get(id).parent) chain.unshift(id);
  const [main, sub, detail] = chain;
  $('subjectMain').value = main || '';
  const mainNode = main && state.byId.get(main);
  if (mainNode) fillSelect($('subjectSub'), mainNode.children, mainNode.children.length ? 'كل الموضوعات الفرعية' : 'لا توجد موضوعات فرعية');
  else { fillSelect($('subjectSub'), [], 'اختر الموضوع الرئيسي أولًا'); }
  $('subjectSub').value = sub || '';
  const subNode = sub && state.byId.get(sub);
  const hasDetail = subNode && subNode.children.length > 0;
  $('subjectDetailWrap').classList.toggle('hidden', !hasDetail);
  if (hasDetail) fillSelect($('subjectDetail'), subNode.children, 'كل الموضوعات التفصيلية');
  $('subjectDetail').value = detail || '';
}

function selectedSubject() {
  return parseInt($('subjectDetail').value || $('subjectSub').value || $('subjectMain').value, 10) || null;
}

/* ---------- rendering ---------- */
function table(columns, rows, startNo, rowAttr) {
  return `<div class="table-wrap"><table>
    <thead><tr><th>م</th>${columns.map(c => `<th>${c[0]}</th>`).join('')}</tr></thead>
    <tbody>${rows.map((r, i) => `<tr ${rowAttr ? rowAttr(r, i) : ''}><td>${startNo + i}</td>${
      columns.map(c => `<td class="${c[2] || ''}">${c[1](r)}</td>`).join('')}</tr>`).join('')}</tbody>
  </table></div>`;
}

function emptyState(text) {
  return `<div class="empty"><div class="icon"><svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="M15.5 15.5 21 21"/></svg></div><h3>لا توجد نتائج مطابقة</h3><p>${text}</p></div>`;
}

function criteriaText() {
  const parts = [];
  const fieldLabel = { all: 'كل المداخل', title: 'العنوان', subject: 'الموضوع', year: 'السنة' }[state.field];
  if (state.q) parts.push(`${fieldLabel}: «${esc(state.q)}»`);
  if (state.yearFrom || state.yearTo) parts.push(`الفترة: ${esc(state.yearFrom || '…')} – ${esc(state.yearTo || '…')}`);
  return parts.join(' • ');
}

function setSummary(files, registers) {
  const crit = criteriaText();
  $('summary').innerHTML = `
    <span class="pill">عدد الملفات: <b>${files}</b></span>
    <span class="pill">عدد السجلات: <b>${registers}</b></span>
    ${crit ? `<span class="pill">${crit}</span>` : ''}`;
}

async function runSearch() {
  syncUrl();
  $('results').classList.add('loading');
  try {
    if (state.subjectId) await renderSubject();
    else await renderInventory();
  } catch (err) {
    $('resultsBody').innerHTML = `<div class="notice gap">${esc(err.message)}</div>`;
  } finally {
    $('results').classList.remove('loading');
  }
}

async function renderInventory() {
  $('tabs').classList.remove('hidden');
  $('pagerRow').classList.remove('hidden');
  $('resultsScope').textContent = 'البحث بتاريخ الملف والعنوان — قائمة الحصر ككل';
  $('resultsTitle').textContent = state.tab === 'files'
    ? 'قائمة حصر ملفات المجلس الأعلى للأزهر المودعة بمكتبة الأزهر'
    : 'قائمة حصر سجلات المجلس الأعلى للأزهر المودعة بمكتبة الأزهر';
  document.querySelectorAll('[data-tab]').forEach(t => t.classList.toggle('active', t.dataset.tab === state.tab));

  const p = criteriaParams();
  const pageParams = new URLSearchParams(p); pageParams.set('page', state.page); pageParams.set('size', PAGE_SIZE);
  const countParams = new URLSearchParams(p); countParams.set('size', 1);
  const isFiles = state.tab === 'files';
  const [active, other] = await Promise.all([
    Api.get(`/api/${isFiles ? 'files' : 'registers'}?${pageParams}`),
    Api.get(`/api/${isFiles ? 'registers' : 'files'}?${countParams}`),
  ]);
  if (active.totalPages > 0 && state.page >= active.totalPages) { state.page = active.totalPages - 1; return runSearch(); }

  const fileTotal = isFiles ? active.total : other.total;
  const registerTotal = isFiles ? other.total : active.total;
  $('countFiles').textContent = fileTotal;
  $('countRegisters').textContent = registerTotal;
  setSummary(fileTotal, registerTotal);

  const type = isFiles ? 'FILE' : 'REGISTER';
  state.viewList = active.items.map(item => ({ type, id: item.id }));
  $('resultsBody').innerHTML = active.items.length
    ? table(isFiles ? FILE_COLUMNS : REGISTER_COLUMNS, active.items, state.page * PAGE_SIZE + 1,
            (r, i) => `data-view="${i}" tabindex="0" title="عرض التسجيلة"`)
    : emptyState('جرّب كلمة أخرى أو مدخل بحث مختلف أو فترة زمنية أوسع.');

  const from = active.total ? state.page * PAGE_SIZE + 1 : 0;
  $('pageInfo').textContent = `عرض ${from} – ${state.page * PAGE_SIZE + active.items.length} من ${active.total}`;
  $('pageNo').textContent = active.totalPages ? `صفحة ${state.page + 1} من ${active.totalPages}` : '';
  $('prevPage').disabled = state.page === 0;
  $('nextPage').disabled = state.page + 1 >= active.totalPages;
}

async function renderSubject() {
  $('tabs').classList.add('hidden');
  $('pagerRow').classList.add('hidden');
  const res = await Api.get(`/api/subjects/${state.subjectId}/entries?${criteriaParams()}`);
  state.lastResult = res;
  $('resultsScope').textContent = 'البحث بالموضوعات';
  $('resultsTitle').textContent = `${res.code} - ${res.path}`;
  setSummary(res.fileCount, res.registerCount);

  state.viewList = [];
  const rowAttr = e => {
    state.viewList.push({ type: e.type, id: e.type === 'FILE' ? e.archiveFileId : e.archiveRegisterId, entry: e });
    return `data-view="${state.viewList.length - 1}" tabindex="0" title="عرض التسجيلة"`;
  };
  $('resultsBody').innerHTML = res.groups.length ? res.groups.map(g => `
    <div class="group">
      <h3><span class="code">${esc(g.code)}</span>${esc(g.name)}</h3>
      ${g.path !== g.name ? `<div class="small">${esc(g.path)}</div>` : ''}
      ${g.files.length ? `<h4>أولًا: الملفات (${g.files.length})</h4>${table(entryColumns('FILE'), g.files, 1, rowAttr)}` : ''}
      ${g.registers.length ? `<h4>${g.files.length ? 'ثانيًا' : 'أولًا'}: السجلات (${g.registers.length})</h4>${
        table(entryColumns('REGISTER'), g.registers, g.files.length + 1, rowAttr)}` : ''}
    </div>`).join('') : emptyState('لا توجد ملفات أو سجلات تطابق البحث داخل هذا الموضوع.');
}

/* ---------- export & print ---------- */
function exportExcel() {
  const p = criteriaParams();
  location.href = state.subjectId ? `/api/export/subjects/${state.subjectId}?${p}` : `/api/export/inventory?${p}`;
}

async function printResults() {
  const head = `<div class="print-head">
      <h2>قاعدة بيانات وثائق وسجلات المجلس الأعلى للأزهر — المحفوظة بمكتبة الأزهر الشريف</h2>
      <div>تقرير نتائج البحث • ${new Date().toLocaleDateString('ar-EG')}</div></div>`;
  let body;
  if (state.subjectId) {
    body = $('results').innerHTML;
  } else {
    // print every matching row of the active list, not only the current page
    const p = criteriaParams(); p.set('size', 1000);
    const isFiles = state.tab === 'files';
    const all = await Api.get(`/api/${isFiles ? 'files' : 'registers'}?${p}`);
    body = `<h3>${esc($('resultsTitle').textContent)}</h3><div class="summary">${$('summary').innerHTML}</div>
      ${table(isFiles ? FILE_COLUMNS : REGISTER_COLUMNS, all.items, 1)}`;
  }
  $('printArea').innerHTML = head + body;
  document.body.classList.add('printing');
  window.print();
}
window.addEventListener('afterprint', () => {
  document.body.classList.remove('printing');
  $('printArea').innerHTML = '';
});

/* ---------- record viewer ---------- */
const viewer = { index: 0, zoom: 1, token: 0 };

function detail(label, value, full) {
  return `<div class="detail${full ? ' full' : ''}"><label>${label}</label>${val(value)}</div>`;
}

async function openViewer(index) {
  if (index < 0 || index >= state.viewList.length) return;
  viewer.index = index;
  const item = state.viewList[index];
  $('viewer').classList.remove('hidden');
  $('viewPos').textContent = `${index + 1} من ${state.viewList.length}`;
  $('viewPrev').disabled = index === 0;
  $('viewNext').disabled = index === state.viewList.length - 1;
  const box = $('viewerContent');
  const token = ++viewer.token;   // ignore responses that arrive after the user moved on
  box.innerHTML = '<p class="small">جارٍ التحميل…</p>';
  let html;
  try {
    if (!item.id && item.entry) html = entryView(item.entry);
    else if (item.type === 'FILE') html = fileView(await Api.get(`/api/files/${item.id}`));
    else html = registerView(await Api.get(`/api/registers/${item.id}`));
  } catch (err) {
    html = `<div class="notice">${esc(err.message)}</div>`;
  }
  if (token !== viewer.token) return;
  box.innerHTML = html;
  $('viewClose').focus();
}

function fileView(f) {
  return `<span class="pill">ملف أرشيفي</span>
    <h2 id="viewerTitle" style="margin-top:12px">${esc(f.title)}</h2>
    <p class="small">رمز الإرجاع: ${val(f.referenceCode)}</p>
    <div class="details">
      ${detail('رقم الملف', f.fileNumber)}${detail('تاريخ بداية الملف', f.startDate)}${detail('تاريخ نهاية الملف', f.endDate)}
      ${detail('لون الغلاف', f.coverColor)}${detail('مقاس الملف', f.fileSize)}${detail('عدد الأوراق', f.sheetCount)}
      ${detail('لغة الكتابة', f.language)}${detail('الحالة المادية', f.physicalCondition)}${detail('ملاحظات', f.notes)}
      ${detail('المحتوى الموضوعي', f.subjectContent, true)}
    </div>
    <div class="notice gap">اطلاع فقط — لا يمكن تحميل الوثائق.</div>`;
}

function registerView(r) {
  return `<span class="pill">سجل أرشيفي</span>
    <h2 id="viewerTitle" style="margin-top:12px">${esc(r.title)}</h2>
    <p class="small">رمز الإرجاع: ${val(r.referenceCode)}</p>
    <div class="details">
      ${detail('رقم السجل', r.registerNumber)}${detail('اسم المتكاملة الفرعية', r.subUnitName)}${detail('صادر / وارد', r.direction)}
      ${detail('تاريخ بداية السجل', r.startDate)}${detail('تاريخ نهاية السجل', r.endDate)}${detail('لغة الكتابة', r.language)}
      ${detail('الأوراق المكتوبة', r.writtenSheets)}${detail('الأوراق الفارغة', r.blankSheets)}${detail('نوع الخط', r.scriptType)}
      ${detail('الطول (سم)', r.lengthCm)}${detail('العرض (سم)', r.widthCm)}${detail('الحالة المادية', r.physicalCondition)}
      ${detail('موضوعات السجل', r.subjectsText, true)}
    </div>
    <div class="notice gap">اطلاع فقط — لا يمكن تحميل الوثائق.</div>`;
}

function entryView(e) {
  return `<span class="pill">${e.type === 'FILE' ? 'ملف' : 'سجل'}</span>
    <h2 id="viewerTitle" style="margin-top:12px">${esc(e.title)}</h2>
    <div class="details">${detail('الرقم', e.itemNumber)}${detail('رمز الإرجاع', e.referenceCode)}${detail('التاريخ', e.dateText)}
      ${detail('المحتوى الموضوعي', e.subjectContent, true)}</div>`;
}

function setZoom(z) {
  viewer.zoom = Math.min(1.8, Math.max(0.7, Math.round(z * 10) / 10));
  $('viewerContent').style.fontSize = `${viewer.zoom * 14}px`;
}

function closeViewer() {
  $('viewer').classList.add('hidden');
  const row = document.querySelector(`[data-view="${viewer.index}"]`);
  if (row) row.focus();
}

/* ---------- wiring ---------- */
function readForm() {
  state.q = $('q').value.trim();
  state.field = $('field').value;
  state.yearFrom = $('yearFrom').value.trim();
  state.yearTo = $('yearTo').value.trim();
}

function writeForm() {
  $('q').value = state.q;
  $('field').value = state.field;
  $('yearFrom').value = state.yearFrom;
  $('yearTo').value = state.yearTo;
}

function onSubjectChange(level) {
  if (level === 'main') { $('subjectSub').value = ''; $('subjectDetail').value = ''; }
  if (level === 'sub') $('subjectDetail').value = '';
  const id = selectedSubject();
  showSubjectInSelects(id);
  state.subjectId = id;
  state.page = 0;
  runSearch();
}

(async () => {
  await initShell('search');
  state.tree = await Api.get('/api/subjects');
  indexTree(state.tree);
  fillSelect($('subjectMain'), state.tree, 'كل الموضوعات — قائمة الحصر ككل');
  $('subjectMain').disabled = false;
  readUrl();
  writeForm();
  showSubjectInSelects(state.subjectId);

  $('subjectMain').addEventListener('change', () => onSubjectChange('main'));
  $('subjectSub').addEventListener('change', () => onSubjectChange('sub'));
  $('subjectDetail').addEventListener('change', () => onSubjectChange('detail'));
  $('searchForm').addEventListener('submit', e => { e.preventDefault(); readForm(); state.page = 0; runSearch(); });
  $('clearBtn').addEventListener('click', () => {
    Object.assign(state, { q: '', field: 'all', yearFrom: '', yearTo: '', subjectId: null, page: 0, tab: 'files' });
    writeForm();
    showSubjectInSelects(null);
    runSearch();
  });
  document.querySelectorAll('[data-tab]').forEach(t => t.addEventListener('click', () => {
    state.tab = t.dataset.tab; state.page = 0; runSearch();
  }));
  $('prevPage').addEventListener('click', () => { state.page--; runSearch(); });
  $('nextPage').addEventListener('click', () => { state.page++; runSearch(); });
  $('exportBtn').addEventListener('click', exportExcel);
  $('printBtn').addEventListener('click', printResults);

  $('resultsBody').addEventListener('click', e => {
    const row = e.target.closest('[data-view]');
    if (row) openViewer(Number(row.dataset.view));
  });
  $('resultsBody').addEventListener('keydown', e => {
    const row = e.target.closest('[data-view]');
    if (row && e.key === 'Enter') openViewer(Number(row.dataset.view));
  });
  $('zoomIn').addEventListener('click', () => setZoom(viewer.zoom + 0.1));
  $('zoomOut').addEventListener('click', () => setZoom(viewer.zoom - 0.1));
  $('viewPrev').addEventListener('click', () => openViewer(viewer.index - 1));
  $('viewNext').addEventListener('click', () => openViewer(viewer.index + 1));
  $('viewClose').addEventListener('click', closeViewer);
  $('viewer').addEventListener('click', e => { if (e.target === $('viewer')) closeViewer(); });
  document.addEventListener('keydown', e => {
    if ($('viewer').classList.contains('hidden')) return;
    if (e.key === 'Escape') closeViewer();
    else if (e.key === 'ArrowLeft') openViewer(viewer.index + 1);   // RTL: left = forward
    else if (e.key === 'ArrowRight') openViewer(viewer.index - 1);
    else if (e.key === '+' || e.key === '=') setZoom(viewer.zoom + 0.1);
    else if (e.key === '-') setZoom(viewer.zoom - 0.1);
  });

  runSearch();
})();
