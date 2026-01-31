SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE meeting_room;

INSERT INTO user (login_id, password, name, email, role)
VALUES
    ('admin', '1234', '관리자', 'admin@meeting.local', 'ADMIN'),
    ('user', '1234', '사용자', 'user@meeting.local', 'USER');
