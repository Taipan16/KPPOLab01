package com.example.VMserver.model;

import com.example.VMserver.entity.eSubscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    private Long id;
    private eSubscription name;
    private String description;
    private String priority;
}