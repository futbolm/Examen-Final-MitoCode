package com.examen.service.Impl;

import com.examen.dto.StudentDTO;
import com.examen.model.Student;
import com.examen.repo.IGenericRepo;
import com.examen.service.IStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IStudentServiceImpl extends CRUDImpl<Student,Integer> implements IStudentService {

    private final IGenericRepo<Student,Integer> repo;

    @Override
    protected IGenericRepo<Student, Integer> getRepo() {
        return repo;
    }

    @Override
    public List<Student> listStudentsOrderByAgeDesc() {
        return repo.findAll()
                .stream()
                .sorted(Comparator.comparing(Student::getAge).reversed())
                .toList();
    }


}
