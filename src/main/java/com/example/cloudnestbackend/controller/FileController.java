package com.example.cloudnestbackend.controller;

import com.example.cloudnestbackend.dto.FileUploadRequest;
import com.example.cloudnestbackend.entity.FileMetadata;
import com.example.cloudnestbackend.entity.User;
import com.example.cloudnestbackend.repository.FileMetadataRepository;
import com.example.cloudnestbackend.repository.UserRepository;
import com.example.cloudnestbackend.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final S3Service s3Service;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;

    public FileController(S3Service s3Service,
            FileMetadataRepository fileMetadataRepository,
            UserRepository userRepository) {
        this.s3Service = s3Service;
        this.fileMetadataRepository = fileMetadataRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<?> getUploadUrl(@RequestBody FileUploadRequest request, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String s3Key = s3Service.generateUniqueS3Key(request.getFilename());
        String uploadUrl = s3Service.generatePresignedUploadUrl(s3Key, request.getContentType());

        // Save metadata initially (maybe with a 'PENDING' status if we add one,
        // but for now we just prepare the record)
        FileMetadata metadata = new FileMetadata();
        metadata.setFilename(request.getFilename());
        metadata.setS3Key(s3Key);
        metadata.setSize(request.getSize());
        metadata.setContentType(request.getContentType());
        metadata.setUser(user);

        fileMetadataRepository.save(metadata);

        return ResponseEntity.ok(Map.of(
                "uploadUrl", uploadUrl,
                "s3Key", s3Key,
                "fileId", metadata.getId()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> getDownloadUrl(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();

        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Basic Access Control: Only the owner can download
        if (!metadata.getUser().getEmail().equals(email)) {
            return ResponseEntity.status(403).body("You do not have permission to access this file");
        }

        String downloadUrl = s3Service.generatePresignedDownloadUrl(metadata.getS3Key());

        return ResponseEntity.ok(Map.of(
                "downloadUrl", downloadUrl,
                "filename", metadata.getFilename()));
    }
}
