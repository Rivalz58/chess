CREATE DATABASE IF NOT EXISTS chess_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chess_db;

CREATE TABLE players (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(50)  UNIQUE NOT NULL,
    email        VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    elo          INT NOT NULL DEFAULT 1200,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE games (
    id               INT PRIMARY KEY AUTO_INCREMENT,
    white_player_id  INT NOT NULL,
    black_player_id  INT NULL,
    status           ENUM('waiting','active','finished','abandoned') NOT NULL DEFAULT 'waiting',
    result           ENUM('white','black','draw') NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at      TIMESTAMP NULL,
    FOREIGN KEY (white_player_id) REFERENCES players(id),
    FOREIGN KEY (black_player_id) REFERENCES players(id)
);

-- Each row is one half-move (ply). move_number starts at 1.
-- from_row/from_col/to_row/to_col match the Android int[8][8] board indices.
-- promotion stores the piece value (5=queen, 4=rook, etc.) or NULL.
CREATE TABLE game_moves (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    game_id     INT NOT NULL,
    move_number INT NOT NULL,
    from_row    TINYINT NOT NULL,
    from_col    TINYINT NOT NULL,
    to_row      TINYINT NOT NULL,
    to_col      TINYINT NOT NULL,
    promotion   TINYINT NULL,
    played_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    UNIQUE KEY unique_ply (game_id, move_number)
);
