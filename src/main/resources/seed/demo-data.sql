INSERT INTO users (name, email, password, role, student_id, status)
SELECT 'LU Admin', 'admin@lulibrisync.edu', 'Admin1234', 'ADMIN', NULL, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'admin@lulibrisync.edu'
);

INSERT INTO users (name, email, password, role, student_id, status)
SELECT 'Maria Santos', 'maria.santos@student.edu', 'Student1234', 'STUDENT', '241-0001', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'maria.santos@student.edu'
);

INSERT INTO users (name, email, password, role, student_id, status)
SELECT 'John Cruz', 'john.cruz@student.edu', 'Student1234', 'STUDENT', '231-0002', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'john.cruz@student.edu'
);

INSERT INTO users (name, email, password, role, student_id, status)
SELECT 'Angela Reyes', 'angela.reyes@student.edu', 'Student1234', 'STUDENT', '221-0018', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'angela.reyes@student.edu'
);

INSERT INTO students (user_id, student_id, course, year_level, phone, address, date_of_birth)
SELECT u.id, '241-0001', 'BS Information Technology', '3rd Year', '09171234567', 'San Fernando, La Union', '2003-09-01'
FROM users u
WHERE u.email = 'maria.santos@student.edu'
  AND NOT EXISTS (
      SELECT 1
      FROM students s
      WHERE s.user_id = u.id
         OR s.student_id = '241-0001'
  );

INSERT INTO students (user_id, student_id, course, year_level, phone, address, date_of_birth)
SELECT u.id, '231-0002', 'BS Education', '2nd Year', '09179876543', 'Agoo, La Union', '2004-02-14'
FROM users u
WHERE u.email = 'john.cruz@student.edu'
  AND NOT EXISTS (
      SELECT 1
      FROM students s
      WHERE s.user_id = u.id
         OR s.student_id = '231-0002'
  );

INSERT INTO students (user_id, student_id, course, year_level, phone, address, date_of_birth)
SELECT u.id, '221-0018', 'BS Business Administration', '4th Year', '09175551234', 'Bauang, La Union', '2002-11-23'
FROM users u
WHERE u.email = 'angela.reyes@student.edu'
  AND NOT EXISTS (
      SELECT 1
      FROM students s
      WHERE s.user_id = u.id
         OR s.student_id = '221-0018'
  );

INSERT INTO categories (name, description)
SELECT 'Computer Science', 'Programming, algorithms, and software engineering references'
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Computer Science'
);

INSERT INTO categories (name, description)
SELECT 'Information Technology', 'Networking, databases, and systems references'
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Information Technology'
);

INSERT INTO categories (name, description)
SELECT 'Education', 'Teaching practice, learning theory, and classroom management'
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Education'
);

INSERT INTO categories (name, description)
SELECT 'Literature', 'Classic and contemporary literary works'
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Literature'
);

INSERT INTO categories (name, description)
SELECT 'Filipino Studies', 'Philippine history, language, and national literature'
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Filipino Studies'
);

INSERT INTO categories (name, description)
SELECT 'Research Methods', 'Research design, academic writing, and data gathering guides'
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Research Methods'
);

INSERT INTO categories (name, description)
SELECT 'Business and Leadership', 'Management, entrepreneurship, and leadership resources'
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE name = 'Business and Leadership'
);

INSERT INTO authors (name, bio)
SELECT 'Robert C. Martin', 'Software craftsman and author of influential programming references'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Robert C. Martin'
);

INSERT INTO authors (name, bio)
SELECT 'Andrew Hunt and David Thomas', 'Authors known for practical software development guidance'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Andrew Hunt and David Thomas'
);

INSERT INTO authors (name, bio)
SELECT 'Thomas H. Cormen', 'Computer scientist and co-author of algorithm references'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Thomas H. Cormen'
);

INSERT INTO authors (name, bio)
SELECT 'James F. Kurose and Keith W. Ross', 'Authors of networking textbooks widely used in universities'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'James F. Kurose and Keith W. Ross'
);

INSERT INTO authors (name, bio)
SELECT 'Abraham Silberschatz, Henry F. Korth, and S. Sudarshan', 'Authors of database systems references'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Abraham Silberschatz, Henry F. Korth, and S. Sudarshan'
);

INSERT INTO authors (name, bio)
SELECT 'Charles R. Severance', 'Author of beginner-friendly programming materials'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Charles R. Severance'
);

INSERT INTO authors (name, bio)
SELECT 'John Dewey', 'Philosopher and educator known for progressive education theory'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'John Dewey'
);

INSERT INTO authors (name, bio)
SELECT 'Uma Sekaran and Roger Bougie', 'Authors of business research methodology references'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Uma Sekaran and Roger Bougie'
);

INSERT INTO authors (name, bio)
SELECT 'Paulo Coelho', 'Brazilian novelist known for inspirational fiction'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Paulo Coelho'
);

INSERT INTO authors (name, bio)
SELECT 'Jane Austen', 'English novelist known for literary classics'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Jane Austen'
);

