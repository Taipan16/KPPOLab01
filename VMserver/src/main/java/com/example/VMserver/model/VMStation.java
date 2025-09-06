package com.example.VMserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VMStation {
    private Long id;
    private String ip;
    private int port;
    private VMState state;
    private String Login;
    private String hashPassword;
}
