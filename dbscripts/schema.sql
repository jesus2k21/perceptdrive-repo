#USE `perceptdrive_data`;

CREATE TABLE user(
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(100),
    pin VARCHAR(24)
);

CREATE TABLE historical_drive_time(
    historical_drive_time_id INT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(50),
    date DATETIME NOT NULL,
    origin VARCHAR(500),
    destination varchar(500),
    travel_time_traffic INT
    #CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES user(id) ON UPDATE CASCADE
);

CREATE TABLE saved_location(
    saved_location_id INT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(50),
    nickname VARCHAR(50),
    location varchar(500)
);

CREATE TABLE schedule(
    schedule_id INT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(50),
    origin VARCHAR(500),
    destination varchar(500),
    days VARCHAR(100),
    time TIME, #MySQL retrieves and displays TIME values in 'hh:mm:ss' format
    saved_location_id INT,
    INDEX `idx_location` (saved_location_id),
    CONSTRAINT `fk_saved_location` FOREIGN KEY (saved_location_id) REFERENCES saved_location(saved_location_id) ON UPDATE CASCADE ON DELETE CASCADE
)