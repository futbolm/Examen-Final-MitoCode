package com.examen.repo;

import com.examen.dto.MonthlyEnrollmentSummaryDTO;
import com.examen.dto.StudentEnrollmentCountDTO;
import com.examen.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IEnrollmentRepo extends IGenericRepo<Enrollment,Integer> {

    @Query("SELECT e FROM Enrollment e WHERE e.student.dni = :dni ORDER BY e.dateTime DESC")
    List<Enrollment> findByStudentDni(@Param("dni") String dni);

    @Query(value = "SELECT e FROM Enrollment e JOIN FETCH e.student s",
           countQuery = "SELECT COUNT(e) FROM Enrollment e JOIN e.student s")
    Page<Enrollment> findAllWithStudent(Pageable pageable);

    @Query("""
           SELECT new com.examen.dto.MonthlyEnrollmentSummaryDTO(
                  YEAR(e.dateTime), MONTH(e.dateTime), COUNT(DISTINCT e), COUNT(ed))
           FROM Enrollment e JOIN e.details ed
           WHERE e.enabled = true
           GROUP BY YEAR(e.dateTime), MONTH(e.dateTime)
           ORDER BY YEAR(e.dateTime) DESC, MONTH(e.dateTime) DESC
           """)
    List<MonthlyEnrollmentSummaryDTO> getMonthlyEnrollmentSummary();

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Enrollment e
              SET e.enabled = false
            WHERE e.student.idStudent = :idStudent
              AND e.enabled = true
           """)
    int cancelEnrollmentsByStudent(@Param("idStudent") Integer idStudent);
}
