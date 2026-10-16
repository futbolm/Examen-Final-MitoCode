package com.examen.controller;

import com.examen.dto.StudentDTO;
import com.examen.model.Student;
import com.examen.service.IStudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final IStudentService service;

    private final ModelMapper defaultMapper;

    @GetMapping
    public ResponseEntity<List<StudentDTO>>findAll(){
        List<StudentDTO> list = service.findAll().stream().map(this::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO>findById(@PathVariable Integer id){
        Student obj = service.findById(id);
        return ResponseEntity.ok(toDTO(obj));
    }

    @PostMapping
    public ResponseEntity<StudentDTO> save(@Valid @RequestBody StudentDTO dto){
        Student obj = service.save(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(obj));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> update(@PathVariable Integer id, @Valid @RequestBody StudentDTO dto) throws Exception {
        Student obj = service.update(id , toEntity(dto));
        return ResponseEntity.ok(toDTO(obj));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StudentDTO> deleteById(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/order-by-age")
    public ResponseEntity<List<StudentDTO>> listOrderByAge() {
        List<StudentDTO> list = service.listStudentsOrderByAgeDesc()
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    private StudentDTO toDTO(Student entity) {
        return defaultMapper.map(entity, StudentDTO.class);
    }

    private Student toEntity(StudentDTO dto) {
        return defaultMapper.map(dto, Student.class);
    }
}
