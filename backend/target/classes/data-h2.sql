-- Referenz / manueller Import — wird im Standardlauf nicht mehr ausgeführt.
-- Demo-Daten bei leerer DB: H2DevDataSeeder (Profil h2).
-- Passwort aller Demo-Konten: password
MERGE INTO users KEY(id) VALUES (1, 'admin', '$2b$12$/FtHWj0GjNXINZ/ALaWQYuZq13Saz/kbzMUmcBQPUMam4zLHdhRym', 'ADMIN', 'admin@example.com');
MERGE INTO users KEY(id) VALUES (2, 'bob', '$2b$12$/FtHWj0GjNXINZ/ALaWQYuZq13Saz/kbzMUmcBQPUMam4zLHdhRym', 'PROJECT_LEAD', 'bob@example.com');
MERGE INTO users KEY(id) VALUES (3, 'charlie', '$2b$12$/FtHWj0GjNXINZ/ALaWQYuZq13Saz/kbzMUmcBQPUMam4zLHdhRym', 'MEMBER', 'charlie@example.com');
MERGE INTO project KEY(id) VALUES (100, 'Server-Migration', 'Wechsel auf neue Cloud-Server', 'ACTIVE', 2);
MERGE INTO project_member KEY(id) VALUES (1, 100, 3, 'WRITE');
MERGE INTO task KEY(id) VALUES (101, 100, 'Backups erstellen', '', 'DONE');
MERGE INTO task KEY(id) VALUES (102, 100, 'Datenbank exportieren', '', 'IN_PROGRESS');
MERGE INTO task KEY(id) VALUES (103, 100, 'DNS umstellen', '', 'OPEN');
ALTER TABLE users ALTER COLUMN id RESTART WITH 100;
ALTER TABLE project ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE project_member ALTER COLUMN id RESTART WITH 100;
ALTER TABLE task ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE invitation ALTER COLUMN id RESTART WITH 1;
