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
public class DocumentAccessResponse {
    private UUID id; // This is the access ID
    private UUID documentId;
    private String userEmail;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;
    private String accessType; // Added for convenience: "Permanent" or "Time-Limited"
}
