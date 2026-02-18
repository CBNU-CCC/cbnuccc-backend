package com.cbnuccc.cbnuccc.Model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Entity
@Table(name = "user", schema = "public")
public class MyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(value = AccessLevel.NONE)
    private Integer id;

    // UserDto
    private UUID uuid;

    // UserDto
    private String email;

    private String password;

    // UserDto
    private Short rank;

    // UserDto
    private Boolean sex;

    // UserDto
    private String name;

    // UserDto
    private Short grade;

    private String studentId;

    // UserDto
    // TODO: delete birthDate
    private LocalDate birthDate;
}
