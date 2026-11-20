const API = 'http://localhost:8080/api';

// ── Token helpers ──────────────────────────────────────────────
function getToken() { return localStorage.getItem('token'); }
function setToken(t) { localStorage.setItem('token', t); }
function clearToken() { localStorage.removeItem('token'); }

function authHeaders() {
    return { 'Content-Type': 'application/json', 'Authorization': `Bearer ${getToken()}` };
}

// ── API calls ──────────────────────────────────────────────────
async function apiFetch(path, options = {}) {
    const res = await fetch(API + path, options);
    if (res.status === 204) return null;
    const data = await res.json();
    if (!res.ok) throw data;
    return data;
}

// ── Auth ───────────────────────────────────────────────────────
async function login() {
    const email    = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    try {
        const data = await apiFetch('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        setToken(data.accessToken);
        await afterLogin();
    } catch (err) {
        showError('login-form', err.message || 'Błędny email lub hasło');
    }
}

async function register() {
    const body = {
        firstName: document.getElementById('reg-firstname').value,
        lastName:  document.getElementById('reg-lastname').value,
        email:     document.getElementById('reg-email').value,
        password:  document.getElementById('reg-password').value
    };
    try {
        await apiFetch('/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        alert('Konto zostało utworzone! Możesz się teraz zalogować.');
        toggleAuthForms();
    } catch (err) {
        const msg = err.fieldErrors
            ? Object.values(err.fieldErrors).join(', ')
            : (err.message || 'Błąd rejestracji');
        showError('register-form', msg);
    }
}

async function afterLogin() {
    const me = await apiFetch('/auth/me', { headers: authHeaders() });
    const role = me.role || '';

    document.getElementById('user-name').textContent = `${me.firstName} ${me.lastName}`;
    document.getElementById('user-role').textContent = role;
    document.getElementById('user-info').classList.remove('hidden');

    const isAdmin      = role === 'ADMIN';
    const isReader     = role === 'READER';
    const isLibrarian  = role === 'LIBRARIAN';

    document.getElementById('nav-reservations').classList.toggle('hidden', !isReader);
    document.getElementById('nav-history').classList.toggle('hidden', !isReader);
    document.getElementById('nav-librarian').classList.toggle('hidden', !isLibrarian);
    document.getElementById('nav-admin').classList.toggle('hidden', !isAdmin);

    show('dashboard-view');
    hide('auth-view');
    showSection('books-section');
    loadBooks();
}

function logout() {
    clearToken();
    show('auth-view');
    hide('dashboard-view');
    document.getElementById('user-info').classList.add('hidden');
}

// ── Books ──────────────────────────────────────────────────────
let currentPage = 0;
const pageSize  = 10;

async function loadBooks(search = '', page = 0) {
    currentPage = page;
    const params = new URLSearchParams({ page, size: pageSize, sortBy: 'title', sortDir: 'asc' });
    if (search) params.set('search', search);

    const data = await apiFetch(`/books?${params}`, { headers: authHeaders() });
    const container = document.getElementById('books-list');

    const isAdmin = document.getElementById('nav-admin') &&
                    !document.getElementById('nav-admin').classList.contains('hidden');

    container.innerHTML = `
        <div class="search-bar">
            <input type="text" id="search-input" placeholder="Szukaj po tytule, autorze lub ISBN..." value="${search}">
            <button onclick="searchBooks()">Szukaj</button>
        </div>
        ${isAdmin ? `<button class="btn-add" onclick="showAddBookForm()">+ Dodaj książkę</button>` : ''}
        <div id="add-book-form" class="hidden card">
            <h3>Nowa książka</h3>
            <input type="text"   id="new-isbn"   placeholder="ISBN">
            <input type="text"   id="new-title"  placeholder="Tytuł">
            <input type="text"   id="new-author" placeholder="Autor">
            <input type="text"   id="new-publisher" placeholder="Wydawnictwo">
            <input type="number" id="new-year"   placeholder="Rok wydania">
            <input type="number" id="new-copies" placeholder="Liczba egzemplarzy">
            <button onclick="createBook()">Zapisz</button>
            <button onclick="hideAddBookForm()">Anuluj</button>
        </div>
        <table>
            <thead><tr>
                <th>Tytuł</th><th>Autor</th><th>ISBN</th>
                <th>Rok</th><th>Dostępne</th><th>Akcje</th>
            </tr></thead>
            <tbody>
            ${data.content.map(b => `
                <tr>
                    <td>${b.title}</td>
                    <td>${b.author}</td>
                    <td>${b.isbn}</td>
                    <td>${b.publicationYear}</td>
                    <td>${b.availableCopies} / ${b.totalCopies}</td>
                    <td>
                        ${b.availableCopies > 0
                            ? `<button onclick="reserveBook(${b.id})">Zarezerwuj</button>`
                            : '<span class="unavailable">Niedostępna</span>'}
                        ${isAdmin ? `
                            <button onclick="showEditBookForm(${b.id}, '${esc(b.isbn)}', '${esc(b.title)}', '${esc(b.author)}', '${esc(b.publisher)}', ${b.publicationYear}, ${b.totalCopies})">Edytuj</button>
                            <button class="btn-danger" onclick="deleteBook(${b.id})">Usuń</button>
                        ` : ''}
                    </td>
                </tr>`).join('')}
            </tbody>
        </table>
        <div class="pagination">
            ${page > 0 ? `<button onclick="loadBooks('${search}', ${page - 1})">← Poprzednia</button>` : ''}
            <span>Strona ${page + 1} z ${data.totalPages}</span>
            ${page + 1 < data.totalPages ? `<button onclick="loadBooks('${search}', ${page + 1})">Następna →</button>` : ''}
        </div>`;
}

function searchBooks() {
    const q = document.getElementById('search-input').value;
    loadBooks(q, 0);
}

function showAddBookForm()  { document.getElementById('add-book-form').classList.remove('hidden'); }
function hideAddBookForm()  { document.getElementById('add-book-form').classList.add('hidden'); }

async function createBook() {
    const body = {
        isbn:            document.getElementById('new-isbn').value,
        title:           document.getElementById('new-title').value,
        author:          document.getElementById('new-author').value,
        publisher:       document.getElementById('new-publisher').value,
        publicationYear: parseInt(document.getElementById('new-year').value),
        totalCopies:     parseInt(document.getElementById('new-copies').value)
    };
    try {
        await apiFetch('/books', { method: 'POST', headers: authHeaders(), body: JSON.stringify(body) });
        loadBooks();
    } catch (err) {
        alert('Błąd: ' + (err.message || JSON.stringify(err)));
    }
}

function showEditBookForm(id, isbn, title, author, publisher, year, copies) {
    const html = `
        <div id="edit-modal" class="modal-overlay">
            <div class="modal card">
                <h3>Edytuj książkę</h3>
                <input type="text"   id="edit-isbn"      value="${isbn}">
                <input type="text"   id="edit-title"     value="${title}">
                <input type="text"   id="edit-author"    value="${author}">
                <input type="text"   id="edit-publisher" value="${publisher}">
                <input type="number" id="edit-year"      value="${year}">
                <input type="number" id="edit-copies"    value="${copies}">
                <button onclick="updateBook(${id})">Zapisz</button>
                <button onclick="closeModal()">Anuluj</button>
            </div>
        </div>`;
    document.body.insertAdjacentHTML('beforeend', html);
}

async function updateBook(id) {
    const body = {
        isbn:            document.getElementById('edit-isbn').value,
        title:           document.getElementById('edit-title').value,
        author:          document.getElementById('edit-author').value,
        publisher:       document.getElementById('edit-publisher').value,
        publicationYear: parseInt(document.getElementById('edit-year').value),
        totalCopies:     parseInt(document.getElementById('edit-copies').value)
    };
    try {
        await apiFetch(`/books/${id}`, { method: 'PUT', headers: authHeaders(), body: JSON.stringify(body) });
        closeModal();
        loadBooks();
    } catch (err) {
        alert('Błąd: ' + (err.message || JSON.stringify(err)));
    }
}

async function deleteBook(id) {
    if (!confirm('Na pewno usunąć tę książkę?')) return;
    try {
        await apiFetch(`/books/${id}`, { method: 'DELETE', headers: authHeaders() });
        loadBooks();
    } catch (err) {
        alert('Błąd: ' + (err.message || JSON.stringify(err)));
    }
}

// ── Reservations ───────────────────────────────────────────────
async function reserveBook(bookId) {
    try {
        await apiFetch('/reservations', {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ bookId })
        });
        alert('Rezerwacja została utworzona!');
        loadBooks();
    } catch (err) {
        alert('Błąd rezerwacji: ' + (err.message || JSON.stringify(err)));
    }
}

