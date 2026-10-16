package com.examen.service.Impl;

import com.examen.model.Course;
import com.examen.repo.ICourseRepo;
import com.examen.repo.IGenericRepo;
import com.examen.service.ICourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ICourseServiceImpl extends CRUDImpl<Course,Integer> implements ICourseService {

    private final ICourseRepo repo;

    @Override
    protected IGenericRepo<Course, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<Course> findCoursesNeverEnrolled() {
        return repo.findCoursesNeverEnrolled();
    }
}
