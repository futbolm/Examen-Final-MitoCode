package com.examen.controller;

import com.examen.dto.CourseDTO;
import com.examen.model.Course;
import com.examen.service.ICourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final ICourseService service;

    private final ModelMapper defaultMapper;

    @GetMapping
    public ResponseEntity<List<CourseDTO>>findAll(){
        List<CourseDTO> list = service.findAll().stream().map(this::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/never-enrolled")
    public ResponseEntity<List<CourseDTO>> findCoursesNeverEnrolled(){
        List<CourseDTO> list = service.findCoursesNeverEnrolled().stream().map(this::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO>findById(@PathVariable Integer id){
        Course obj = service.findById(id);
        return ResponseEntity.ok(toDTO(obj));
    }

    @PostMapping
    public ResponseEntity<CourseDTO> save(@Valid @RequestBody CourseDTO dto){
        Course obj = service.save(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(obj));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDTO> update(@PathVariable Integer id, @Valid @RequestBody CourseDTO dto) throws Exception {
        Course obj = service.update(id , toEntity(dto));
        return ResponseEntity.ok(toDTO(obj));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private CourseDTO toDTO(Course entity) {
        return defaultMapper.map(entity, CourseDTO.class);
    }

    private Course toEntity(CourseDTO dto) {
        return defaultMapper.map(dto, Course.class);
    }
}
