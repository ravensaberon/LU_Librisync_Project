USE lu_librisync;

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN ddl_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
    ) THEN
        SET @ddl_sql = ddl_sql;
        PREPARE ddl_statement FROM @ddl_sql;
        EXECUTE ddl_statement;
        DEALLOCATE PREPARE ddl_statement;
    END IF;
END$$
DELIMITER ;

ALTER TABLE users
    MODIFY COLUMN status ENUM('ACTIVE', 'INACTIVE', 'PENDING', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE';

CALL add_column_if_missing(
    'users',
    'first_name',
    'ALTER TABLE users ADD COLUMN first_name VARCHAR(50) NULL AFTER id'
);

CALL add_column_if_missing(
    'users',
    'middle_name',
    'ALTER TABLE users ADD COLUMN middle_name VARCHAR(50) NULL AFTER first_name'
);

CALL add_column_if_missing(
    'users',
    'last_name',
    'ALTER TABLE users ADD COLUMN last_name VARCHAR(50) NULL AFTER middle_name'
);

CALL add_column_if_missing(
    'users',
    'suffix',
    'ALTER TABLE users ADD COLUMN suffix VARCHAR(20) NULL AFTER last_name'
);

SET @backfill_first_name_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'name'
        ),
        'UPDATE users SET first_name = COALESCE(NULLIF(TRIM(first_name), ''''), TRIM(SUBSTRING_INDEX(TRIM(name), '' '', 1))) WHERE name IS NOT NULL AND TRIM(name) <> ''''',
        'SELECT 1'
    )
);
PREPARE backfill_first_name_statement FROM @backfill_first_name_sql;
EXECUTE backfill_first_name_statement;
DEALLOCATE PREPARE backfill_first_name_statement;

