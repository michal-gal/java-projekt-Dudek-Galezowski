# java-projekt-Dudek-Galezowski
# Projekt: REST API Księgarni

Kompletne REST API do zarządzania zasobami biblioteki. Aplikacja umożliwia rejestrację użytkowników, przeglądanie katalogu książek, wypożyczanie, a także tworzenie rezerwacji. 
System posiada wbudowane mechanizmy bezpieczeństwa oparte na tokenach JWT oraz kontrolę dostępu bazującą na rolach.

## Realizacja wymogów formalnych projektu
Projekt został zrealizowany zgodnie z filarami programowania obiektowego oraz zasadami SOLID.

* * Polimorfizm: Zaimplementowany poprzez stworzenie własnej hierarchii wyjątków biznesowych (m.in. `ResourceNotFoundException`) dziedziczących po `RuntimeException`.
* Wzorce projektowe: Builder (budowanie tokenów JWT w konfiguracji Spring Security), Repository (wzorzec dostępu do danych w Spring Data JPA), DTO (Data Transfer Object dla modeli zapytania), Dependency Injection (wstrzykiwanie przez konstruktory), oraz Filter/Chain of Responsibility (wykorzystane w JwtAuthenticationFilter przy filtrowaniu zapytań HTTP).
* RBAC (Role-Based Access Control): Pełny podział ról w systemie (ADMIN / READER) z zabezpieczeniem konkretnych endpointów.
* Testowanie: Zaimplementowano testy integracyjne przy użyciu narzędzi JUnit 5 oraz MockMvc. Do analizy pokrycia kodu skonfigurowano w środowisku Maven narzędzie JaCoCo.

## Technologie i narzędzia
* **Język:** Java 21
* **Framework:** Spring Boot 4.0.6 (Spring WebMVC, Spring Data JPA, Spring Security, Validation)
* **Baza danych i migracje:** PostgreSQL + Flyway
* **Autoryzacja (JWT):** Java JWT w wersji 0.12.6
* **Dokumentacja API:** SpringDoc OpenAPI 3 (Swagger UI) w wersji 3.0.3
* **Redukcja kodu:** Lombok
* **Testowanie i pokrycie kodu:** Spring Boot Starter Test (JUnit 5, Mockito) + plugin JaCoCo w wersji 0.8.13
* **Infrastruktura i budowanie:** Docker, Maven

## Instrukcja uruchomienia

Projekt posiada dwa kontenery, bazy danych 'Postgres' oraz aplikacji 'spring_backend' do uruchamiania w kontenerach Docker.

### Uruchomienie

docker compose up postgres -d (potrzebny aby testy podczas budowania przeszły)
./mvnw clean package
docker compose up -d

Aplikacja będzie nasłuchiwać pod adresem: http://localhost:8080.

### Endpointy i Swagger

Interaktywna dokumentacja Swagger UI dostępna jest pod adresem:
http://localhost:8080/swagger-ui/index.html

#### Zrzuty ekranu
[Swagger](Readme_Resources/endpoint1.png)
[Swagger](Readme_Resources/endpoint2.png)

## ERD bazy danych
Poniższy diagram przedstawia dokładną strukturę tabel w bazie PostgreSQL.

```mermaid
classDiagram
direction BT

class books {
   varchar(20) isbn
   varchar(255) title
   varchar(255) author
   varchar(255) publisher
   integer publication_year
   integer total_copies
   integer available_copies
   timestamp created_at
   timestamp updated_at
   bigint id
}

class borrow_history {
   bigint user_id
   bigint book_id
   bigint reservation_id
   varchar(20) status
   timestamp borrowed_at
   timestamp due_at
   timestamp returned_at
   timestamp created_at
   timestamp updated_at
   bigint id
}

class flyway_schema_history {
   varchar(50) version
   varchar(200) description
   varchar(20) type
   varchar(1000) script
   integer checksum
   varchar(100) installed_by
   timestamp installed_on
   integer execution_time
   boolean success
   integer installed_rank
}

class reservations {
   bigint user_id
   bigint book_id
   varchar(20) status
   timestamp reserved_at
   timestamp expires_at
   timestamp fulfilled_at
   timestamp cancelled_at
   timestamp created_at
   timestamp updated_at
   bigint id
}

class roles {
   varchar(50) name
   varchar(255) description
   timestamp created_at
   timestamp updated_at
   bigint id
}

class users {
   bigint role_id
   varchar(100) first_name
   varchar(100) last_name
   varchar(255) email
   varchar(255) password_hash
   boolean is_active
   timestamp created_at
   timestamp updated_at
   bigint id
}

borrow_history  -->  books : book_id:id
borrow_history  -->  reservations : reservation_id:id
borrow_history  -->  users : user_id:id
reservations  -->  books : book_id:id
reservations  -->  users : user_id:id
users  -->  roles : role_id:id