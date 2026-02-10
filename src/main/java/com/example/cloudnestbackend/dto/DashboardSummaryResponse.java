package com.example.cloudnestbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private int totalFiles;
    private long totalStorageBytes;
    private long quotaBytes;
    private List<DocumentDashboardResponse> recentUploads;
    private List<Object> recentRestores; // Placeholder for now
    private List<SharingActivityResponse> sharingActivity;
}