// ── Reservations ───────────────────────────────────────────────
async function loadReservations() {
    const data = await apiFetch('/reservations/my', { headers: authHeaders() });
    const container = document.getElementById('reservations-list');

    if (!data.length) {
        container.innerHTML = '<p>Brak rezerwacji.</p>';
        return;
    }

    container.innerHTML = `
        <table>
            <thead><tr>
                <th>Książka</th><th>Status</th>
                <th>Zarezerwowano</th><th>Wygasa</th><th>Akcje</th>
            </tr></thead>
            <tbody>
            ${data.map(r => `
                <tr>
                    <td>${r.bookTitle}</td>
                    <td class="status-${r.status.toLowerCase()}">${statusLabel(r.status)}</td>
                    <td>${fmt(r.reservedAt)}</td>
                    <td>${fmt(r.expiresAt)}</td>
                    <td>${r.status === 'ACTIVE' ? `
                        <button class="btn-danger" onclick="cancelReservation(${r.id})">Anuluj</button>
                    ` : ''}</td>
                </tr>`).join('')}
            </tbody>
        </table>`;
}

async function cancelReservation(id) {
    if (!confirm('Na pewno anulować tę rezerwację?')) return;
    try {
        await apiFetch(`/reservations/${id}`, { method: 'DELETE', headers: authHeaders() });
        loadReservations();
    } catch (err) {
        alert('Błąd: ' + (err.message || JSON.stringify(err)));
    }
}

