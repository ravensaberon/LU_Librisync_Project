CREATE DATABASE IF NOT EXISTS latte_and_letters;
USE latte_and_letters;

-- Shared authentication/accounts table.
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50) NULL,
    last_name VARCHAR(50) NOT NULL,
    suffix VARCHAR(20) NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'STUDENT') NOT NULL DEFAULT 'STUDENT',
    status ENUM('ACTIVE', 'INACTIVE', 'PENDING', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Role-specific admin profile table.
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Role-specific student profile table.
CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    student_id VARCHAR(20) NOT NULL UNIQUE,
    course VARCHAR(100) NOT NULL DEFAULT 'Not set',
    year_level VARCHAR(60) NOT NULL DEFAULT 'Not set',
    phone VARCHAR(30) UNIQUE,
    address VARCHAR(200),
    date_of_birth DATE NULL,
    qr_code_path VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Reference tables.
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS authors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL UNIQUE,
    bio TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Catalog and circulation tables.
CREATE TABLE IF NOT EXISTS books (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(180) NOT NULL,
    isbn VARCHAR(30) NOT NULL UNIQUE,
    barcode VARCHAR(60) UNIQUE,
    category_id BIGINT NULL,
    author_id BIGINT NULL,
    publication_year INT,
    quantity INT NOT NULL DEFAULT 1,
    available_quantity INT NOT NULL DEFAULT 1,
    shelf_location VARCHAR(80),
    cover_image VARCHAR(200),
    description TEXT,
    ebook_path VARCHAR(200),
    qr_code_path VARCHAR(200),
    is_digital BOOLEAN NOT NULL DEFAULT FALSE,
    is_visible_in_catalog BOOLEAN NOT NULL DEFAULT TRUE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_books_author FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS issue_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    issued_by BIGINT NOT NULL,
    qr_issue_code VARCHAR(120),
    issue_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date DATETIME NOT NULL,
    return_date DATETIME NULL,
    return_requested_at DATETIME NULL,
    status ENUM('ISSUED', 'RETURNED', 'OVERDUE') NOT NULL DEFAULT 'ISSUED',
    fine_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    remarks VARCHAR(180),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_issue_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_admin FOREIGN KEY (issued_by) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS reservations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    queue_position INT NOT NULL DEFAULT 1,
    status ENUM('PENDING', 'PENDING_APPROVAL', 'READY', 'CLAIMED', 'CANCELLED', 'DENIED') NOT NULL DEFAULT 'PENDING',
    request_type ENUM('BORROW', 'RESERVATION') NOT NULL DEFAULT 'RESERVATION',
    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NULL,
    preferred_pickup_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_res_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS fines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    issue_record_id BIGINT NOT NULL UNIQUE,
    student_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status ENUM('UNPAID', 'PAID', 'WAIVED') NOT NULL DEFAULT 'UNPAID',
    calculated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fine_issue FOREIGN KEY (issue_record_id) REFERENCES issue_records(id) ON DELETE CASCADE,
    CONSTRAINT fk_fine_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

-- Account recovery and messaging tables.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS registration_otp_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS email_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notification_type ENUM('DUE_REMINDER', 'DUE_REMINDER_3_DAYS', 'DUE_REMINDER_1_DAY', 'DUE_REMINDER_ON_DATE', 'RESERVATION_READY', 'RESERVATION_EXPIRED', 'UNPAID_FINE', 'PASSWORD_RESET') NOT NULL,
    subject VARCHAR(180) NOT NULL,
    body TEXT NOT NULL,
    scheduled_at DATETIME NULL,
    sent_at DATETIME NULL,
    status ENUM('PENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Shared in-app notifications for both admins and students.
CREATE TABLE IF NOT EXISTS user_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    link_url VARCHAR(200),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Audit and OTP workflow tables.
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_email VARCHAR(120) NULL,
    actor_name VARCHAR(120) NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80) NULL,
    summary VARCHAR(255) NOT NULL,
    details VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student_profile_otp_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    pending_name VARCHAR(100) NOT NULL,
    pending_course VARCHAR(100),
    pending_year_level VARCHAR(60),
    pending_phone VARCHAR(30),
    pending_address VARCHAR(200),
    pending_date_of_birth DATE NULL,
    otp_hash VARCHAR(64) NOT NULL,
    destination_email VARCHAR(100) NOT NULL,
    last_sent_at DATETIME NOT NULL,
    resend_available_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_otp_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

CREATE OR REPLACE VIEW vw_student_reading_history AS
SELECT
    s.student_id,
    TRIM(CONCAT_WS(' ', u.first_name, NULLIF(u.middle_name, ''), u.last_name, NULLIF(u.suffix, ''))) AS student_name,
    b.title AS book_title,
    i.issue_date,
    i.due_date,
    i.return_date,
    i.status,
    i.fine_amount
FROM issue_records i
JOIN students s ON s.id = i.student_id
JOIN users u ON u.id = s.user_id
JOIN books b ON b.id = i.book_id;