SET @backfill_last_name_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'name'
        ),
        'UPDATE users SET last_name = COALESCE(NULLIF(TRIM(last_name), ''''), TRIM(CASE WHEN INSTR(TRIM(name), '' '') > 0 THEN SUBSTRING(TRIM(name), INSTR(TRIM(name), '' '') + 1) ELSE '''' END)) WHERE name IS NOT NULL AND TRIM(name) <> ''''',
        'SELECT 1'
    )
);
PREPARE backfill_last_name_statement FROM @backfill_last_name_sql;
EXECUTE backfill_last_name_statement;
DEALLOCATE PREPARE backfill_last_name_statement;

UPDATE users
SET first_name = 'Library'
WHERE first_name IS NULL OR TRIM(first_name) = '';

UPDATE users
SET last_name = 'User'
WHERE last_name IS NULL OR TRIM(last_name) = '';

ALTER TABLE users
    MODIFY COLUMN first_name VARCHAR(50) NOT NULL,
    MODIFY COLUMN middle_name VARCHAR(50) NULL,
    MODIFY COLUMN last_name VARCHAR(50) NOT NULL,
    MODIFY COLUMN suffix VARCHAR(20) NULL;

SET @drop_legacy_name_column = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'name'
        ),
        'ALTER TABLE users DROP COLUMN name',
        'SELECT 1'
    )
);
PREPARE drop_legacy_name_column_statement FROM @drop_legacy_name_column;
EXECUTE drop_legacy_name_column_statement;
DEALLOCATE PREPARE drop_legacy_name_column_statement;

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

CALL add_column_if_missing(
    'users',
    'must_change_password',
    'ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE AFTER status'
);

CALL add_column_if_missing(
    'books',
    'is_visible_in_catalog',
    'ALTER TABLE books ADD COLUMN is_visible_in_catalog BOOLEAN NOT NULL DEFAULT TRUE AFTER is_digital'
);

CALL add_column_if_missing(
    'books',
    'is_archived',
    'ALTER TABLE books ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT FALSE AFTER is_visible_in_catalog'
);

CALL add_column_if_missing(
    'reservations',
    'request_type',
    'ALTER TABLE reservations ADD COLUMN request_type ENUM(''BORROW'', ''RESERVATION'') NOT NULL DEFAULT ''RESERVATION'' AFTER status'
);

CALL add_column_if_missing(
    'reservations',
    'preferred_pickup_date',
    'ALTER TABLE reservations ADD COLUMN preferred_pickup_date DATE NULL AFTER expires_at'
);

ALTER TABLE reservations
    MODIFY COLUMN status ENUM('PENDING', 'PENDING_APPROVAL', 'READY', 'CLAIMED', 'CANCELLED', 'DENIED') NOT NULL DEFAULT 'PENDING';

ALTER TABLE reservations
    MODIFY COLUMN request_type ENUM('BORROW', 'RESERVATION') NOT NULL DEFAULT 'RESERVATION';

CALL add_column_if_missing(
    'issue_records',
    'return_requested_at',
    'ALTER TABLE issue_records ADD COLUMN return_requested_at DATETIME NULL AFTER return_date'
);

ALTER TABLE email_notifications
    MODIFY COLUMN notification_type ENUM(
        'DUE_REMINDER',
        'DUE_REMINDER_3_DAYS',
        'DUE_REMINDER_1_DAY',
        'DUE_REMINDER_ON_DATE',
        'RESERVATION_READY',
        'RESERVATION_EXPIRED',
        'UNPAID_FINE',
        'PASSWORD_RESET'
    ) NOT NULL;

CREATE TABLE IF NOT EXISTS student_registration_otp_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pending_first_name VARCHAR(50) NOT NULL,
    pending_middle_name VARCHAR(50),
    pending_last_name VARCHAR(50) NOT NULL,
    pending_full_name VARCHAR(100) NOT NULL,
    pending_program VARCHAR(120) NOT NULL,
    pending_year_level VARCHAR(60) NOT NULL,
    pending_email VARCHAR(120) NOT NULL,
    pending_contact_number VARCHAR(30) NOT NULL,
    pending_birth_date DATE NOT NULL,
    pending_province VARCHAR(120) NOT NULL,
    pending_city_municipality VARCHAR(120) NOT NULL,
    pending_barangay VARCHAR(120) NOT NULL,
    pending_street VARCHAR(180) NOT NULL,
    pending_zipcode VARCHAR(4) NOT NULL,
    pending_address VARCHAR(255) NOT NULL,
    pending_password_hash VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(128) NOT NULL,
    destination_email VARCHAR(120) NOT NULL,
    last_sent_at DATETIME NOT NULL,
    resend_available_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student_password_change_otp_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    pending_password_hash VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(128) NOT NULL,
    destination_email VARCHAR(120) NOT NULL,
    last_sent_at DATETIME NOT NULL,
    resend_available_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_password_otp_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

UPDATE reservations
SET request_type = 'RESERVATION'
WHERE request_type IS NULL OR request_type = '';

UPDATE reservations
SET status = 'PENDING_APPROVAL'
WHERE request_type = 'BORROW'
  AND status = 'PENDING';

UPDATE reservations r
SET r.status = 'CLAIMED',
    r.expires_at = NULL
WHERE r.status IN ('PENDING', 'PENDING_APPROVAL', 'READY')
  AND EXISTS (
      SELECT 1
      FROM issue_records i
      WHERE i.book_id = r.book_id
        AND i.student_id = r.student_id
        AND i.status IN ('ISSUED', 'OVERDUE')
  );

UPDATE books b
SET b.available_quantity = GREATEST(
    0,
    b.quantity - (
        SELECT COUNT(*)
        FROM issue_records i
        WHERE i.book_id = b.id
          AND i.status IN ('ISSUED', 'OVERDUE')
    )
)
WHERE b.available_quantity < 0
   OR b.available_quantity > b.quantity;

UPDATE books
SET is_visible_in_catalog = FALSE
WHERE is_archived = TRUE;

DROP PROCEDURE IF EXISTS add_column_if_missing;
