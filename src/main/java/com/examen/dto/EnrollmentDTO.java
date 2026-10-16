package com.examen.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrollmentDTO {

    private Integer idEnrollment;

    @NotNull(message = "La fecha de matrícula es obligatoria")
    private LocalDateTime dateTime;


    @NotNull(message = "El estudiante es obligatorio")
    private StudentDTO student;

    @NotEmpty(message = "La matrícula debe tener al menos un detalle")
    @Valid
    private List<@NotNull EnrollmentDetailDTO> details;

    @NotNull(message = "El estado de la matrícula es obligatorio")
    private Boolean enabled;

}
