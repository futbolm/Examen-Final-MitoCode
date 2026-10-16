package com.examen.service;

import com.examen.dto.MonthlyEnrollmentSummaryDTO;
import com.examen.dto.StudentEnrollmentCountDTO;
import com.examen.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface IEnrollmentService extends ICRUD<Enrollment, Integer>{

    List<Enrollment> findByStudentDni(String dni);

    List<MonthlyEnrollmentSummaryDTO> getMonthlyEnrollmentSummary();

    List<StudentEnrollmentCountDTO> getStudentsWithMoreThanNEnrollments(long minEnrollments);

    Page<Enrollment> findAllWithStudent(Pageable pageable);

    int cancelEnrollmentsByStudent(Integer idStudent);

    Map<String, List<String>> getStudentsByCourse();
}