function statusLabel(s) {
    return {
        ACTIVE: 'Aktywna', FULFILLED: 'Zrealizowana', CANCELLED: 'Anulowana', EXPIRED: 'Wygasła',
        BORROWED: 'Wypożyczona', RETURNED: 'Zwrócona', OVERDUE: 'Po terminie'
    }[s] || s;
}

// ── Librarian panel ────────────────────────────────────────────
async function loadLibrarianPanel() {
    await Promise.all([loadLibrarianReservations(), loadLibrarianBorrows()]);
}

async function loadLibrarianReservations() {
    const data = await apiFetch('/reservations/active', { headers: authHeaders() });
    const container = document.getElementById('librarian-reservations-list');

    if (!data.length) {
        container.innerHTML = '<p>Brak aktywnych rezerwacji.</p>';
        return;
    }

    container.innerHTML = `
        <table>
            <thead><tr>
                <th>Czytelnik</th><th>Książka</th>
                <th>Zarezerwowano</th><th>Wygasa</th><th>Akcje</th>
            </tr></thead>
            <tbody>
            ${data.map(r => `
                <tr>
                    <td>${r.userEmail}</td>
                    <td>${r.bookTitle}</td>
                    <td>${fmt(r.reservedAt)}</td>
                    <td>${fmt(r.expiresAt)}</td>
                    <td><button onclick="issueBook(${r.id})">Wydaj książkę</button></td>
                </tr>`).join('')}
            </tbody>
        </table>`;
}

