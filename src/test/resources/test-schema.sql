CREATE TABLE IF NOT EXISTS user_info (
    user_id VARCHAR(10) NOT NULL,
    nick_name VARCHAR(20),
    email VARCHAR(150),
    password VARCHAR(50),
    qq_open_id VARCHAR(35),
    qq_avatar VARCHAR(150),
    join_time DATETIME,
    last_login_time DATETIME,
    status TINYINT,
    use_space BIGINT DEFAULT 0,
    total_space BIGINT,
    PRIMARY KEY (user_id)
);

CREATE TABLE IF NOT EXISTS email_code (
    email VARCHAR(150) NOT NULL,
    code VARCHAR(5) NOT NULL,
    create_time DATETIME,
    status TINYINT,
    PRIMARY KEY (email, code)
);

CREATE TABLE IF NOT EXISTS file_info (
    file_id VARCHAR(10) NOT NULL,
    user_id VARCHAR(10) NOT NULL,
    file_md5 VARCHAR(32),
    file_pid VARCHAR(10),
    file_size BIGINT,
    file_name VARCHAR(200),
    file_cover VARCHAR(100),
    file_path VARCHAR(100),
    create_time DATETIME,
    last_update_time DATETIME,
    folder_type TINYINT,
    file_category TINYINT,
    file_type TINYINT,
    status TINYINT,
    recovery_time DATETIME,
    del_flag TINYINT DEFAULT 2,
    PRIMARY KEY (file_id, user_id)
);

CREATE TABLE IF NOT EXISTS file_share (
    share_id VARCHAR(20) NOT NULL,
    file_id VARCHAR(10) NOT NULL,
    user_id VARCHAR(10) NOT NULL,
    valid_type TINYINT,
    expire_time DATETIME,
    share_time DATETIME,
    code VARCHAR(5),
    show_count INT DEFAULT 0,
    PRIMARY KEY (share_id)
);
