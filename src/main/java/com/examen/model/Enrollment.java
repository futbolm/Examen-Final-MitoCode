package com.examen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idEnrollment;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @ManyToOne
    @JoinColumn(name="id_student", nullable = false, foreignKey = @ForeignKey(name="fk_Enrollment_student"))
    private Student student;

    @OneToMany(mappedBy = "enrollment",cascade = CascadeType.ALL,orphanRemoval = true )
    private List<EnrollmentDetail> details;

    @Column(nullable = false)
    private boolean enabled;

}
