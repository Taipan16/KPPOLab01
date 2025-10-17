package com.example.vmserver.dto;

import java.util.Set;

public record VMUserLoggedDTO(
    Long id,
    String username,
    String password,
    String role,
    Set<String> permission){
        
    }
