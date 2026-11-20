INSERT INTO users (first_name, last_name, email, password_hash, role_id, is_active, created_at, updated_at)
SELECT 'Anna', 'Bibliotekarz', 'bibliotekarz@ksiegarnia.pl',
       '$2b$10$n.JCoZzVzg2CpcG6psewi.07UXIEjQ6Rtqnl.WKcCCzT37vju4lb.',
       r.id, true, NOW(), NOW()
FROM roles r WHERE r.name = 'LIBRARIAN';

INSERT INTO users (first_name, last_name, email, password_hash, role_id, is_active, created_at, updated_at)
SELECT 'Admin', 'Systemu', 'admin@ksiegarnia.pl',
       '$2b$10$OI/STJrsuOE7H/cPgGp1xuBmrkf1y7PG8VSeunQNFBUdyWmqAxfWO',
       r.id, true, NOW(), NOW()
FROM roles r WHERE r.name = 'ADMIN';
