package com.examen.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrollmentDetailDTO {

    private Integer idEnrollmentDetail;


    @NotNull(message = "El curso es obligatorio")
    private CourseDTO course;


    private Integer idEnrollment;

    @NotBlank(message = "El aula es obligatoria")
    @Size(min = 1, max = 20, message = "El aula debe tener entre 1 y 20 caracteres")
    private String classroom;

}
