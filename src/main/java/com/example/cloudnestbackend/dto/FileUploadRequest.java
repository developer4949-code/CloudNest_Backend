package com.example.cloudnestbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {
    private String filename;
    private Long size;
    private String contentType;
}

@Data
@AllArgsConstructor
class FileUploadResponse {
    private String uploadUrl;
    private String s3Key;
}
