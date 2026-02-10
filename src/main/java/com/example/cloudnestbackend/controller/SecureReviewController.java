package com.example.cloudnestbackend.controller;

import com.example.cloudnestbackend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class SecureReviewController {

    private final DocumentService documentService;

    @GetMapping("/review/{token}")
    public ResponseEntity<?> review(@PathVariable String token) {
        try {
            String presignedUrl = documentService.generateReviewUrl(token);

            // Redirect to S3
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(presignedUrl))
                    .build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: " + e.getMessage());
        }
    }
}
