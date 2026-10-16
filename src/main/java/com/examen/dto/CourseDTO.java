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
public class CourseDTO {

    private Integer idCourse;

    @NotBlank(message = "El nombre del curso es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String name;

    @NotBlank(message = "Las siglas del curso son obligatorias")
    @Size(min = 1, max = 15, message = "Las siglas deben tener entre 1 y 15 caracteres")
    private String siglas;

    @NotNull(message = "El estado del curso es obligatorio")
    private Boolean enabled;

}
