package com.example.cloudnestbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharingActivityResponse {
    private UUID documentId;
    private String fileName;
    private String sharedWith;
    private String type;
    private Instant sharedAt;
}
