# قاعدة بيانات وثائق وسجلات المجلس الأعلى للأزهر

A web database of the Supreme Council of Al-Azhar's files and registers (1917–1980) kept at the Al-Azhar Library. It is built from the specification in `تصور قاعدة  البيانات بعد التعديل.docx`.

**Stack:** Java 17, Spring Boot 3.4, Spring Data JPA, PostgreSQL 16, Flyway, Apache POI (Excel export), and a plain HTML/CSS/JS frontend served by Spring Boot.

## Run

```bash
docker compose up -d          # PostgreSQL on localhost:5433 (db/user/password: azhar_archive / azhar / azhar)
mvn spring-boot:run           # http://localhost:8080
```

Sign in with **Admin** / **Admin 123** (the account named in the specification; change it after deploying).

To use a different database, set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`. Flyway builds the whole schema and loads all the data on first start.

`mvn test` runs unit tests and an integration test. The integration test applies every migration to a fresh PostgreSQL in Testcontainers, so Docker must be running.

## Deploy for free (Render + Neon)

1. **Database:** at [neon.tech](https://neon.tech), create a free project. From its connection details, take the host, database, user and password. Build the JDBC URL as `jdbc:postgresql://<host>/<database>?sslmode=require`.
2. **Code:** push this repository to GitHub (a private repo is fine).
3. **App:** at [render.com](https://render.com), choose **New → Blueprint** and select the repo. Render reads `render.yaml`. Fill in `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and an `ADMIN_PASSWORD` of your choice.
4. When the first deploy finishes, Flyway creates and fills the database. Sign in as **Admin** with your `ADMIN_PASSWORD`.

On the free plan the service sleeps after 15 idle minutes. The first visit after that takes about a minute to wake it.

## Pages (`src/main/resources/static`)

| Page | Contents |
|---|---|
| `login.html` | Login. Shows the title, the thesis cover image, and the library photo as background |
| `index.html` | Interface page. Tabs: الرئيسية (home: stats + the 10 main subjects with counts), مقدمة, الهدف من إنشاء قاعدة البيانات, دليل استخدام قاعدة البيانات |
| `search.html` | Search options. Combo boxes for main, sub and detailed subject. Keyword search (all / title / subject / year) with the lens button. Year range. File and register counts. Full inventory tables with paging, or the subject's own tables (files first, then registers). Excel export, printing, and a record viewer (zoom in/out, previous/next, end view; view only) |

## REST API (all endpoints except login need a session)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/login`, `/api/auth/logout` · GET `/api/auth/me` | Session authentication |
| GET | `/api/stats` | Totals |
| GET | `/api/subjects` | Subject tree with distinct file and register counts |
| GET | `/api/subjects/{id}/entries?q&field&yearFrom&yearTo` | Tables for the subject and its sub-subjects |
| GET | `/api/files?q&field&yearFrom&yearTo&page&size` · `/api/files/{id}` | Files inventory |
| GET | `/api/registers?…` · `/api/registers/{id}` | Registers inventory |
| GET | `/api/export/inventory?…` · `/api/export/subjects/{id}?…` | Excel report of the current results |

`field` is one of `all`, `title`, `subject`, `year`. Search ignores Arabic spelling variants (أ/إ/آ/ا, ة/ه, ى/ي, ؤ/ئ, tatweel) through the SQL function `normalize_ar`.

## Database (Flyway, `src/main/resources/db/migration`)

| Migration | Contents |
|---|---|
| `V1__create_schema.sql` | Tables `app_user`, `subject` (3-level tree), `archive_file`, `archive_register`, `subject_entry`. Also foreign keys, check constraints, indexes, and `normalize_ar()` |
| `V2__seed_subjects.sql` | 10 main subjects and 34 sub/detailed subjects |
| `V3__seed_archive_files.sql` | 237 rows of the files inventory (قائمة حصر الملفات) |
| `V4__seed_archive_registers.sql` | 60 rows of the registers inventory (قائمة حصر السجلات) |
| `V5__seed_subject_entries.sql` | 567 rows from the subject tables, each linked to its inventory file or register |
| `V6__seed_admin_user.sql` | The Admin account (BCrypt hash) |

Migrations V2–V5 are generated from the Word document by `tools/docx-import`:

```bash
unzip -o "تصور قاعدة  البيانات بعد التعديل.docx" -d /tmp/docx
python3 tools/docx-import/parse.py /tmp/docx/word/document.xml /tmp/doc.json
python3 tools/docx-import/gen_sql.py /tmp/doc.json src/main/resources/db/migration
```

## Assumptions

- **Subject numbering follows the subject tables, not the summary list.** Where the two disagree, the detailed tables in the document are used. Examples: امتحانات has تفتيش as 2/3, and 7/3 is طلاب وافدون.
- **Subject-table rows keep the document's wording** (title, date, subject content) and link to the inventory row for full details. The link is matched by item number first, then reference code. This handles reference codes that were copied wrongly in the document. All 567 rows are linked.
- **Dates are stored as written** (e.g. `1944/1945`, `5/  7/1931`). Separate `year_from`/`year_to` columns hold the years extracted from them, and these drive the date filters.
- **The "interaction keys inside the record" are a record viewer, not a scan viewer.** The document has no digitised images, so the viewer shows the catalogue record. There is nothing to download.
- **The document's placeholder link (`htttt…`)** is replaced by the site's actual address.
- **The database is read-only for researchers.** The specification asks for no data-entry screens.
