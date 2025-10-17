package com.example.vmserver.dto;

import java.io.Serializable;
import java.util.Set;

public record VMUserDTO(
    Long id,
    String username,
    String password,
    String role,
    Set<String> permission) implements Serializable{

    }
