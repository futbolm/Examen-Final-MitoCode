package com.examen.dto;

public record StudentEnrollmentCountDTO(
        Integer idStudent, String dni, String fullName, Long enrollmentsCount
) { }
