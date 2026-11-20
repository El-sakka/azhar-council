/* Shared helpers: API calls, session guard, page header & navigation. */
const Api = {
  async request(url, options = {}) {
    const res = await fetch(url, {
      credentials: 'same-origin',
      headers: { 'Accept': 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}) },
      ...options,
    });
    if (res.status === 401 && !url.startsWith('/api/auth/login')) {
      location.replace('/login.html');
      return new Promise(() => {});   // page is navigating away; never settle

    }
    const data = res.status === 204 ? null : await res.json().catch(() => null);
    if (!res.ok) throw new Error((data && data.message) || 'حدث خطأ غير متوقع');
    return data;
  },
  get(url) { return this.request(url); },
  post(url, body) { return this.request(url, { method: 'POST', body: body ? JSON.stringify(body) : undefined }); },
};

function esc(value) {
  return value == null ? '' : String(value)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/** Shows "—" for empty values. */
function val(value) {
  return value == null || value === '' ? '—' : esc(value);
}

const NAV = [
  { id: 'home', href: '/index.html#home', label: 'الرئيسية' },
  { id: 'intro', href: '/index.html#intro', label: 'مقدمة' },
  { id: 'goal', href: '/index.html#goal', label: 'الهدف من إنشاء قاعدة البيانات' },
  { id: 'guide', href: '/index.html#guide', label: 'دليل استخدام قاعدة البيانات' },
  { id: 'search', href: '/search.html', label: 'خيارات البحث' },
];

/** Renders the top bar and navigation into #shell, verifies the session and returns the user. */
async function initShell(active) {
  const shell = document.getElementById('shell');
  shell.innerHTML = `
    <header class="top">
      <a class="brand" href="/index.html#home">
        <span class="mark" aria-hidden="true">۞</span>
        <div><b>قاعدة بيانات وثائق وسجلات المجلس الأعلى للأزهر</b>
          <div class="small">المحفوظة بمكتبة الأزهر الشريف</div></div>
      </a>
      <div class="user">
        <span class="pill" id="userName">…</span>
        <button class="btn soft" id="logoutBtn" type="button">تسجيل الخروج</button>
      </div>
    </header>
    <nav class="nav" aria-label="القائمة الرئيسية">
      ${NAV.map(n => `<a href="${n.href}" data-nav="${n.id}" class="${n.id === active ? 'active' : ''}">${n.label}</a>`).join('')}
    </nav>`;
  document.getElementById('logoutBtn').addEventListener('click', async () => {
    try { await Api.post('/api/auth/logout'); } finally { location.replace('/login.html'); }
  });
  const user = await Api.get('/api/auth/me');
  document.getElementById('userName').textContent = user.displayName || user.username;
  return user;
}

function setActiveNav(id) {
  document.querySelectorAll('[data-nav]').forEach(a => a.classList.toggle('active', a.dataset.nav === id));
}
