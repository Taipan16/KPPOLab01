package com.example.VMserver.model;

import java.time.DateTimeException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VMQueue {
    private Long id;
    private User currentUser;
    private DateTimeException beginSession;
}