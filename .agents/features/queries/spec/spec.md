# Enrollment and Course Queries

| | |
| --- | --- |
| **Status** | Draft |
| **Date** | 2026-10-16 |
| **Scope** | `examen-final` (backend only) |
| **Affects** | `IEnrollmentRepo`, `ICourseRepo`, `IEnrollmentService`, `EnrollmentServiceImpl`, `ICourseService`, `CourseServiceImpl`, `EnrollmentController`, `CourseController`, `dto` |
| **Reference** | Existing CRUD layering: controller → service → repository, `ModelMapper` in each controller. Functional style reference: `getMostSellerProduct` (step-by-step streams with intermediate variables). |

> Specification only. Part 2 is written after the implementation.

---

# Part 1 — Specification

## Problem

The API cannot answer seven operational questions:

1. Retrieve a student's enrollments from the DNI. `IEnrollmentRepo` has no lookup by `Student` attributes.
2. List courses that have never been enrolled. Unused courses are not reachable from the API.
3. Read enrollments page by page together with their student. `GET /v1/enrollments` returns the whole table and resolves `Enrollment.student` with one query per row.
4. Obtain enrollment totals per month. `IEnrollmentRepo` has no aggregation query.
5. Filter students by how many enrollments they have.
6. Cancel every enrollment of a student in a single statement.
7. Show, for each enrolled course, the students enrolled in it. No endpoint groups students by course.

## Goal

Add six JPQL queries and one functional-programming operation (streams + `Map`) that resolve the seven gaps above, each exposed through the existing controller → service → repository layering.

## Scope

Every change is an addition. No existing query, service method, endpoint or DTO is modified, renamed or deleted.

**In scope**

- Six `@Query` (JPQL) methods: five added to `IEnrollmentRepo` (Q1, Q3, Q4, Q5, Q6) and one added to `ICourseRepo` (Q2).
- One functional-programming service method (F1) written step by step with intermediate `Stream` variables, `flatMap` and `Collectors.groupingBy`, with no new repository method.
- Two projection records in `com.examen.dto`.
- One service method and one REST endpoint per query.

**Out of scope**

- Changes to `GlobalErrorHandler`. No operation in this spec raises a new exception type.

## Decisions

| # | Decision | Reason |
| --- | --- | --- |
| D1 | Q1–Q6 are JPQL. No native SQL. | Native SQL ties the query to one database and returns unmapped rows. JPQL keeps the queries portable and entity-mapped. |
| D2 | `findCoursesNeverEnrolled` uses `LEFT JOIN ... WHERE ed IS NULL`. | `NOT EXISTS` is equivalent. One form only, so the repository holds a single implementation of this query. |
| D3 | Aggregation results use `record` constructor expressions (`SELECT new ...`). | Interface projections are not used here. |
| D4 | `getMonthlyEnrollmentSummary` uses the JPA `YEAR()` and `MONTH()` functions. | Supported natively by Hibernate 6. Do not use `FUNCTION('YEAR', ...)`. |
| D5 | `findAllWithStudent` declares an explicit `countQuery` without `FETCH`. | A `JOIN FETCH` query combined with `Pageable` cannot derive its count query. The count query uses a plain `JOIN`. |
| D6 | `cancelEnrollmentsByStudent` is annotated `@Modifying(clearAutomatically = true, flushAutomatically = true)`; `@Transactional` goes on the service implementation, not on the repository. | The bulk update bypasses the persistence context; clearing it prevents stale `Enrollment` instances. The transaction boundary belongs to the business operation. |
| D7 | The bulk update filters by `e.student.idStudent`. | That path reads the `id_student` FK column and needs no join. JPQL does not allow joins in a bulk `UPDATE`. |
| D8 | Both aggregations and F1 filter `e.enabled = true`. | A disabled enrollment is a cancelled enrollment and must not be counted or listed. |
| D9 | `findByStudentDni` and F1 return data without fetch-joining. | `spring.jpa.open-in-view` is enabled (Spring Boot default; must not be overridden in `application.yaml`), so the lazy `student` and `details` associations resolve during mapping. |
| D10 | The bulk update only affects enrollments with `enabled = true`. | Already cancelled enrollments are not touched, so the returned row count reflects real changes. |
| D11 | No new exception handler is added. | No operation in this spec validates a numeric range; the unknown-student case returns `0`, not an error. |
| D12 | `findAllWithStudent` fetch-joins `student` only, never `details`. | Fetching a collection together with `Pageable` makes Hibernate paginate in memory. `details` stay lazy and load per row during DTO mapping (accepted trade-off). |
| D13 | F1 is solved in the service with streams over `repo.findAll()`, not with JPQL. | The exercise requires functional programming with a `Map<K,V>`. `groupingBy` builds the map directly; no new repository method is needed. |
| D14 | F1 follows the course teacher's step-by-step style: one named variable per stream stage (`Stream<Enrollment>` → `Stream<List<EnrollmentDetail>>` → `Stream<EnrollmentDetail>` → `Map`). | Each stage is readable and explainable on its own, as in `getMostSellerProduct`. |
| D15 | F1 reaches the student through `EnrollmentDetail.getEnrollment().getStudent()` and uses `TreeMap::new` as the map factory. | `EnrollmentDetail` holds a reference to its `Enrollment`, so no intermediate pair object is needed. `TreeMap` returns courses in alphabetical order. |
| D16 | F1 does not call `distinct()`. A student enrolled twice, with both enrollments enabled, in the same course appears twice in that course's list. | Keeps the pipeline identical in shape to the teacher's example. Accepted trade-off. |

