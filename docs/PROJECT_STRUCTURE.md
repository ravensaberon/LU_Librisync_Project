# Latte and Letters Project Structure

This guide maps the codebase by responsibility so the system is easier to present and review.

## Root Files

- `pom.xml` defines the Maven/Spring Boot project, Java version, dependencies, and WAR packaging.
- `README.md` explains setup, features, demo accounts, and local run steps.
- `.gitignore` keeps generated build output and local runtime files out of source control.

## Application Code

- `src/main/java/com/latteandletters/LatteAndLettersApplication.java`
  Main Spring Boot entry point.
- `src/main/java/com/latteandletters/config`
  Security, password encoding, database schema checks, demo-data initialization, and user-details loading.
- `src/main/java/com/latteandletters/controller`
  Web and API request handlers. These classes connect URLs to views, JSON endpoints, and service methods.
- `src/main/java/com/latteandletters/service`
  Business logic for authentication, books, reservations, circulation, fines, reports, notifications, and student profiles.
- `src/main/java/com/latteandletters/model`
  JPA entities and enums that represent database records and controlled values.
- `src/main/java/com/latteandletters/repository`
  Spring Data JPA interfaces for database access.
- `src/main/java/com/latteandletters/dto`
  Data-transfer objects used for forms, OTP states, dashboards, integration APIs, and service responses.
- `src/main/java/com/latteandletters/util`
  Shared helper classes for pagination, display formatting, address forms, and year-level options.

## Web UI

- `src/main/webapp/WEB-INF/jsp/auth`
  Login, registration, email verification, and forgot-password screens.
- `src/main/webapp/WEB-INF/jsp/admin`
  Admin dashboard, student management, reservations, fines, reports, references, and profile pages.
- `src/main/webapp/WEB-INF/jsp/student`
  Student dashboard, catalog, reservations, borrowing history, e-book reader, password change, and profile pages.
- `src/main/webapp/WEB-INF/jsp/books`
  Admin book-management page.
- `src/main/webapp/WEB-INF/jsp/issues`
  Admin circulation/issue-management page.

## Static Assets

- `src/main/resources/static/css/app.css`
  Main application stylesheet.
- `src/main/resources/static/js/app.js`
  Shared UI behaviors such as shell navigation, confirmation dialogs, notifications, and page interactions.
- `src/main/resources/static/js/qr-tools.js`
  Shared QR scanning utilities.
- `src/main/resources/static/vendor`
  Local third-party browser libraries used by the QR features.
- `src/main/resources/static/assets/images`
  Logo and UI image assets.

## Database And Storage

- `database/schema.sql`
  Manual database creation script.
- `database/demo-data.sql`
  Manual demo-data seed script.
- `src/main/resources/seed/demo-data.sql`
  Auto-seed data used by the app during startup.
- `storage/book-covers`
  Demo/local uploaded book-cover files.
- `storage/ebooks`, `storage/email-outbox`, and `storage/profile-pictures`
  Runtime-generated folders ignored by Git.

## Presentation Notes

- OOP entities are mainly in `model`.
- Encapsulation examples are in entity fields with getters/setters, such as `User` and `Student`.
- Abstraction examples are in repository interfaces that extend `JpaRepository`.
- Polymorphism examples are in classes implementing Spring interfaces, such as `LegacyAwarePasswordEncoder` and `CustomUserDetailsService`.
- Validation and exception handling are mainly in `service` classes and controller `try-catch` blocks.
