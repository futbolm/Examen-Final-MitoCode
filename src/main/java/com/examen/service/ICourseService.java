package com.examen.service;

import com.examen.model.Course;

import java.util.List;

public interface ICourseService extends ICRUD<Course, Integer>{

    List<Course> findCoursesNeverEnrolled();
}
