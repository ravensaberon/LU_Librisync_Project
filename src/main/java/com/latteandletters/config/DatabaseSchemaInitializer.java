package com.latteandletters.config;

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
            ensureAdminProfilesTable(statement);
            ensureStudentReadingHistoryView(statement);
            ensurePreferredPickupDateColumn(statement);
            dropUnusedLegacyOtpTables(statement);
            ensureReservationRequestTypeColumn(statement);
            ensureIssueReturnRequestColumn(statement);
            ensureUserNotificationsTable(statement);
            ensureReservationStatusEnumValues(statement);
            ensureEmailNotificationTypeEnumValues(statement);
            ensureRegistrationOtpTokensTable(statement);
            ensureUserStatusPendingValue(statement);
            ensureMustChangePasswordColumn(statement);
            ensureBookArchiveColumns(statement);
            ensureProtectedForeignKeys(statement);
            tightenColumnSizes(statement);
        }
    }

    private void ensureUserNameColumns(Statement statement) throws Exception {
        if (!tableExists(statement, "users")) {
            return;
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

        if (hasColumn(statement, "users", "student_id") && tableExists(statement, "students")) {
            statement.executeUpdate(
                    "UPDATE students s "
                            + "JOIN users u ON u.id = s.user_id "
                            + "SET s.student_id = COALESCE(NULLIF(TRIM(s.student_id), ''), u.student_id) "
                            + "WHERE u.student_id IS NOT NULL AND TRIM(u.student_id) <> ''"
            );
            statement.executeUpdate("ALTER TABLE users DROP COLUMN student_id");
            logger.info("Removed redundant users.student_id column after backfilling students.student_id.");
        }
    }

    private void ensureAdminProfilesTable(Statement statement) throws Exception {
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS admins ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                        + "user_id BIGINT NOT NULL UNIQUE, "
                        + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                        + "CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT"
                        + ")"
        );

        statement.executeUpdate(
                "INSERT INTO admins (user_id) "
                        + "SELECT u.id "
                        + "FROM users u "
                        + "LEFT JOIN admins a ON a.user_id = u.id "
                        + "WHERE u.role = 'ADMIN' AND a.id IS NULL"
        );
    }

    private void ensurePreferredPickupDateColumn(Statement statement) throws Exception {
        if (!tableExists(statement, "reservations")) {
            return;
        }

        if (hasColumn(statement, "reservations", "preferred_pickup_date")) {
            return;
        }

        statement.executeUpdate("ALTER TABLE reservations ADD COLUMN preferred_pickup_date DATE NULL AFTER expires_at");
        logger.info("Added reservations.preferred_pickup_date column for scheduled pickup support.");
    }

    private void dropUnusedLegacyOtpTables(Statement statement) throws Exception {
        if (tableExists(statement, "student_registration_otp_requests")) {
            statement.executeUpdate("DROP TABLE student_registration_otp_requests");
            logger.info("Dropped unused student_registration_otp_requests table.");
        }
        if (tableExists(statement, "student_password_change_otp_requests")) {
            statement.executeUpdate("DROP TABLE student_password_change_otp_requests");
            logger.info("Dropped unused student_password_change_otp_requests table.");
        }
    }

    private void ensureReservationRequestTypeColumn(Statement statement) throws Exception {
        if (!tableExists(statement, "reservations")) {
            return;
        }

        if (!hasColumn(statement, "reservations", "request_type")) {
            statement.executeUpdate("ALTER TABLE reservations ADD COLUMN request_type VARCHAR(20) NOT NULL DEFAULT 'RESERVATION' AFTER status");
            logger.info("Added reservations.request_type column for borrow-vs-reservation flows.");
        }

        statement.executeUpdate("UPDATE reservations SET request_type = 'RESERVATION' WHERE request_type IS NULL OR request_type = ''");
    }

    private void ensureIssueReturnRequestColumn(Statement statement) throws Exception {
        if (!tableExists(statement, "issue_records")) {
            return;
        }

        if (hasColumn(statement, "issue_records", "return_requested_at")) {
            return;
        }

        statement.executeUpdate("ALTER TABLE issue_records ADD COLUMN return_requested_at DATETIME NULL AFTER return_date");
        logger.info("Added issue_records.return_requested_at column for desk-confirmed returns.");
    }

    private void ensureUserNotificationsTable(Statement statement) throws Exception {
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS user_notifications ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                        + "user_id BIGINT NOT NULL, "
                        + "notification_type VARCHAR(30) NOT NULL, "
                        + "title VARCHAR(180) NOT NULL, "
                        + "message TEXT NOT NULL, "
                        + "link_url VARCHAR(200), "
                        + "is_read BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "read_at DATETIME NULL, "
                        + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "CONSTRAINT fk_user_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                        + ")"
        );

        if (tableExists(statement, "admin_notifications")) {
            statement.executeUpdate(
                    "INSERT INTO user_notifications (user_id, notification_type, title, message, link_url, is_read, read_at, created_at) "
                            + "SELECT legacy.admin_user_id, legacy.notification_type, legacy.title, legacy.message, legacy.link_url, legacy.is_read, legacy.read_at, legacy.created_at "
                            + "FROM admin_notifications legacy "
                            + "LEFT JOIN user_notifications current "
                            + "ON current.user_id = legacy.admin_user_id "
                            + "AND current.notification_type = legacy.notification_type "
                            + "AND current.title = legacy.title "
                            + "AND current.message = legacy.message "
                            + "AND current.created_at = legacy.created_at "
                            + "WHERE current.id IS NULL"
            );
            statement.executeUpdate("DROP TABLE admin_notifications");
            logger.info("Migrated admin_notifications into user_notifications for both admin and student in-app alerts.");
        }
    }

    private void ensureReservationStatusEnumValues(Statement statement) throws Exception {
        if (!tableExists(statement, "reservations")) {
            return;
        }

        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM reservations LIKE 'status'")) {
            if (columns.next()) {
                String columnType = columns.getString("Type");
                if (columnType != null && columnType.contains("PENDING_APPROVAL")) {
                    return;
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
                        + "token VARCHAR(64) NOT NULL UNIQUE, "
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
        if (!tableExists(statement, "email_notifications")) {
            return;
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
        if (!tableExists(statement, "users")) {
            return;
        }

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
        if (!tableExists(statement, "users")) {
            return;
        }

        if (hasColumn(statement, "users", "must_change_password")) {
            return;
        }
        statement.executeUpdate("ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE AFTER status");
        logger.info("Added users.must_change_password column for first-login password updates.");
    }

    private void ensureBookArchiveColumns(Statement statement) throws Exception {
        if (!tableExists(statement, "books")) {
            return;
        }

        if (!hasColumn(statement, "books", "is_visible_in_catalog")) {
            statement.executeUpdate("ALTER TABLE books ADD COLUMN is_visible_in_catalog BOOLEAN NOT NULL DEFAULT TRUE AFTER is_digital");
            logger.info("Added books.is_visible_in_catalog column for student catalog visibility.");
        }

        if (!hasColumn(statement, "books", "is_archived")) {
            statement.executeUpdate("ALTER TABLE books ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT FALSE AFTER is_visible_in_catalog");
            logger.info("Added books.is_archived column for soft-delete recovery.");
        }

        statement.executeUpdate("UPDATE books SET is_visible_in_catalog = FALSE WHERE is_archived = TRUE");
    }

    private void ensureProtectedForeignKeys(Statement statement) throws Exception {
        if (tableExists(statement, "students")) {
            dropForeignKeyIfExists(statement, "students", "fk_students_user");
            statement.executeUpdate(
                    "ALTER TABLE students "
                            + "ADD CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT"
            );
        }

        if (tableExists(statement, "admins")) {
            dropForeignKeyIfExists(statement, "admins", "fk_admins_user");
            statement.executeUpdate(
                    "ALTER TABLE admins "
                            + "ADD CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT"
            );
        }

        if (tableExists(statement, "issue_records")) {
            dropForeignKeyIfExists(statement, "issue_records", "fk_issue_admin");
            statement.executeUpdate(
                    "ALTER TABLE issue_records "
                            + "ADD CONSTRAINT fk_issue_admin FOREIGN KEY (issued_by) REFERENCES users(id) ON DELETE RESTRICT"
            );
        }
    }

    private void tightenColumnSizes(Statement statement) throws Exception {
        if (tableExists(statement, "users")) {
            statement.executeUpdate("ALTER TABLE users MODIFY COLUMN email VARCHAR(100) NOT NULL");
        }

        if (tableExists(statement, "students")) {
            statement.executeUpdate(
                    "ALTER TABLE students "
                            + "MODIFY COLUMN course VARCHAR(100) NOT NULL DEFAULT 'Not set', "
                            + "MODIFY COLUMN address VARCHAR(200) NULL, "
                            + "MODIFY COLUMN qr_code_path VARCHAR(200) NULL"
            );
        }

        if (tableExists(statement, "books")) {
            statement.executeUpdate(
                    "ALTER TABLE books "
                            + "MODIFY COLUMN cover_image VARCHAR(200) NULL, "
                            + "MODIFY COLUMN ebook_path VARCHAR(200) NULL, "
                            + "MODIFY COLUMN qr_code_path VARCHAR(200) NULL"
            );
        }

        if (tableExists(statement, "issue_records")) {
            statement.executeUpdate("ALTER TABLE issue_records MODIFY COLUMN remarks VARCHAR(180) NULL");
        }

        if (tableExists(statement, "password_reset_tokens")) {
            statement.executeUpdate("ALTER TABLE password_reset_tokens MODIFY COLUMN token VARCHAR(64) NOT NULL");
        }

        if (tableExists(statement, "registration_otp_tokens")) {
            statement.executeUpdate("ALTER TABLE registration_otp_tokens MODIFY COLUMN token VARCHAR(64) NOT NULL");
        }

        if (tableExists(statement, "student_profile_otp_requests")) {
            statement.executeUpdate(
                    "ALTER TABLE student_profile_otp_requests "
                            + "MODIFY COLUMN pending_course VARCHAR(100) NULL, "
                            + "MODIFY COLUMN pending_address VARCHAR(200) NULL, "
                            + "MODIFY COLUMN otp_hash VARCHAR(64) NOT NULL, "
                            + "MODIFY COLUMN destination_email VARCHAR(100) NOT NULL"
            );
        }

        if (tableExists(statement, "user_notifications")) {
            statement.executeUpdate("ALTER TABLE user_notifications MODIFY COLUMN link_url VARCHAR(200) NULL");
        }
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet tables = statement.executeQuery("SHOW TABLES LIKE '" + tableName + "'")) {
            return tables.next();
        }
    }

    private boolean hasColumn(Statement statement, String tableName, String columnName) throws Exception {
        try (ResultSet columns = statement.executeQuery("SHOW COLUMNS FROM " + tableName + " LIKE '" + columnName + "'")) {
            return columns.next();
        }
    }

    private void dropForeignKeyIfExists(Statement statement, String tableName, String constraintName) throws Exception {
        try (ResultSet constraints = statement.executeQuery(
                "SELECT 1 FROM information_schema.table_constraints "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = '" + tableName + "' "
                        + "AND constraint_name = '" + constraintName + "' "
                        + "AND constraint_type = 'FOREIGN KEY'"
        )) {
            if (!constraints.next()) {
                return;
            }
        }

        statement.executeUpdate("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
    }
}
