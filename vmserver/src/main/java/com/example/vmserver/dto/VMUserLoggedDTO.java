package com.example.vmserver.dto;

import java.util.Set;

public record VMUserLoggedDTO(
    String username,
    String role,
    Set<String> permission){
        
    }
