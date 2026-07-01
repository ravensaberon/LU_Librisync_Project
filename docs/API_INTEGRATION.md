# Latte and Letters Library Module API

This module belongs to Group 1 in the integrated Library, Attendance, and Cafe system.
All integration payloads use JSON. Other groups must call these endpoints instead of reading the library database directly.

## Base URL

Local default:

```text
http://localhost:8080
```

When testing on a local network, replace `localhost` with the host machine IP address.

## Exposed Endpoints

### GET /api/library/module-info

Returns module status and the public API contract.

Sample response:

```json
{
  "module": "library",
  "system": "Latte and Letters",
  "version": "1.0.0",
  "status": "ready",
  "serverTime": "2026-07-01T12:00:00",
  "endpoints": {
    "books": "GET /api/library/books?availableOnly=true&keyword=java"
  }
}
```

### GET /api/library/books

Returns catalog books for Attendance or Cafe screens that need live library data.

Query parameters:

```text
keyword       optional string
availableOnly optional boolean, default false
```

Sample response:

```json
[
  {
    "id": 1,
    "title": "Clean Code",
    "isbn": "9780132350884",
    "barcode": "LL-A1B2C3D4",
    "category": "Programming",
    "author": "Robert C. Martin",
    "publicationYear": 2008,
    "quantity": 3,
    "availableQuantity": 2,
    "shelfLocation": "CS-01",
    "digital": false,
    "visibleInCatalog": true
  }
]
```

### GET /api/library/books/{bookId}

Returns one book by library book ID.

Error response:

```json
{
  "error": "Book not found.",
  "code": 404,
  "path": "/api/library/books/999",
  "timestamp": "2026-07-01T12:00:00"
}
```

### GET /api/library/students/{studentId}/borrower-eligibility

Returns whether a student can borrow books. This is the best endpoint for Attendance or Cafe to check student standing.

Sample response:

```json
{
  "studentId": "2026-0001",
  "name": "Juan Dela Cruz",
  "email": "juan@example.com",
  "course": "BSIT",
  "yearLevel": "2nd Year",
  "accountStatus": "ACTIVE",
  "eligibleToBorrow": true,
  "activeLoanCount": 1,
  "unpaidFineCount": 0,
  "unpaidFineTotal": 0,
  "reasons": []
}
```

### GET /api/library/summary

Returns counts for dashboards and end-to-end reporting.

Sample response:

```json
{
  "totalBooks": 120,
  "availableBooks": 93,
  "catalogVisibleBooks": 115,
  "archivedBooks": 5,
  "activeStudents": 45,
  "inactiveStudents": 2,
  "activeLoans": 16,
  "overdueLoans": 3,
  "unpaidFines": 4
}
```

## Consumed Endpoints

Configure the other module URLs through environment variables before running the app:

```powershell
$env:LATTE_AND_LETTERS_ATTENDANCE_BASE_URL='http://localhost:8081'
$env:LATTE_AND_LETTERS_CAFE_BASE_URL='http://localhost:8082'
```

The library module can then consume:

```text
GET /api/integrations/attendance/students/{studentId}/status
GET /api/integrations/cafe/students/{studentId}/profile
GET /api/integrations/health
```

Expected Attendance endpoint:

```text
GET {ATTENDANCE_BASE_URL}/api/attendance/students/{studentId}/status
```

Expected Cafe endpoint:

```text
GET {CAFE_BASE_URL}/api/cafe/students/{studentId}/profile
```

If a base URL is not configured or the other module is offline, the response stays graceful:

```json
{
  "module": "attendance",
  "url": null,
  "configured": false,
  "reachable": false,
  "data": null,
  "error": "Base URL is not configured. Set the matching latteandletters.integration.* property."
}
```

## Suggested End-to-End Demo

1. Attendance module scans or selects a student.
2. Attendance calls `GET /api/library/students/{studentId}/borrower-eligibility`.
3. Library returns whether the student is clear to borrow.
4. Cafe calls `GET /api/library/summary` to show live library activity on a shared dashboard.
5. Library calls `GET /api/integrations/attendance/students/{studentId}/status` to show it can consume another group's live API.