## Data Flow

```
Controller                                      Service                                   Repository          Returns
------------------------------------------------------------------------------------------------------------------------------------------
GET   /v1/enrollments/by-student/{dni}       -> findByStudentDni(dni)                  -> IEnrollmentRepo  -> List<Enrollment> -> List<EnrollmentDTO>
GET   /v1/enrollments/monthly-summary        -> getMonthlyEnrollmentSummary()          -> IEnrollmentRepo  -> List<MonthlyEnrollmentSummaryDTO>
GET   /v1/enrollments/frequent-students      -> getStudentsWithMoreThanNEnrollments(n) -> IEnrollmentRepo  -> List<StudentEnrollmentCountDTO>
GET   /v1/enrollments/paged                  -> findAllWithStudent(pageable)           -> IEnrollmentRepo  -> Page<Enrollment> -> Page<EnrollmentDTO>
PATCH /v1/enrollments/student/{id}/cancel    -> cancelEnrollmentsByStudent(id)         -> IEnrollmentRepo  -> int (rows updated)
GET   /v1/enrollments/students-by-course     -> getStudentsByCourse()                  -> IEnrollmentRepo  -> findAll() -> Map<String, List<String>>
GET   /v1/courses/never-enrolled             -> findCoursesNeverEnrolled()             -> ICourseRepo      -> List<Course> -> List<CourseDTO>
```

Rules:

- Entities never leave the controller, except in F1, where the service already returns a `Map` of plain strings built from the entities.
- `Enrollment` and `Course` are mapped to `EnrollmentDTO` / `CourseDTO` with the `defaultMapper` `ModelMapper` bean, through the `toDTO` methods already present in each controller.
- Projection records are already DTOs and are returned without mapping.
- Aside from F1, services contain no aggregation logic. Every other service method in this spec is a delegation, plus the argument normalization the contract requires.

## Contract

### 1. Enrollments by student DNI

| | |
| --- | --- |
| **Method / Path** | `GET /v1/enrollments/by-student/{dni}` |
| **Path param** | `dni` — string, the student's DNI (8 digits). The service applies `trim()` before querying. |
| **200** | `List<EnrollmentDTO>`, ordered by `dateTime` descending. |
| **Empty result** | `200` with `[]` when the student has no enrollments or the DNI does not exist. Never `404`. |

### 2. Monthly enrollment summary

| | |
| --- | --- |
| **Method / Path** | `GET /v1/enrollments/monthly-summary` |
| **Params** | None. |
| **200** | `List<MonthlyEnrollmentSummaryDTO>`, ordered by `year` descending, then `month` descending. |
| **Empty result** | `200` with `[]` when there are no enabled enrollments. |

