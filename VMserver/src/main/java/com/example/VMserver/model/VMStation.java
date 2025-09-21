package com.example.vmserver.model;

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
    //private eVMState state;
    private String login;
    private String hashPassword;
}

