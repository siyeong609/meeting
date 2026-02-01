-- =========================================
-- meeting_room data.sql (seed)
-- =========================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE meeting_room;

-- =========================================
-- Seed: user
-- =========================================
INSERT INTO `user` (login_id, password, name, email, role)
VALUES
    ('admin', '1234', '관리자', 'admin@meeting.local', 'ADMIN'),
    ('user', '1234', '사용자', 'user@meeting.local', 'USER');

-- =========================================
-- Seed: room
-- =========================================
-- 정책 기본값:
-- - slot_minutes=60(기본)
-- - buffer_minutes는 드롭다운 값(0/10/30/60) 중 선택
INSERT INTO `room` (
    name, location, capacity, is_active,
    available_start_date, available_end_date,
    slot_minutes, min_minutes, max_minutes,
    buffer_minutes, booking_open_days_ahead
)
VALUES
    ('A 회의실', '3층', 6, 1, NULL, NULL, 60, 60, 240, 30, 30),
    ('B 회의실', '4층', 10, 1, NULL, NULL, 60, 60, 240, 0, 30);

-- =========================================
-- Seed: room_operating_hours
-- =========================================
-- 운영 규칙:
-- - 월~금 09:00~18:00 운영
-- - 토/일 휴무
-- 주의: fresh init 기준으로 A=1, B=2로 들어간다고 가정
INSERT INTO `room_operating_hours` (room_id, dow, is_closed, open_time, close_time)
VALUES
    -- A(1)
    (1, 1, 0, '09:00:00', '18:00:00'),
    (1, 2, 0, '09:00:00', '18:00:00'),
    (1, 3, 0, '09:00:00', '18:00:00'),
    (1, 4, 0, '09:00:00', '18:00:00'),
    (1, 5, 0, '09:00:00', '18:00:00'),
    (1, 6, 1, NULL, NULL),
    (1, 7, 1, NULL, NULL),

    -- B(2)
    (2, 1, 0, '09:00:00', '18:00:00'),
    (2, 2, 0, '09:00:00', '18:00:00'),
    (2, 3, 0, '09:00:00', '18:00:00'),
    (2, 4, 0, '09:00:00', '18:00:00'),
    (2, 5, 0, '09:00:00', '18:00:00'),
    (2, 6, 1, NULL, NULL),
    (2, 7, 1, NULL, NULL);