Response element:

```json
{ "year": 2026, "month": 10, "enrollmentsCount": 12, "coursesCount": 31 }
```

`enrollmentsCount` is the number of enabled enrollments in the month; `coursesCount` is the total number of enrollment details (courses taken) across them.

### 3. Frequent students

| | |
| --- | --- |
| **Method / Path** | `GET /v1/enrollments/frequent-students?minEnrollments={n}` |
| **Query param** | `minEnrollments` — `long`, optional, default `1`. Strict comparison: a student is returned when its enabled-enrollment count is **greater than** `minEnrollments`. |
| **Negative value** | Accepted, no error. `minEnrollments < 0` behaves like `0` and returns every student with at least one enabled enrollment. |
| **200** | `List<StudentEnrollmentCountDTO>`, ordered by `enrollmentsCount` descending. |

Response element:

```json
{ "idStudent": 4, "dni": "70123456", "fullName": "Juan Perez Lopez", "enrollmentsCount": 3 }
```

### 4. Courses never enrolled

| | |
| --- | --- |
| **Method / Path** | `GET /v1/courses/never-enrolled` |
| **Params** | None. |
| **200** | `List<CourseDTO>`, ordered by `name` ascending. Disabled courses are included. |

### 5. Paged enrollments with student

| | |
| --- | --- |
| **Method / Path** | `GET /v1/enrollments/paged?page={p}&size={s}&sort={field},{dir}` |
| **Params** | Standard `Pageable`. Defaults declared with `@PageableDefault`: `page=0`, `size=20`, `sort=dateTime,desc`. |
| **Sortable fields** | `dateTime`, `idEnrollment`, `student.lastNames`. |
| **200** | `Page<EnrollmentDTO>` with the standard Spring Data page metadata. |
| **Out-of-range page** | `200` with an empty `content` array and the real `totalElements`. |

### 6. Cancel enrollments of a student

| | |
| --- | --- |
| **Method / Path** | `PATCH /v1/enrollments/student/{idStudent}/cancel` |
| **Path param** | `idStudent` — integer. |
| **200** | Plain JSON integer: the number of updated rows. |
| **Unknown student** | `200` with `0`. The endpoint performs no student lookup, so it does not return `404`. |
| **Rows affected** | Only enrollments of that student with `enabled = true`; they are set to `enabled = false`. |

### 7. Students grouped by enrolled course

| | |
| --- | --- |
| **Method / Path** | `GET /v1/enrollments/students-by-course` |
| **Params** | None. |
| **200** | `Map<String, List<String>>`: course name → list of student full names (`name` + space + `lastNames`) enrolled in it. |
| **Ordering** | Courses in alphabetical order (`TreeMap`). Students in the order they are found. |
| **Filtering** | Only enrollments with `enabled = true`. Courses without enrollments do not appear. One entry per enabled enrollment detail: a student enrolled twice in the same course appears twice (D16). |
| **Empty result** | `200` with `{}` when there are no enabled enrollments. |

Response example:

```json
{
  "Base de Datos": ["Mito Rojas", "Code Yupanqui"],
  "Programación": ["Jaime Medina"]
}
```

## Queries

### Q1 — `IEnrollmentRepo.findByStudentDni`

Purpose: retrieve a student's enrollment history from the DNI, without knowing `idStudent`.
Technique: implicit association navigation — `e.student.dni` makes Hibernate emit the inner join by itself.

```java
@Query("SELECT e FROM Enrollment e WHERE e.student.dni = :dni ORDER BY e.dateTime DESC")
List<Enrollment> findByStudentDni(@Param("dni") String dni);
```

### Q2 — `ICourseRepo.findCoursesNeverEnrolled`

Purpose: detect unused courses — courses that have never appeared in an enrollment detail.
Technique: anti-join with `LEFT JOIN` + `IS NULL`. `EnrollmentDetail` is addressed directly as an entity in JPQL.

