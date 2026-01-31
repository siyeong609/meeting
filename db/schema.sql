CREATE DATABASE IF NOT EXISTS meeting_room
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE meeting_room;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS user (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    login_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role ENUM('ADMIN', 'USER') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

-- 회의실 테이블
CREATE TABLE IF NOT EXISTS room (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

-- 예약 테이블
CREATE TABLE IF NOT EXISTS reservation (
                                           id INT AUTO_INCREMENT PRIMARY KEY,
                                           user_id INT NOT NULL,
                                           room_id INT NOT NULL,
                                           start_time DATETIME NOT NULL,
                                           end_time DATETIME NOT NULL,
                                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                           CONSTRAINT fk_reservation_user
                                           FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT fk_reservation_room
    FOREIGN KEY (room_id) REFERENCES room(id)
    );
