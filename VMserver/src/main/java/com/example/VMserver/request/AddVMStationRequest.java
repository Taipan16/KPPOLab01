package com.example.VMserver.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//сущность запроса на добавление вм
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddVMStationRequest {
    private String ip;
    private int port;
    private String login;
    private String password;
}