```java
@Query("SELECT c FROM Course c LEFT JOIN EnrollmentDetail ed ON ed.course = c WHERE ed IS NULL ORDER BY c.name ASC")
List<Course> findCoursesNeverEnrolled();
```

### Q3 — `IEnrollmentRepo.findAllWithStudent`

Purpose: paged enrollment grid showing the student, with one query instead of `1 + N`.
Technique: `JOIN FETCH` combined with `Pageable`, and the explicit `countQuery` that combination requires.

```java
@Query(value = "SELECT e FROM Enrollment e JOIN FETCH e.student s",
       countQuery = "SELECT COUNT(e) FROM Enrollment e JOIN e.student s")
Page<Enrollment> findAllWithStudent(Pageable pageable);
```

### Q4 — `IEnrollmentRepo.getMonthlyEnrollmentSummary`

Purpose: month-by-month enrollment report — number of enrollments and number of courses taken.
Technique: date functions in `SELECT` and `GROUP BY`, aggregates `COUNT(DISTINCT ...)` and `COUNT`, and a record constructor expression.

```java
@Query("""
       SELECT new com.examen.dto.MonthlyEnrollmentSummaryDTO(
              YEAR(e.dateTime), MONTH(e.dateTime), COUNT(DISTINCT e), COUNT(ed))
       FROM Enrollment e JOIN e.details ed
       WHERE e.enabled = true
       GROUP BY YEAR(e.dateTime), MONTH(e.dateTime)
       ORDER BY YEAR(e.dateTime) DESC, MONTH(e.dateTime) DESC
       """)
List<MonthlyEnrollmentSummaryDTO> getMonthlyEnrollmentSummary();
```

`COUNT(DISTINCT e)` is required because the join with `details` repeats each enrollment once per detail.

### Q5 — `IEnrollmentRepo.getStudentsWithMoreThanNEnrollments`

Purpose: recurring students with more than N enrollments.
Technique: `GROUP BY` over a joined entity plus `HAVING` — a filter applied after grouping, which `WHERE` cannot express.

```java
@Query("""
       SELECT new com.examen.dto.StudentEnrollmentCountDTO(
              st.idStudent, st.dni, CONCAT(st.name, ' ', st.lastNames), COUNT(e))
       FROM Enrollment e JOIN e.student st
       WHERE e.enabled = true
       GROUP BY st.idStudent, st.dni, st.name, st.lastNames
       HAVING COUNT(e) > :minEnrollments
       ORDER BY COUNT(e) DESC
       """)
List<StudentEnrollmentCountDTO> getStudentsWithMoreThanNEnrollments(@Param("minEnrollments") long minEnrollments);
```

Every non-aggregated value in `SELECT` is covered by `GROUP BY`. `name` and `lastNames` are grouped individually because the `CONCAT` expression is not a grouping key in PostgreSQL.

### Q6 — `IEnrollmentRepo.cancelEnrollmentsByStudent`

Purpose: cancel all active enrollments of a student in one statement, instead of loading N entities and saving them one by one.
Technique: bulk `UPDATE` with `@Modifying`, and the persistence-context flags it requires.

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("""
       UPDATE Enrollment e
          SET e.enabled = false
        WHERE e.student.idStudent = :idStudent
          AND e.enabled = true
       """)