async function loadLibrarianBorrows() {
    const data = await apiFetch('/borrows/active', { headers: authHeaders() });
    const container = document.getElementById('librarian-borrows-list');

    if (!data.length) {
        container.innerHTML = '<p>Brak aktywnych wypożyczeń.</p>';
        return;
    }

    container.innerHTML = `
        <table>
            <thead><tr>
                <th>Czytelnik</th><th>Książka</th>
                <th>Wypożyczono</th><th>Termin zwrotu</th>
            </tr></thead>
            <tbody>
            ${data.map(b => `
                <tr>
                    <td>${b.userEmail}</td>
                    <td>${b.bookTitle}</td>
                    <td>${fmt(b.borrowedAt)}</td>
                    <td>${fmt(b.dueAt)}</td>
                </tr>`).join('')}
            </tbody>
        </table>`;
}

async function issueBook(reservationId) {
    if (!confirm('Wydać książkę dla tej rezerwacji?')) return;
    try {
        await apiFetch('/borrows', {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ reservationId })
        });
        alert('Książka wydana. Wypożyczenie zarejestrowane na 14 dni.');
        loadLibrarianPanel();
    } catch (err) {
        alert('Błąd: ' + (err.message || JSON.stringify(err)));
    }
}

// ── Borrow history ─────────────────────────────────────────────
async function loadHistory() {
    const data = await apiFetch('/borrows/history', { headers: authHeaders() });
    const container = document.getElementById('history-list');

    if (!data.length) {
        container.innerHTML = '<p>Brak historii wypożyczeń.</p>';
        return;
    }

    container.innerHTML = `
        <table>
            <thead><tr>
                <th>Książka</th><th>Data wypożyczenia</th>
                <th>Termin zwrotu</th><th>Status</th><th>Akcje</th>
            </tr></thead>
            <tbody>
            ${data.map(b => `
                <tr>
                    <td>${b.bookTitle}</td>
                    <td>${fmt(b.borrowedAt)}</td>
                    <td>${fmt(b.dueAt)}</td>
                    <td class="status-${b.status.toLowerCase()}">${b.status}</td>
                    <td>${b.status === 'BORROWED'
                        ? `<button onclick="returnBook(${b.id})">Zwróć</button>`
                        : ''}</td>
                </tr>`).join('')}
            </tbody>
        </table>`;
}

async function returnBook(borrowId) {
    try {
        await apiFetch(`/borrows/${borrowId}/return`, { method: 'POST', headers: authHeaders() });
        alert('Zwrot zarejestrowany!');
        loadHistory();
    } catch (err) {
        alert('Błąd zwrotu: ' + (err.message || JSON.stringify(err)));
    }
}

// ── UI helpers ─────────────────────────────────────────────────
function show(id) { document.getElementById(id).classList.remove('hidden'); }
function hide(id) { document.getElementById(id).classList.add('hidden'); }
function esc(s)   { return String(s).replace(/'/g, "\\'"); }
function fmt(dt)  { return dt ? new Date(dt).toLocaleDateString('pl-PL') : '—'; }
function closeModal() { document.getElementById('edit-modal')?.remove(); }

function showSection(id) {
    document.querySelectorAll('.content-section').forEach(s => s.classList.add('hidden'));
    document.getElementById(id).classList.remove('hidden');
    if (id === 'reservations-section') loadReservations();
    if (id === 'history-section') loadHistory();
    if (id === 'librarian-section') loadLibrarianPanel();
}

function toggleAuthForms() {
    document.getElementById('login-form').classList.toggle('hidden');
    document.getElementById('register-form').classList.toggle('hidden');
}

function showError(formId, msg) {
    const form = document.getElementById(formId);
    let err = form.querySelector('.error-msg');
    if (!err) {
        err = document.createElement('p');
        err.className = 'error-msg';
        form.appendChild(err);
    }
    err.textContent = msg;
}

// ── Init ───────────────────────────────────────────────────────
window.addEventListener('DOMContentLoaded', async () => {
    if (getToken()) {
        try { await afterLogin(); }
        catch { clearToken(); }
    }
});
