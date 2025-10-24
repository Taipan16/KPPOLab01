package com.example.vmserver.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

//import java.time.DateTimeException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class VMQueue {
    @Id
    private Long id;
    @ManyToOne
    private User currentUser;
    @OneToOne
    private VMStation vmStation;
}