int cancelEnrollmentsByStudent(@Param("idStudent") Integer idStudent);
```

`flushAutomatically` pushes pending changes before the update; `clearAutomatically` drops the managed `Enrollment` instances that the update leaves stale.

## Functional Operation

### F1 — `EnrollmentServiceImpl.getStudentsByCourse`

Purpose: show each enrolled course with the students enrolled in it.
Technique: functional programming written in the course teacher's step-by-step style. Each stage is a named variable: filter the enabled enrollments, extract their detail lists with `map`, flatten them with `flatMap`, and group by course name with `Collectors.groupingBy`, `TreeMap::new` as the map factory and `Collectors.mapping` to keep the student's full name.

```java
@Override
public Map<String, List<String>> getStudentsByCourse() {
    Stream<Enrollment> enrollmentStream = repo.findAll().stream()
            .filter(Enrollment::isEnabled);

    Stream<List<EnrollmentDetail>> lsStream = enrollmentStream.map(Enrollment::getDetails);

    Stream<EnrollmentDetail> streamDetail = lsStream.flatMap(Collection::stream);

    Map<String, List<String>> byCourse = streamDetail
            .collect(groupingBy(
                    d -> d.getCourse().getName(),
                    TreeMap::new,
                    mapping(d -> d.getEnrollment().getStudent().getName() + " "
                            + d.getEnrollment().getStudent().getLastNames(),
                            toList())));

    return byCourse;
}
```

Signature in `IEnrollmentService`:

```java
Map<String, List<String>> getStudentsByCourse();
```

Endpoint in `EnrollmentController` (returns the map as-is; no `toDTO` involved):

```java
@GetMapping("/students-by-course")
public ResponseEntity<Map<String, List<String>>> studentsByCourse() {
    return ResponseEntity.ok(service.getStudentsByCourse());
}
```

Required imports in the service:

```java
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
```

## Mapping

| Source | Target | Mechanism |
| --- | --- | --- |
| `Enrollment` | `EnrollmentDTO` | `EnrollmentController.toDTO` — `defaultMapper` |
| `Course` | `CourseDTO` | `CourseController.toDTO` — `defaultMapper` |
| `Page<Enrollment>` | `Page<EnrollmentDTO>` | `page.map(this::toDTO)` in `EnrollmentController`; page metadata is preserved |
| `Enrollment` (aggregated) | `MonthlyEnrollmentSummaryDTO` | JPQL `SELECT new` — no mapper |
| `Student` (aggregated) | `StudentEnrollmentCountDTO` | JPQL `SELECT new` — no mapper |
| `EnrollmentDetail` (grouped) | `Map<String, List<String>>` | Built in the service with streams — no mapper |
| `int` (rows updated) | response body | returned as-is |

No new `MapperConfig` entry is needed. `EnrollmentDTO` and `CourseDTO` already match their entities by field name.

Record definitions:

```java
public record MonthlyEnrollmentSummaryDTO(Integer year, Integer month, Long enrollmentsCount, Long coursesCount) { }

