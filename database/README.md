# Database Scripts

Use these files when setting up the MySQL database manually:

1. `schema.sql`
   Creates the `latte_and_letters` database, tables, foreign keys, and `vw_student_reading_history`.
2. `demo-data.sql`
   Inserts the sample admin account, student accounts, reference data, and presentation-ready demo records.

Notes:

- `users` now works as the shared account table for authentication.
- `admins` and `students` store role-specific profile data separately.
- `user_notifications` is for both admin and student in-app notifications.
- Unused legacy OTP tables were removed from the schema to reduce redundancy.

The application also has a runtime seed file at `src/main/resources/seed/demo-data.sql`. That version is loaded by `DemoDataInitializer` when auto-seeding is enabled.
