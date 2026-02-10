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
public class DocumentDashboardResponse {
    private UUID id;
    private String name;
    private boolean isOwner;
    private int versionCount;
    private String ownerEmail;
    private String ownerName; // Display name
    private Instant createdAt;
    private Instant uploadedAt;
    private long size;
    private String accessType; // Permanent or Time-Limited
    private Instant expiresAt;
    private UUID accessId; // To identify the specific share record
    private String reviewUrl;
}