public record StudentEnrollmentCountDTO(Integer idStudent, String dni, String fullName, Long enrollmentsCount) { }
```

Constructor parameter types must match the JPQL result types exactly: `YEAR()` and `MONTH()` produce `Integer`, and `COUNT()` produces `Long`.

## Expected Files

**New**

| File | Content |
| --- | --- |
| `src/main/java/com/examen/dto/MonthlyEnrollmentSummaryDTO.java` | Record for Q4 |
| `src/main/java/com/examen/dto/StudentEnrollmentCountDTO.java` | Record for Q5 |

**Modified**

| File | Change |
| --- | --- |
| `src/main/java/com/examen/repo/IEnrollmentRepo.java` | Q1, Q3, Q4, Q5, Q6 |
| `src/main/java/com/examen/repo/ICourseRepo.java` | Q2 |
| `src/main/java/com/examen/service/IEnrollmentService.java` | `findByStudentDni`, `getMonthlyEnrollmentSummary`, `getStudentsWithMoreThanNEnrollments`, `findAllWithStudent`, `cancelEnrollmentsByStudent`, `getStudentsByCourse` |
| `src/main/java/com/examen/service/impl/EnrollmentServiceImpl.java` | Implementations; `dni.trim()`; `@Transactional` on `cancelEnrollmentsByStudent`; F1 in step-by-step stream style |
| `src/main/java/com/examen/service/ICourseService.java` | `findCoursesNeverEnrolled` |
| `src/main/java/com/examen/service/impl/CourseServiceImpl.java` | Implementation |
| `src/main/java/com/examen/controller/EnrollmentController.java` | 6 endpoints |
| `src/main/java/com/examen/controller/CourseController.java` | 1 endpoint |

## Acceptance Criteria

1. The application starts. Every `@Query` is validated at bootstrap; no query syntax exception is raised.
2. `GET /v1/enrollments/by-student/{dni}` returns only that student's enrollments, newest first, and `[]` for an unknown DNI.
3. `GET /v1/enrollments/monthly-summary` returns one row per year/month pair; `enrollmentsCount` equals the number of enabled enrollments in that month and `coursesCount` equals the number of their details. Disabled enrollments are excluded.
4. `GET /v1/enrollments/frequent-students?minEnrollments=2` returns only students with 3 or more enabled enrollments, ordered by `enrollmentsCount` descending.
5. `GET /v1/courses/never-enrolled` returns exactly the courses with no row in `enrollment_detail`. Enrolling a student in one of them removes it from the result.
6. `GET /v1/enrollments/paged?size=5` returns 5 elements with correct `totalElements` and `totalPages`. With `show-sql: true`, the page is resolved by one fetch query and one count query, and no per-row `student` query appears (lazy `details` loads per row are expected).
7. `PATCH /v1/enrollments/student/{id}/cancel` sets `enabled = false` on that student's active enrollments and returns the number of updated rows. Enrollments of other students and already cancelled ones are not touched.
8. The same endpoint returns `200` with `0` for an unknown student id.
9. Reading an enrollment right after the cancellation, inside the same transaction, returns `enabled = false` — evidence that `clearAutomatically` took effect.
10. `GET /v1/enrollments/students-by-course` returns a map whose keys are course names in alphabetical order and whose values list the students of the enabled enrollments in each course.
11. After cancelling an enrollment (Q6 or `PUT` with `enabled: false`), its student no longer appears under that course in `students-by-course`; a course left with no students disappears from the map.
12. The endpoint returns `200` with `{}` when there are no enabled enrollments.

## Prerequisites

1. Database reachable and credentials configured in `application.yaml` (or environment variables).
2. Seed data required to verify the criteria (the `data-generator` agent can create it through the REST API):
    - at least 3 students and 4 courses;
    - at least 1 course with no `enrollment_detail` row;
    - at least 1 disabled course;
    - enrollments spread over 2 or more different months;
    - at least 1 student with 3 or more enrollments and 1 student with a single enrollment;
    - at least 2 students enrolled in the same course, to verify the grouping in F1;
    - at least 1 disabled enrollment, to verify it is excluded from the aggregations and from F1.
---

# Part 2 — Implementation

**Created**

- `src/main/java/com/examen/dto/MonthlyEnrollmentSummaryDTO.java`
- `src/main/java/com/examen/dto/StudentEnrollmentCountDTO.java`

**Modified**

- `src/main/java/com/examen/repo/IEnrollmentRepo.java` (Q1, Q3, Q4, Q5, Q6)
- `src/main/java/com/examen/repo/ICourseRepo.java` (Q2)
- `src/main/java/com/examen/service/IEnrollmentService.java`
- `src/main/java/com/examen/service/Impl/IEnrollmentServiceImpl.java` (`dni.trim()`, `@Transactional` on `cancelEnrollmentsByStudent`, F1 step-by-step streams)
- `src/main/java/com/examen/service/ICourseService.java`
- `src/main/java/com/examen/service/Impl/ICourseServiceImpl.java`
- `src/main/java/com/examen/controller/EnrollmentController.java` (6 endpoints)
- `src/main/java/com/examen/controller/CourseController.java` (1 endpoint)

**Deviations**

- The spec names `service/impl/EnrollmentServiceImpl` and `CourseServiceImpl`; the existing classes are `service/Impl/IEnrollmentServiceImpl` and `ICourseServiceImpl`. The existing files were modified; nothing was renamed.
- In both implementations the injected `repo` field changed from `IGenericRepo<...>` to `IEnrollmentRepo` / `ICourseRepo`, needed to call the new queries. `getRepo()` is unchanged in behavior.
- Static endpoints (`/by-student/{dni}`, `/never-enrolled`, etc.) were placed before `/{id}` in the controllers; no mapping conflict.

**Verification**

`./mvnw.cmd -q compile` finished with exit code 0 (only Lombok Unsafe warnings). The application was not started and the database was not touched.