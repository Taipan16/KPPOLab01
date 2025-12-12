package com.example.vmserver.dto;

public record AssignStationRequest(
    Long userId,
    Long stationId
) {}