INSERT INTO authors (name, bio)
SELECT 'Jose Rizal', 'Philippine national hero and author'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Jose Rizal'
);

INSERT INTO authors (name, bio)
SELECT 'Simon Sinek', 'Author and speaker on leadership and organizational purpose'
WHERE NOT EXISTS (
    SELECT 1
    FROM authors
    WHERE name = 'Simon Sinek'
);

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Clean Code', '9780132350884', 'BC-9780132350884', c.id, a.id, 2008, 6, 6, 'A1-04', 'A practical guide to writing readable and maintainable software.', TRUE
FROM categories c
JOIN authors a ON a.name = 'Robert C. Martin'
WHERE c.name = 'Computer Science'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9780132350884'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'The Pragmatic Programmer', '9780135957059', 'BC-9780135957059', c.id, a.id, 2019, 5, 5, 'A1-07', 'A software development reference focused on practical habits and delivery.', TRUE
FROM categories c
JOIN authors a ON a.name = 'Andrew Hunt and David Thomas'
WHERE c.name = 'Computer Science'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9780135957059'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Introduction to Algorithms', '9780262033848', 'BC-9780262033848', c.id, a.id, 2009, 4, 4, 'A1-12', 'A core reference for data structures, algorithms, and complexity.', FALSE
FROM categories c
JOIN authors a ON a.name = 'Thomas H. Cormen'
WHERE c.name = 'Computer Science'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9780262033848'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Computer Networking: A Top-Down Approach', '9780136681557', 'BC-9780136681557', c.id, a.id, 2021, 4, 4, 'A2-03', 'A networking title covering internet architecture and transport systems.', FALSE
FROM categories c
JOIN authors a ON a.name = 'James F. Kurose and Keith W. Ross'
WHERE c.name = 'Information Technology'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9780136681557'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Database System Concepts', '9781260084504', 'BC-9781260084504', c.id, a.id, 2019, 4, 4, 'A2-09', 'A database systems reference for relational design and transactions.', FALSE
FROM categories c
JOIN authors a ON a.name = 'Abraham Silberschatz, Henry F. Korth, and S. Sudarshan'
WHERE c.name = 'Information Technology'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9781260084504'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Python for Everybody', '9781530051120', 'BC-9781530051120', c.id, a.id, 2016, 5, 5, 'A2-14', 'A beginner-friendly guide to Python programming and data handling.', TRUE
FROM categories c
JOIN authors a ON a.name = 'Charles R. Severance'
WHERE c.name = 'Information Technology'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9781530051120'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Democracy and Education', '9780684836317', 'BC-9780684836317', c.id, a.id, 1916, 3, 3, 'B1-02', 'A classic reference on education and democratic learning.', FALSE
FROM categories c
JOIN authors a ON a.name = 'John Dewey'
WHERE c.name = 'Education'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9780684836317'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Research Methods for Business', '9781119942252', 'BC-9781119942252', c.id, a.id, 2016, 3, 3, 'B1-08', 'A practical guide to research design, analysis, and reporting.', FALSE
FROM categories c
JOIN authors a ON a.name = 'Uma Sekaran and Roger Bougie'
WHERE c.name = 'Research Methods'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9781119942252'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'The Alchemist', '9780062315007', 'BC-9780062315007', c.id, a.id, 1993, 4, 4, 'C2-05', 'A frequently requested literary title about purpose and journey.', TRUE
FROM categories c
JOIN authors a ON a.name = 'Paulo Coelho'
WHERE c.name = 'Literature'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9780062315007'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Pride and Prejudice', '9780141439518', 'BC-9780141439518', c.id, a.id, 1813, 3, 3, 'C2-09', 'A classic novel often used in literature classes.', TRUE
FROM categories c
JOIN authors a ON a.name = 'Jane Austen'
WHERE c.name = 'Literature'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9780141439518'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Noli Me Tangere', '9789710813474', 'BC-9789710813474', c.id, a.id, 1887, 6, 6, 'D1-03', 'A foundational Philippine novel available for literature and history classes.', TRUE
FROM categories c
JOIN authors a ON a.name = 'Jose Rizal'
WHERE c.name = 'Filipino Studies'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9789710813474'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'El Filibusterismo', '9789710813481', 'BC-9789710813481', c.id, a.id, 1891, 5, 5, 'D1-05', 'A companion Philippine classic that supports Filipino studies courses.', TRUE
FROM categories c
JOIN authors a ON a.name = 'Jose Rizal'
WHERE c.name = 'Filipino Studies'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9789710813481'
  );

INSERT INTO books (title, isbn, barcode, category_id, author_id, publication_year, quantity, available_quantity, shelf_location, description, is_digital)
SELECT 'Start with Why', '9781591846444', 'BC-9781591846444', c.id, a.id, 2009, 4, 4, 'E1-02', 'A leadership and entrepreneurship title often used in management classes.', TRUE
FROM categories c
JOIN authors a ON a.name = 'Simon Sinek'
WHERE c.name = 'Business and Leadership'
  AND NOT EXISTS (
      SELECT 1
      FROM books
      WHERE isbn = '9781591846444'
  );

