package com.examen.repo;

import com.examen.model.Course;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ICourseRepo extends IGenericRepo<Course,Integer> {

    @Query("SELECT c FROM Course c LEFT JOIN EnrollmentDetail ed ON ed.course = c WHERE ed IS NULL ORDER BY c.name ASC")
    List<Course> findCoursesNeverEnrolled();
}
