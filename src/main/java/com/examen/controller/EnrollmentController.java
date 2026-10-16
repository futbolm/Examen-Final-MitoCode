package com.examen.controller;

import com.examen.dto.EnrollmentDTO;
import com.examen.dto.MonthlyEnrollmentSummaryDTO;
import com.examen.dto.StudentEnrollmentCountDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.examen.model.Enrollment;
import com.examen.service.IEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final IEnrollmentService service;

    private final ModelMapper defaultMapper;

    @GetMapping
    public ResponseEntity<List<EnrollmentDTO>>findAll(){
        List<EnrollmentDTO> list = service.findAll().stream().map(this::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/by-student/{dni}")
    public ResponseEntity<List<EnrollmentDTO>> findByStudentDni(@PathVariable String dni){
        List<EnrollmentDTO> list = service.findByStudentDni(dni).stream().map(this::toDTO).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<List<MonthlyEnrollmentSummaryDTO>> monthlySummary(){
        return ResponseEntity.ok(service.getMonthlyEnrollmentSummary());
    }

    @GetMapping("/frequent-students")
    public ResponseEntity<List<StudentEnrollmentCountDTO>> frequentStudents(@RequestParam(defaultValue = "1") long minEnrollments){
        return ResponseEntity.ok(service.getStudentsWithMoreThanNEnrollments(minEnrollments));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<EnrollmentDTO>> findAllPaged(
            @PageableDefault(page = 0, size = 20, sort = "dateTime", direction = Sort.Direction.DESC) Pageable pageable){
        Page<EnrollmentDTO> page = service.findAllWithStudent(pageable).map(this::toDTO);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/student/{idStudent}/cancel")
    public ResponseEntity<Integer> cancelByStudent(@PathVariable Integer idStudent){
        return ResponseEntity.ok(service.cancelEnrollmentsByStudent(idStudent));
    }

    @GetMapping("/students-by-course")
    public ResponseEntity<Map<String, List<String>>> studentsByCourse() {
        return ResponseEntity.ok(service.getStudentsByCourse());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentDTO>findById(@PathVariable Integer id){
        Enrollment obj = service.findById(id);
        return ResponseEntity.ok(toDTO(obj));
    }

    @PostMapping
    public ResponseEntity<EnrollmentDTO> save(@Valid @RequestBody EnrollmentDTO dto){
        Enrollment obj = service.save(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(obj));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnrollmentDTO> update(@PathVariable Integer id, @Valid @RequestBody EnrollmentDTO dto) throws Exception {
        Enrollment obj = service.update(id , toEntity(dto));
        return ResponseEntity.ok(toDTO(obj));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }




    private EnrollmentDTO toDTO(Enrollment entity) {
        return defaultMapper.map(entity, EnrollmentDTO.class);
    }

    private Enrollment toEntity(EnrollmentDTO dto) {
        Enrollment enrollment = defaultMapper.map(dto, Enrollment.class);
        // El lado propietario de la relacion es el detalle: se enlaza cada detalle con su cabecera
        enrollment.getDetails().forEach(d -> d.setEnrollment(enrollment));
        return enrollment;
    }
}