UPDATE issue_records i
JOIN students s ON s.id = i.student_id
SET
    i.status = 'RETURNED',
    i.return_date = COALESCE(i.return_date, NOW() - INTERVAL 2 DAY),
    i.fine_amount = 0.00,
    i.remarks = 'Seed sync cleared previous overdue state'
WHERE s.student_id IN ('241-0001', '231-0002', '221-0018')
  AND i.status = 'OVERDUE';

UPDATE fines f
JOIN students s ON s.id = f.student_id
SET
    f.amount = 0.00,
    f.status = 'WAIVED',
    f.paid_at = COALESCE(f.paid_at, NOW())
WHERE s.student_id IN ('241-0001', '231-0002', '221-0018')
  AND f.status = 'UNPAID';

INSERT INTO issue_records (book_id, student_id, issued_by, qr_issue_code, issue_date, due_date, return_date, status, fine_amount, remarks)
SELECT
    b.id,
    s.id,
    a.id,
    'QR-ISSUE-0001',
    NOW() - INTERVAL 5 DAY,
    NOW() + INTERVAL 9 DAY,
    NULL,
    'ISSUED',
    0.00,
    'Active software engineering loan'
FROM books b
JOIN students s ON s.student_id = '241-0001'
JOIN users a ON a.email = 'admin@lulibrisync.edu'
WHERE b.isbn = '9780132350884'
  AND NOT EXISTS (
      SELECT 1
      FROM issue_records
      WHERE qr_issue_code = 'QR-ISSUE-0001'
  );

INSERT INTO issue_records (book_id, student_id, issued_by, qr_issue_code, issue_date, due_date, return_date, status, fine_amount, remarks)
SELECT
    b.id,
    s.id,
    a.id,
    'QR-ISSUE-0002',
    NOW() - INTERVAL 24 DAY,
    NOW() - INTERVAL 10 DAY,
    NOW() - INTERVAL 12 DAY,
    'RETURNED',
    0.00,
    'Completed Filipino literature loan'
FROM books b
JOIN students s ON s.student_id = '231-0002'
JOIN users a ON a.email = 'admin@lulibrisync.edu'
WHERE b.isbn = '9789710813474'
  AND NOT EXISTS (
      SELECT 1
      FROM issue_records
      WHERE qr_issue_code = 'QR-ISSUE-0002'
  );

INSERT INTO issue_records (book_id, student_id, issued_by, qr_issue_code, issue_date, due_date, return_date, status, fine_amount, remarks)
SELECT
    b.id,
    s.id,
    a.id,
    'QR-ISSUE-0003',
    NOW() - INTERVAL 42 DAY,
    NOW() - INTERVAL 28 DAY,
    NOW() - INTERVAL 30 DAY,
    'RETURNED',
    0.00,
    'Returned after literature class use'
FROM books b
JOIN students s ON s.student_id = '241-0001'
JOIN users a ON a.email = 'admin@lulibrisync.edu'
WHERE b.isbn = '9780062315007'
  AND NOT EXISTS (
      SELECT 1
      FROM issue_records
      WHERE qr_issue_code = 'QR-ISSUE-0003'
  );

INSERT INTO issue_records (book_id, student_id, issued_by, qr_issue_code, issue_date, due_date, return_date, status, fine_amount, remarks)
SELECT
    b.id,
    s.id,
    a.id,
    'QR-ISSUE-0004',
    NOW() - INTERVAL 2 DAY,
    NOW() + INTERVAL 12 DAY,
    NULL,
    'ISSUED',
    0.00,
    'Leadership reading assignment'
FROM books b
JOIN students s ON s.student_id = '221-0018'
JOIN users a ON a.email = 'admin@lulibrisync.edu'
WHERE b.isbn = '9781591846444'
  AND NOT EXISTS (
      SELECT 1
      FROM issue_records
      WHERE qr_issue_code = 'QR-ISSUE-0004'
  );

INSERT INTO reservations (book_id, student_id, queue_position, status, reserved_at, expires_at)
SELECT b.id, s.id, 1, 'PENDING', NOW() - INTERVAL 4 HOUR, NOW() + INTERVAL 2 DAY
FROM books b
JOIN students s ON s.student_id = '231-0002'
WHERE b.isbn = '9780135957059'
  AND NOT EXISTS (
      SELECT 1
      FROM reservations r
      WHERE r.book_id = b.id
        AND r.student_id = s.id
        AND r.status IN ('PENDING', 'READY', 'CLAIMED')
  );

UPDATE books b
LEFT JOIN (
    SELECT ir.book_id, COUNT(*) AS active_issue_count
    FROM issue_records ir
    WHERE ir.status IN ('ISSUED', 'OVERDUE')
      AND ir.return_date IS NULL
    GROUP BY ir.book_id
) active_loans ON active_loans.book_id = b.id
SET b.available_quantity = GREATEST(0, b.quantity - COALESCE(active_loans.active_issue_count, 0));
