-- =========================================
-- meeting_room schema.sql (fresh init)
-- =========================================

-- DB 생성
CREATE DATABASE IF NOT EXISTS meeting_room
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE meeting_room;

-- =========================================
-- TABLE: user
-- =========================================
-- 사용자 테이블
CREATE TABLE IF NOT EXISTS user (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    login_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,

    email VARCHAR(100) NULL,
    profile_image VARCHAR(255) NULL,

    -- ✅ 추가: 관리자 메모(회원 관련 메모)
    memo TEXT NULL,

    role ENUM('ADMIN', 'USER') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_email (email)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLE: room
-- =========================================
CREATE TABLE IF NOT EXISTS room (
                                    id INT AUTO_INCREMENT PRIMARY KEY,

                                    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLE: reservation
-- =========================================
CREATE TABLE IF NOT EXISTS reservation (
                                           id INT AUTO_INCREMENT PRIMARY KEY,

                                           user_id INT NOT NULL,
                                           room_id INT NOT NULL,

                                           start_time DATETIME NOT NULL,
                                           end_time DATETIME NOT NULL,

                                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP,

                                           CONSTRAINT fk_reservation_user
                                           FOREIGN KEY (user_id) REFERENCES user(id),

    CONSTRAINT fk_reservation_room
    FOREIGN KEY (room_id) REFERENCES room(id),

    -- 시간 검증(가능한 MySQL 8에서만)
    -- start_time < end_time 보장
    CONSTRAINT chk_reservation_time CHECK (start_time < end_time)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;

-- 조회/충돌 방지용 인덱스(예약 기능 구현할 때 필수급)
CREATE INDEX idx_reservation_room_time ON reservation (room_id, start_time, end_time);
CREATE INDEX idx_reservation_user_time ON reservation (user_id, start_time, end_time);
