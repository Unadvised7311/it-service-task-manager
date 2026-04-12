-- Optional: nur wenn spring.sql.init.mode=always (z. B. Erststart)
DELETE FROM invitation;
DELETE FROM project_member;
DELETE FROM task;
DELETE FROM project;
DELETE FROM users;

INSERT INTO users (id, username, password, role, email) VALUES (1, 'admin', '$2b$12$/FtHWj0GjNXINZ/ALaWQYuZq13Saz/kbzMUmcBQPUMam4zLHdhRym', 'ADMIN', 'admin@example.com');
INSERT INTO users (id, username, password, role, email) VALUES (2, 'bob', '$2b$12$/FtHWj0GjNXINZ/ALaWQYuZq13Saz/kbzMUmcBQPUMam4zLHdhRym', 'PROJECT_LEAD', 'bob@example.com');
INSERT INTO users (id, username, password, role, email) VALUES (3, 'charlie', '$2b$12$/FtHWj0GjNXINZ/ALaWQYuZq13Saz/kbzMUmcBQPUMam4zLHdhRym', 'MEMBER', 'charlie@example.com');

INSERT INTO project (id, name, description, status, owner_id) VALUES (100, 'Server-Migration', 'Wechsel auf neue Cloud-Server', 'ACTIVE', 2);
INSERT INTO project_member (id, project_id, user_id, access_level) VALUES (1, 100, 3, 'WRITE');
INSERT INTO task (id, project_id, title, description, status) VALUES (101, 100, 'Backups erstellen', '', 'DONE');
INSERT INTO task (id, project_id, title, description, status) VALUES (102, 100, 'Datenbank exportieren', '', 'IN_PROGRESS');
INSERT INTO task (id, project_id, title, description, status) VALUES (103, 100, 'DNS umstellen', '', 'OPEN');

SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval(pg_get_serial_sequence('project', 'id'), COALESCE((SELECT MAX(id) FROM project), 1));
SELECT setval(pg_get_serial_sequence('project_member', 'id'), COALESCE((SELECT MAX(id) FROM project_member), 1));
SELECT setval(pg_get_serial_sequence('task', 'id'), COALESCE((SELECT MAX(id) FROM task), 1));
SELECT setval(pg_get_serial_sequence('invitation', 'id'), COALESCE((SELECT MAX(id) FROM invitation), 1));
