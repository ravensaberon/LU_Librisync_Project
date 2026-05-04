# LU Librisync

LU Librisync is a `Spring Boot + JSP + MySQL` Library Management System starter for school or campus libraries. It is designed for a stack using:

- `JSP` for server-rendered UI
- `Spring Boot` for MVC, Security, and data access
- `MySQL Workbench` for database management and local schema setup

## Implemented Foundation

This repository now includes a working project foundation for:

- Login using Spring Security
- Student self-registration with generated student ID
- Admin dashboard with circulation counts and simple analytics
- Expanded admin dashboard with circulation, fine, borrower block, and audit snapshots
- Category and author management
- Full book management
- Book issue and return flow with circulation policy checks
- Student search by student ID
- Student dashboard, profile update, and borrowing history
- Advanced catalog search by title or barcode keyword, category, author, availability, and ISBN
- Fine calculation for overdue records
- Fine ledger management with paid and waived states
- Reservation queue system with ready-to-claim workflow
- Due-date email reminder queue with SMTP-ready delivery and file outbox fallback
- Actual PDF upload, e-book access, and inline reader page
- Digital-ready book fields such as barcode, QR issue code, and e-book path
- Borrower eligibility rules based on overdue items, unpaid fines, account status, and active loan limits
- Admin reports center with CSV exports for circulation, overdue, fines, reservations, and audit logs
- Audit logging for key admin and system-side actions

## Current Scope

Implemented now:

- Core admin and student web flow
- Database-first JPA mapping based on the existing schema
- Legacy-friendly login that still accepts the plain-text demo passwords from `database/sample-data.sql`
- Reservation management for students and admins
- Automatic reminder scheduling for due dates and ready reservations
- Storage-backed digital library module for PDF resources
- Admin fine settlement workflow with payment and waiver actions
- Admin reporting and export module
- Borrower standing visibility on admin and student pages
- Database-backed audit trail for operational actions

Planned next:

- Optional payment receipts and partial fine payment flow
- Copy-level inventory records and lost or damaged item handling
- Dedicated database migrations using Flyway or Liquibase

## Project Structure

- `src/main/java/com/lulibrisync/config`
  Security and authentication setup
- `src/main/java/com/lulibrisync/controller`
  MVC controllers for auth, admin, student, books, and circulation
- `src/main/java/com/lulibrisync/model`
  JPA entities and enums
- `src/main/java/com/lulibrisync/repository`
  Spring Data repositories
- `src/main/java/com/lulibrisync/service`
  Business logic such as registration, search, issue/return, and fines
- `src/main/resources/application.properties`
  Database and JSP configuration
- `src/main/resources/static/css/app.css`
  Shared UI styling
- `src/main/webapp/WEB-INF/jsp`
  JSP views
- `database/schema.sql`
  MySQL schema
- `database/sample-data.sql`
  Demo data

## Database Setup In MySQL Workbench

1. Open MySQL Workbench and connect to your local MySQL server.
2. Run `database/schema.sql`.
3. Run `database/sample-data.sql`.
4. Confirm that the database name is `lu_librisync`.

## Local Requirements

- Java 17
- Maven 3.9+
- MySQL 8

## Local Run

Recommended:

1. Run the local startup script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-lu-librisync.ps1
```

2. Enter your MySQL password when prompted.

3. Open:

```text
http://localhost:8080
```

If the connected database is missing the documented demo records, the app now auto-seeds the demo admin and sample student data on startup. Set `LU_LIBRISYNC_DEMO_DATA_ENABLED=false` if you want to disable that behavior.

Manual alternative:

1. Set your database environment variables in PowerShell:

```powershell
$env:LU_LIBRISYNC_DB_URL="jdbc:mysql://127.0.0.1:3306/lu_librisync?useSSL=false&serverTimezone=Asia/Manila&allowPublicKeyRetrieval=true"
$env:LU_LIBRISYNC_DB_USERNAME="root"
$env:LU_LIBRISYNC_DB_PASSWORD="your_mysql_password"
```

2. Start the app:

```powershell
.\tools\apache-maven-3.9.14\bin\mvn.cmd -Dmaven.repo.local=.\.m2\repository spring-boot:run
```

3. Open:

```text
http://localhost:8080
```

## Demo Accounts

- Admin: `admin@lulibrisync.edu` / `Admin1234`
- Student: `maria.santos@student.edu` / `Student1234`
- Student: `john.cruz@student.edu` / `Student1234`

## Notes

- New registrations are stored using BCrypt-ready password hashing.
- Existing sample users can still log in because the app accepts legacy plain-text seed passwords for migration compatibility.
- A local Maven runtime is included under `tools/apache-maven-3.9.14` for this workspace setup.
- PDF uploads are stored under `storage/ebooks` by default, or under `LU_LIBRISYNC_STORAGE_ROOT` if that environment variable is set.
- Email reminders and profile OTP messages are configured to use `lulibrisync@gmail.com` as the default sender identity.
- For real Gmail delivery, set `LU_LIBRISYNC_SMTP_PASSWORD` to the Gmail App Password for `lulibrisync@gmail.com`. You can optionally override `LU_LIBRISYNC_SMTP_HOST`, `LU_LIBRISYNC_SMTP_PORT`, `LU_LIBRISYNC_SMTP_USERNAME`, `LU_LIBRISYNC_SMTP_FROM`, and `LU_LIBRISYNC_SMTP_SSL` as needed.
- If SMTP is not configured, reminder content is still generated and written to `storage/email-outbox` for local review.
- Student profile OTPs are persisted in the database with a 3-minute resend cooldown, so they remain active even after logout/login until they expire or are used.
- Forgot password now uses persistent OTP records in `password_reset_tokens`, with the same 3-minute resend countdown and database-backed recovery flow.
- Demo admin, student accounts, and starter catalog data are auto-seeded at startup when `admin@lulibrisync.edu` is missing from the connected database. Disable this by setting `LU_LIBRISYNC_DEMO_DATA_ENABLED=false`.
- Re-run `database/schema.sql` after pulling the latest changes so the `audit_logs` table, `student_profile_otp_requests` table, and contact-number uniqueness rules are available in MySQL.
