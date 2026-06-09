const API_BASE_URL = 'http://localhost:8080/api';
const TOKEN_KEY = 'jwt_token';

// --- Utility Functions ---
function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}

function removeToken() {
    localStorage.removeItem(TOKEN_KEY);
}

function showSection(sectionId) {
    document.querySelectorAll('section').forEach(section => {
        section.classList.add('hidden');
    });
    document.getElementById(sectionId).classList.remove('hidden');
}

function showDashboardSections(role) {
    document.getElementById('nav-history').classList.add('hidden');
    document.getElementById('nav-admin').classList.add('hidden');

    if (role === 'READER') {
        document.getElementById('nav-history').classList.remove('hidden');
        document.getElementById('history-section').classList.add('hidden'); // Hide history by default
    } else if (role === 'ADMIN') {
        document.getElementById('nav-admin').classList.remove('hidden');
        document.getElementById('admin-section').classList.add('hidden'); // Hide admin by default
    }

    // Always show books section by default for logged in users
    document.getElementById('books-section').classList.remove('hidden');
    showSection('dashboard-view');
}

async function makeApiRequest(url, method = 'GET', data = null, requiresAuth = true) {
    const headers = {
        'Content-Type': 'application/json',
    };

    if (requiresAuth) {
        const token = getToken();
        if (!token) {
            console.error("Brak tokenu autoryzacji.");
            logout();
            throw new Error("Brak tokenu autoryzacji.");
        }
        headers['Authorization'] = `Bearer ${token}`;
    }

    const options = {
        method,
        headers,
    };

    if (data) {
        options.body = JSON.stringify(data);
    }

    try {
        const response = await fetch(url, options);
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || `Błąd HTTP: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error(`Błąd podczas ${method} ${url}:`, error);
        alert(error.message);
        throw error;
    }
}

// --- Auth Functions ---
async function register() {
    const firstName = document.getElementById('reg-firstname').value;
    const lastName = document.getElementById('reg-lastname').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;

    try {
        const response = await makeApiRequest(`${API_BASE_URL}/auth/register`, 'POST', { firstName, lastName, email, password }, false);
        alert("Rejestracja zakończona sukcesem! Możesz się zalogować.");
        toggleAuthForms(); // Przełącz na formularz logowania
    } catch (error) {
        // Błąd obsługiwany przez makeApiRequest
    }
}

async function login() {
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    try {
        const data = await makeApiRequest(`${API_BASE_URL}/auth/login`, 'POST', { email, password }, false);
        setToken(data.token);
        await initializeApp();
    } catch (error) {
        // Błąd obsługiwany przez makeApiRequest
    }
}

function logout() {
    removeToken();
    document.getElementById('user-info').classList.add('hidden');
    showSection('auth-view');
}

function toggleAuthForms() {
    document.getElementById('login-form').classList.toggle('hidden');
    document.getElementById('register-form').classList.toggle('hidden');
}

// --- Initialization ---
async function initializeApp() {
    const token = getToken();
    if (token) {
        try {
            const user = await makeApiRequest(`${API_BASE_URL}/auth/me`);
            document.getElementById('user-name').textContent = `${user.firstName} ${user.lastName}`;
            document.getElementById('user-role').textContent = user.role;
            document.getElementById('user-info').classList.remove('hidden');
            showDashboardSections(user.role);
        } catch (error) {
            // Jeśli token jest niepoprawny lub wygasł, wyloguj
            console.error("Błąd autoryzacji przy inicjalizacji, wylogowuję:", error);
            logout();
        }
    } else {
        showSection('auth-view');
    }
}

// Uruchomienie aplikacji po załadowaniu DOM
document.addEventListener('DOMContentLoaded', initializeApp);


// --- Stubs for further implementation ---
// Implementacja logiki dla katalogu książek
async function fetchBooks() {
    console.log("Pobieram książki...");
    // const search = document.getElementById('book-search').value;
    // const books = await makeApiRequest(`${API_BASE_URL}/books?search=${search}`);
    // renderBooks(books);
}

// Implementacja funkcji rezerwacji dla czytelnika
async function reserveBook(bookId) {
    console.log(`Rezerwuję książkę o ID: ${bookId}`);
    // await makeApiRequest(`${API_BASE_URL}/reservations`, 'POST', { bookId });
    // alert('Książka zarezerwowana!');
    // fetchBooks(); // Odśwież listę książek po rezerwacji
}

// Implementacja logiki dla historii wypożyczeń czytelnika
async function fetchBorrowHistory() {
    console.log("Pobieram historię wypożyczeń...");
    // const history = await makeApiRequest(`${API_BASE_URL}/borrows/history`);
    // renderBorrowHistory(history);
}

// Implementacja logiki dla panelu admina (dodawanie książek)
async function addBook() {
    console.log("Dodaję książkę...");
    // const isbn = document.getElementById('new-isbn').value;
    // const title = document.getElementById('new-title').value;
    // const author = document.getElementById('new-author').value;
    // const publisher = document.getElementById('new-publisher').value;
    // const publicationYear = parseInt(document.getElementById('new-year').value);
    // const totalCopies = parseInt(document.getElementById('new-copies').value);
    //
    // await makeApiRequest(`${API_BASE_URL}/books`, 'POST', {
    //     isbn, title, author, publisher, publicationYear, totalCopies
    // }, true, 'ADMIN');
    // alert('Książka dodana pomyślnie!');
    // // Clear form and refresh books
    // fetchBooks();
}
