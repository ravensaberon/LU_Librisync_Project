package com.lulibrisync.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);

    private final DataSource dataSource;

    public DatabaseSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            ensureUserNameColumns(statement);
            ensureStudentReadingHistoryView(statement);
            ensurePreferredPickupDateColumn(statement);
            ensureStudentRegistrationOtpTable(statement);
            ensureStudentPasswordChangeOtpTable(statement);
            ensureReservationRequestTypeColumn(statement);
            ensureIssueReturnRequestColumn(statement);
            ensureAdminNotificationsTable(statement);
            ensureReservationStatusEnumValues(statement);
            ensureEmailNotificationTypeEnumValues(statement);
            ensureRegistrationOtpTokensTable(statement);
            ensureUserStatusPendingValue(statement);
            ensureMustChangePasswordColumn(statement);
            ensureBookArchiveColumns(statement);
        }
    }

    private void ensureUserNameColumns(Statement statement) throws Exception {
        try (ResultSet tables = statement.executeQuery("SHOW TABLES LIKE 'users'")) {
            if (!tables.next()) {
                return;
            }
        }

        if (!hasColumn(statement, "users", "first_name")) {
            statement.executeUpdate("ALTER TABLE users ADD COLUMN first_name VARCHAR(50) NULL AFTER id");
        }
        if (!hasColumn(statement, "users", "middle_name")) {
            statement.executeUpdate("ALTER TABLE users ADD COLUMN middle_name VARCHAR(50) NULL AFTER first_name");
        }
        if (!hasColumn(statement, "users", "last_name")) {
            statement.executeUpdate("ALTER TABLE users ADD COLUMN last_name VARCHAR(50) NULL AFTER middle_name");
        }
        if (!hasColumn(statement, "users", "suffix")) {
            statement.executeUpdate("ALTER TABLE users ADD COLUMN suffix VARCHAR(20) NULL AFTER last_name");
        }

        boolean hasLegacyNameColumn = hasColumn(statement, "users", "name");
        if (hasLegacyNameColumn) {
            statement.executeUpdate(
                    "UPDATE users "
                            + "SET first_name = COALESCE(NULLIF(TRIM(first_name), ''), TRIM(SUBSTRING_INDEX(TRIM(name), ' ', 1))) "
                            + "WHERE name IS NOT NULL AND TRIM(name) <> ''"
            );
            statement.executeUpdate(
                    "UPDATE users "
                            + "SET last_name = COALESCE(NULLIF(TRIM(last_name), ''), "
                            + "TRIM(CASE WHEN INSTR(TRIM(name), ' ') > 0 "
                            + "THEN SUBSTRING(TRIM(name), INSTR(TRIM(name), ' ') + 1) ELSE '' END)) "
                            + "WHERE name IS NOT NULL AND TRIM(name) <> ''"
            );
        }

        statement.executeUpdate("UPDATE users SET first_name = 'Library' WHERE first_name IS NULL OR TRIM(first_name) = ''");
        statement.executeUpdate("UPDATE users SET last_name = 'User' WHERE last_name IS NULL OR TRIM(last_name) = ''");
        statement.executeUpdate("ALTER TABLE users MODIFY COLUMN first_name VARCHAR(50) NOT NULL");
        statement.executeUpdate("ALTER TABLE users MODIFY COLUMN middle_name VARCHAR(50) NULL");
        statement.executeUpdate("ALTER TABLE users MODIFY COLUMN last_name VARCHAR(50) NOT NULL");
        statement.executeUpdate("ALTER TABLE users MODIFY COLUMN suffix VARCHAR(20) NULL");

        if (hasLegacyNameColumn) {
            statement.executeUpdate("ALTER TABLE users DROP COLUMN name");
            logger.info("Migrated users.name into first_name/middle_name/last_name/suffix and removed the legacy column.");
        }
    }

    private void ensurePreferredPickupDateColumn(Statement statement) throws Exception {
        try (ResultSet tables = statement.executeQuery("SHOW TABLES LIKE 'reservations'")) {
            if (!tables.next()) {
                return;
            }
        }

        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM reservations LIKE 'preferred_pickup_date'")) {
            if (columns.next()) {
                return;
            }
        }

        statement.executeUpdate("ALTER TABLE reservations ADD COLUMN preferred_pickup_date DATE NULL AFTER expires_at");
        logger.info("Added reservations.preferred_pickup_date column for scheduled pickup support.");
    }

    private void ensureStudentRegistrationOtpTable(Statement statement) throws Exception {
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS student_registration_otp_requests ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                        + "pending_first_name VARCHAR(50) NOT NULL, "
                        + "pending_middle_name VARCHAR(50), "
                        + "pending_last_name VARCHAR(50) NOT NULL, "
                        + "pending_full_name VARCHAR(100) NOT NULL, "
                        + "pending_program VARCHAR(120) NOT NULL, "
                        + "pending_year_level VARCHAR(60) NOT NULL, "
                        + "pending_email VARCHAR(120) NOT NULL, "
                        + "pending_contact_number VARCHAR(30) NOT NULL, "
                        + "pending_birth_date DATE NOT NULL, "
                        + "pending_province VARCHAR(120) NOT NULL, "
                        + "pending_city_municipality VARCHAR(120) NOT NULL, "
                        + "pending_barangay VARCHAR(120) NOT NULL, "
                        + "pending_street VARCHAR(180) NOT NULL, "
                        + "pending_zipcode VARCHAR(4) NOT NULL, "
                        + "pending_address VARCHAR(255) NOT NULL, "
                        + "pending_password_hash VARCHAR(255) NOT NULL, "
                        + "otp_hash VARCHAR(128) NOT NULL, "
                        + "destination_email VARCHAR(120) NOT NULL, "
                        + "last_sent_at DATETIME NOT NULL, "
                        + "resend_available_at DATETIME NOT NULL, "
                        + "expires_at DATETIME NOT NULL, "
                        + "used BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "verified_at DATETIME NULL, "
                        + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                        + ")"
        );
    }

    private void ensureStudentPasswordChangeOtpTable(Statement statement) throws Exception {
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS student_password_change_otp_requests ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                        + "student_id BIGINT NOT NULL, "
                        + "pending_password_hash VARCHAR(255) NOT NULL, "
                        + "otp_hash VARCHAR(128) NOT NULL, "
                        + "destination_email VARCHAR(120) NOT NULL, "
                        + "last_sent_at DATETIME NOT NULL, "
                        + "resend_available_at DATETIME NOT NULL, "
                        + "expires_at DATETIME NOT NULL, "
                        + "used BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "verified_at DATETIME NULL, "
                        + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                        + "CONSTRAINT fk_student_password_otp_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE"
                        + ")"
        );
    }

    private void ensureReservationRequestTypeColumn(Statement statement) throws Exception {
        try (ResultSet tables = statement.executeQuery("SHOW TABLES LIKE 'reservations'")) {
            if (!tables.next()) {
                return;
            }
        }

        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM reservations LIKE 'request_type'")) {
            if (!columns.next()) {
                statement.executeUpdate("ALTER TABLE reservations ADD COLUMN request_type VARCHAR(20) NOT NULL DEFAULT 'RESERVATION' AFTER status");
                logger.info("Added reservations.request_type column for borrow-vs-reservation flows.");
            }
        }

        statement.executeUpdate("UPDATE reservations SET request_type = 'RESERVATION' WHERE request_type IS NULL OR request_type = ''");
    }

    private void ensureIssueReturnRequestColumn(Statement statement) throws Exception {
        try (ResultSet tables = statement.executeQuery("SHOW TABLES LIKE 'issue_records'")) {
            if (!tables.next()) {
                return;
            }
        }

        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM issue_records LIKE 'return_requested_at'")) {
            if (columns.next()) {
                return;
            }
        }

        statement.executeUpdate("ALTER TABLE issue_records ADD COLUMN return_requested_at DATETIME NULL AFTER return_date");
        logger.info("Added issue_records.return_requested_at column for desk-confirmed returns.");
    }

    private void ensureAdminNotificationsTable(Statement statement) throws Exception {
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS admin_notifications ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                        + "admin_user_id BIGINT NOT NULL, "
                        + "notification_type VARCHAR(30) NOT NULL, "
                        + "title VARCHAR(180) NOT NULL, "
                        + "message TEXT NOT NULL, "
                        + "link_url VARCHAR(255), "
                        + "is_read BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "read_at DATETIME NULL, "
                        + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "CONSTRAINT fk_admin_notifications_user FOREIGN KEY (admin_user_id) REFERENCES users(id) ON DELETE CASCADE"
                        + ")"
        );
        logger.info("Ensured admin_notifications table exists for in-app admin alerts.");
    }

    private void ensureReservationStatusEnumValues(Statement statement) throws Exception {
        try (ResultSet tables = statement.executeQuery("SHOW TABLES LIKE 'reservations'")) {
            if (!tables.next()) {
                return;
            }
        }

        // Check if PENDING_APPROVAL is already in the enum
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM reservations LIKE 'status'")) {
            if (columns.next()) {
                String columnType = columns.getString("Type");
                if (columnType != null && columnType.contains("PENDING_APPROVAL")) {
                    return; // Already migrated
                }
            }
        }

        statement.executeUpdate(
                "ALTER TABLE reservations MODIFY COLUMN status "
                        + "ENUM('PENDING','PENDING_APPROVAL','READY','CLAIMED','CANCELLED','DENIED') "
                        + "NOT NULL DEFAULT 'PENDING'"
        );
        logger.info("Updated reservations.status enum to include PENDING_APPROVAL and DENIED values.");
    }

    private void ensureRegistrationOtpTokensTable(Statement statement) throws Exception {
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS registration_otp_tokens ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                        + "user_id BIGINT NOT NULL, "
                        + "token VARCHAR(120) NOT NULL UNIQUE, "
                        + "expires_at DATETIME NOT NULL, "
                        + "used BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "CONSTRAINT fk_reg_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                        + ")"
        );
        logger.info("Ensured registration_otp_tokens table exists for email verification.");
    }

    private void ensureStudentReadingHistoryView(Statement statement) throws Exception {
        statement.executeUpdate(
                "CREATE OR REPLACE VIEW vw_student_reading_history AS "
                        + "SELECT s.student_id, "
                        + "TRIM(CONCAT_WS(' ', u.first_name, NULLIF(u.middle_name, ''), u.last_name, NULLIF(u.suffix, ''))) AS student_name, "
                        + "b.title AS book_title, "
                        + "i.issue_date, "
                        + "i.due_date, "
                        + "i.return_date, "
                        + "i.status, "
                        + "i.fine_amount "
                        + "FROM issue_records i "
                        + "JOIN students s ON s.id = i.student_id "
                        + "JOIN users u ON u.id = s.user_id "
                        + "JOIN books b ON b.id = i.book_id"
        );
    }

    private void ensureEmailNotificationTypeEnumValues(Statement statement) throws Exception {
        try (ResultSet tables = statement.executeQuery("SHOW TABLES LIKE 'email_notifications'")) {
            if (!tables.next()) {
                return;
            }
        }

        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM email_notifications LIKE 'notification_type'")) {
            if (columns.next()) {
                String columnType = columns.getString("Type");
                if (columnType != null && columnType.contains("UNPAID_FINE")) {
                    return;
                }
            }
        }

        statement.executeUpdate(
                "ALTER TABLE email_notifications MODIFY COLUMN notification_type "
                        + "ENUM('DUE_REMINDER','DUE_REMINDER_3_DAYS','DUE_REMINDER_1_DAY','DUE_REMINDER_ON_DATE','RESERVATION_READY','RESERVATION_EXPIRED','UNPAID_FINE','PASSWORD_RESET') "
                        + "NOT NULL"
        );
        logger.info("Updated email_notifications.notification_type enum to include reservation and fine notification values.");
    }

    private void ensureUserStatusPendingValue(Statement statement) throws Exception {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM users LIKE 'status'")) {
            if (columns.next()) {
                String columnType = columns.getString("Type");
                if (columnType != null && columnType.contains("PENDING") && columnType.contains("ARCHIVED")) {
                    return;
                }
            }
        }
        statement.executeUpdate(
                "ALTER TABLE users MODIFY COLUMN status ENUM('ACTIVE','INACTIVE','PENDING','ARCHIVED') NOT NULL DEFAULT 'ACTIVE'"
        );
        logger.info("Updated users.status enum to include PENDING and ARCHIVED values.");
    }

    private void ensureMustChangePasswordColumn(Statement statement) throws Exception {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM users LIKE 'must_change_password'")) {
            if (columns.next()) {
                return;
            }
        }
        statement.executeUpdate("ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE AFTER status");
        logger.info("Added users.must_change_password column for first-login password updates.");
    }

    private void ensureBookArchiveColumns(Statement statement) throws Exception {
        try (ResultSet tables = statement.executeQuery("SHOW TABLES LIKE 'books'")) {
            if (!tables.next()) {
                return;
            }
        }

        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM books LIKE 'is_visible_in_catalog'")) {
            if (!columns.next()) {
                statement.executeUpdate("ALTER TABLE books ADD COLUMN is_visible_in_catalog BOOLEAN NOT NULL DEFAULT TRUE AFTER is_digital");
                logger.info("Added books.is_visible_in_catalog column for student catalog visibility.");
            }
        }

        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM books LIKE 'is_archived'")) {
            if (!columns.next()) {
                statement.executeUpdate("ALTER TABLE books ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT FALSE AFTER is_visible_in_catalog");
                logger.info("Added books.is_archived column for soft-delete recovery.");
            }
        }

        statement.executeUpdate("UPDATE books SET is_visible_in_catalog = FALSE WHERE is_archived = TRUE");
    }

    private boolean hasColumn(Statement statement, String tableName, String columnName) throws Exception {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM " + tableName + " LIKE '" + columnName + "'")) {
            return columns.next();
        }
    }
}
