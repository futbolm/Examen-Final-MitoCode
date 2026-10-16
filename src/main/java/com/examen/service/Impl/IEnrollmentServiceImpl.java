package com.examen.service.Impl;

import com.examen.dto.MonthlyEnrollmentSummaryDTO;
import com.examen.dto.StudentEnrollmentCountDTO;
import com.examen.model.Enrollment;
import com.examen.model.EnrollmentDetail;
import com.examen.repo.IEnrollmentRepo;
import com.examen.repo.IGenericRepo;
import com.examen.service.IEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class IEnrollmentServiceImpl extends CRUDImpl<Enrollment,Integer> implements IEnrollmentService {

    private final IEnrollmentRepo repo;

    @Override
    protected IGenericRepo<Enrollment, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<Enrollment> findByStudentDni(String dni) {
        return repo.findByStudentDni(dni.trim());
    }

    @Override
    public List<MonthlyEnrollmentSummaryDTO> getMonthlyEnrollmentSummary() {
        return repo.getMonthlyEnrollmentSummary();
    }

    @Override
    public List<StudentEnrollmentCountDTO> getStudentsWithMoreThanNEnrollments(long minEnrollments) {
        return repo.getStudentsWithMoreThanNEnrollments(minEnrollments);
    }

    @Override
    public Page<Enrollment> findAllWithStudent(Pageable pageable) {
        return repo.findAllWithStudent(pageable);
    }

    @Override
    @Transactional
    public int cancelEnrollmentsByStudent(Integer idStudent) {
        return repo.cancelEnrollmentsByStudent(idStudent);
    }

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
}
