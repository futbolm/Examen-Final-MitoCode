package com.examen.service;

import com.examen.dto.StudentDTO;
import com.examen.model.Student;

import java.util.List;
import java.util.Map;

public interface IStudentService extends ICRUD<Student, Integer>{

    List<Student> listStudentsOrderByAgeDesc();


}
