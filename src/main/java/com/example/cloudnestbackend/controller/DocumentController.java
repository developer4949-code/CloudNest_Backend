package com.example.cloudnestbackend.controller;

import com.example.cloudnestbackend.dto.DashboardSummaryResponse;
import com.example.cloudnestbackend.dto.DocumentAccessResponse;
import com.example.cloudnestbackend.dto.DocumentDashboardResponse;
import com.example.cloudnestbackend.dto.DocumentVersionResponse;
import com.example.cloudnestbackend.entity.Document;
import com.example.cloudnestbackend.entity.DocumentAccess;
import com.example.cloudnestbackend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, Authentication authentication)
            throws IOException {
        String email = authentication.getName();
        Document doc = documentService.uploadDocument(file, email);
        return ResponseEntity.ok(Map.of(
                "message", "Document uploaded successfully",
                "documentId", doc.getId(),
                "name", doc.getName()));
    }

    @PostMapping("/{id}/version")
    public ResponseEntity<?> uploadNewVersion(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        String email = authentication.getName();
        documentService.uploadNewVersion(id, file, email);
        return ResponseEntity.ok("New version uploaded successfully");
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(documentService.getDashboardSummary(email));
    }

    @GetMapping("/owned")
    public ResponseEntity<List<DocumentDashboardResponse>> getOwned(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(documentService.getOwnedDocuments(email));
    }

    @GetMapping("/shared")
    public ResponseEntity<List<DocumentDashboardResponse>> getShared(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(documentService.getSharedDocuments(email));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<String> getDownloadUrl(@PathVariable UUID id, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(documentService.getDownloadUrl(id, email));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<String> getPreviewUrl(@PathVariable UUID id, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(documentService.getDownloadUrl(id, email)); // Same presigned URL logic
    }

    @PostMapping("/grant-access")
    public ResponseEntity<?> grantAccess(
            @RequestParam UUID documentId,
            @RequestParam String reviewerEmail,
            @RequestParam(required = false) Instant expiresAt,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        String token = documentService.grantAccess(documentId, reviewerEmail, ownerEmail, expiresAt);

        String reviewLink = "http://localhost:8080/review/" + token;
        return ResponseEntity.ok(Map.of(
                "message", "Access granted",
                "reviewLink", reviewLink,
                "token", token));
    }

    @GetMapping("/{id}/access")
    public ResponseEntity<List<DocumentAccessResponse>> getAccessList(@PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(documentService.getAccessList(id, email));
    }

    @PostMapping("/revoke/{token}")
    public ResponseEntity<?> revokeAccess(@PathVariable String token, Authentication authentication) {
        String email = authentication.getName();
        documentService.revokeAccess(token, email);
        return ResponseEntity.ok("Access revoked successfully");
    }

    @DeleteMapping("/access/{accessId}")
    public ResponseEntity<?> revokeAccessById(@PathVariable UUID accessId, Authentication authentication) {
        String email = authentication.getName();
        documentService.revokeAccessById(accessId, email);
        return ResponseEntity.ok("Access revoked successfully");
    }

    @PutMapping("/access/{accessId}")
    public ResponseEntity<?> updateAccess(
            @PathVariable UUID accessId,
            @RequestParam Instant expiresAt,
            Authentication authentication) {
        String email = authentication.getName();
        documentService.updateAccess(accessId, expiresAt, email);
        return ResponseEntity.ok("Access updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable UUID id, Authentication authentication) {
        String email = authentication.getName();
        documentService.deleteDocument(id, email);
        return ResponseEntity.ok("Document and all versions deleted successfully");
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> getVersions(@PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(documentService.getFileVersions(id, email));
    }

    @PostMapping("/{id}/restore/{versionId}")
    public ResponseEntity<?> restoreVersion(@PathVariable UUID id, @PathVariable UUID versionId,
            Authentication authentication) {
        String email = authentication.getName();
        documentService.restoreVersion(id, versionId, email);
        return ResponseEntity.ok("Version restored successfully");
    }
}
