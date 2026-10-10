START TRANSACTION;
INSERT INTO users (id, username, role) VALUES
    (1, 'admin', 'ADMIN'),
    (2, 'reader1', 'READER'),
    (3, 'reader2', 'READER');
COMMIT;