DELETE FROM invitation;
DELETE FROM project_member;
DELETE FROM task;
DELETE FROM project;
DELETE FROM users;

-- Passwort für alle Demo-Konten: password
INSERT INTO users (id, username, password, role, email) VALUES (1, 'admin', '$2b$12$/FtHWj0GjNXINZ/ALaWQYuZq13Saz/kbzMUmcBQPUMam4zLHdhRym', 'ADMIN', 'admin@example.com');
INSERT INTO users (id, username, password, role, email) VALUES (2, 'bob', '$2b$12$/FtHWj0GjNXINZ/ALaWQYuZq13Saz/kbzMUmcBQPUMam4zLHdhRym', 'PROJECT_LEAD', 'bob@example.com');
INSERT INTO users (id, username, password, role, email) VALUES (3, 'charlie', '$2b$12$/FtHWj0GjNXINZ/ALaWQYuZq13Saz/kbzMUmcBQPUMam4zLHdhRym', 'MEMBER', 'charlie@example.com');

INSERT INTO project (id, name, description, status, owner_id) VALUES (100, 'Server-Migration', 'Wechsel auf neue Cloud-Server', 'ACTIVE', 2);

INSERT INTO project_member (id, project_id, user_id, access_level) VALUES (1, 100, 3, 'WRITE');

INSERT INTO task (id, project_id, title, description, status) VALUES (101, 100, 'Backups erstellen', '', 'DONE');
INSERT INTO task (id, project_id, title, description, status) VALUES (102, 100, 'Datenbank exportieren', '', 'IN_PROGRESS');
INSERT INTO task (id, project_id, title, description, status) VALUES (103, 100, 'DNS umstellen', '', 'OPEN');

-- Wichtig: Nach festen IDs müssen Auto-Increment-Zähler hochgesetzt werden, sonst schlagen neue INSERTs fehl
ALTER TABLE users ALTER COLUMN id RESTART WITH 100;
ALTER TABLE project ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE project_member ALTER COLUMN id RESTART WITH 100;
ALTER TABLE task ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE invitation ALTER COLUMN id RESTART WITH 1;
