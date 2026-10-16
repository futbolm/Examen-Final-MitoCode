package com.examen.dto;

public record MonthlyEnrollmentSummaryDTO(
        Integer year,
        Integer month,
        Long enrollmentsCount,
        Long coursesCount
) {

}
