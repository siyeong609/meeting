-- =========================================
-- meeting_room schema.sql (fresh init)
-- =========================================
-- 목적:
-- - user(회원) + room(회의실) + reservation(예약)
-- - 회의실 운영 설정(요일별 운영시간/특정일 예외)까지 포함
-- - slot(예약 단위)은 기본 60분이지만, 추후 변경 가능성을 위해 DB에서 60 고정 CHECK는 걸지 않음
-- - buffer(버퍼)는 드롭다운 고정값(0/10/30/60)이라 DB에서도 CHECK로 제한

-- DB 생성
CREATE DATABASE IF NOT EXISTS meeting_room
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE meeting_room;

-- =========================================
-- TABLE: user
-- =========================================
-- 사용자 테이블
CREATE TABLE IF NOT EXISTS `user` (
                                      id INT AUTO_INCREMENT PRIMARY KEY,
                                      login_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,

    email VARCHAR(100) NULL,
    profile_image VARCHAR(255) NULL,

    -- ✅ 관리자 메모(회원 관련 메모)
    memo TEXT NULL,

    role ENUM('ADMIN', 'USER') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- ✅ NULL은 중복 허용(유니크 인덱스에서 NULL은 여러 개 허용됨)
    UNIQUE KEY uk_user_email (email)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLE: room
-- =========================================
-- 회의실 마스터 + 예약 정책
CREATE TABLE IF NOT EXISTS `room` (
                                      id INT AUTO_INCREMENT PRIMARY KEY,

                                      name VARCHAR(100) NOT NULL,
    location VARCHAR(100) NULL,
    capacity INT NOT NULL,

    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=사용,0=비활성',

    -- ✅ 운영 기간(선택): NULL이면 제한 없음
    available_start_date DATE NULL,
    available_end_date DATE NULL,

    -- ✅ 예약 정책(추후 변경 가능성 고려)
    slot_minutes INT NOT NULL DEFAULT 60 COMMENT '예약 단위(분): 기본 60(1시간)',
    min_minutes INT NOT NULL DEFAULT 60 COMMENT '최소 예약 시간(분)',
    max_minutes INT NOT NULL DEFAULT 240 COMMENT '최대 예약 시간(분)',

    -- ✅ 버퍼: 드롭다운 고정(0/10/30/60) → DB에서 제한
    buffer_minutes INT NOT NULL DEFAULT 0 COMMENT '예약 간 버퍼(분): 0/10/30/60',
    booking_open_days_ahead INT NOT NULL DEFAULT 30 COMMENT '며칠 앞까지 예약 가능',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_room_name (name),

    -- ✅ 최소 제약(너무 딥하게 고정하지 않음)
    CONSTRAINT ck_room_capacity CHECK (capacity >= 1),
    CONSTRAINT ck_room_buffer_minutes CHECK (buffer_minutes IN (0, 10, 30, 60)),
    CONSTRAINT ck_room_booking_open_days CHECK (booking_open_days_ahead >= 1),
    CONSTRAINT ck_room_available_range CHECK (
                                                 available_start_date IS NULL OR available_end_date IS NULL OR available_start_date <= available_end_date
                                             ),
    CONSTRAINT ck_room_minmax CHECK (
                                        min_minutes >= 1 AND max_minutes >= min_minutes
                                    )
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLE: room_operating_hours
-- =========================================
-- 요일별 운영시간(주간 반복 규칙)
-- dow: 1=월, 2=화 ... 7=일 (Java DayOfWeek(1~7)과 매핑 쉬움)
-- is_closed=1이면 휴무(시간은 NULL)
CREATE TABLE IF NOT EXISTS `room_operating_hours` (
                                                      id INT AUTO_INCREMENT PRIMARY KEY,

                                                      room_id INT NOT NULL,
                                                      dow TINYINT NOT NULL COMMENT '요일(1=월..7=일)',
                                                      is_closed TINYINT(1) NOT NULL DEFAULT 0,

    open_time TIME NULL,
    close_time TIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_room_dow (room_id, dow),
    KEY idx_room_hours_room (room_id),

    CONSTRAINT fk_room_hours_room
    FOREIGN KEY (room_id) REFERENCES `room`(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

    CONSTRAINT ck_room_hours_dow CHECK (dow BETWEEN 1 AND 7),

    -- ✅ 휴무면 시간 NULL / 운영이면 open<close
    CONSTRAINT ck_room_hours_closed_rule CHECK (
(is_closed = 1 AND open_time IS NULL AND close_time IS NULL)
    OR
(is_closed = 0 AND open_time IS NOT NULL AND close_time IS NOT NULL AND open_time < close_time)
    )
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLE: room_operating_exceptions
-- =========================================
-- 특정 날짜 예외(휴무/단축/특별 운영)
CREATE TABLE IF NOT EXISTS `room_operating_exceptions` (
                                                           id INT AUTO_INCREMENT PRIMARY KEY,

                                                           room_id INT NOT NULL,
                                                           exception_date DATE NOT NULL,
                                                           is_closed TINYINT(1) NOT NULL DEFAULT 0,

    open_time TIME NULL,
    close_time TIME NULL,

    reason VARCHAR(200) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_room_exception_date (room_id, exception_date),
    KEY idx_room_exception (room_id, exception_date),

    CONSTRAINT fk_room_ex_room
    FOREIGN KEY (room_id) REFERENCES `room`(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

    CONSTRAINT ck_room_ex_closed_rule CHECK (
(is_closed = 1 AND open_time IS NULL AND close_time IS NULL)
    OR
(is_closed = 0 AND open_time IS NOT NULL AND close_time IS NOT NULL AND open_time < close_time)
    )
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLE: reservation
-- =========================================
-- 예약 테이블(기능 구현 전이라도 스키마는 미리 확정)
CREATE TABLE IF NOT EXISTS `reservation` (
                                             id INT AUTO_INCREMENT PRIMARY KEY,

                                             user_id INT NOT NULL,
                                             room_id INT NOT NULL,

                                             title VARCHAR(200) NULL COMMENT '예약 제목/목적(선택)',
    status ENUM('BOOKED', 'CANCELED') NOT NULL DEFAULT 'BOOKED' COMMENT '예약 상태',

    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservation_user
    FOREIGN KEY (user_id) REFERENCES `user`(id)
    ON UPDATE CASCADE,

    CONSTRAINT fk_reservation_room
    FOREIGN KEY (room_id) REFERENCES `room`(id)
    ON UPDATE CASCADE,

    -- ✅ 기본 시간 검증
    CONSTRAINT chk_reservation_time CHECK (start_time < end_time)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- ✅ 조회/충돌 체크용 인덱스(예약 구현할 때 필수급)
CREATE INDEX idx_reservation_room_time ON `reservation` (room_id, start_time, end_time);
CREATE INDEX idx_reservation_user_time ON `reservation` (user_id, start_time, end_time);
CREATE INDEX idx_reservation_status_time ON `reservation` (status, start_time);
