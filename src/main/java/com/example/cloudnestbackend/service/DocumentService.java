package com.example.cloudnestbackend.service;

import com.example.cloudnestbackend.dto.DashboardSummaryResponse;
import com.example.cloudnestbackend.dto.DocumentAccessResponse;
import com.example.cloudnestbackend.dto.DocumentDashboardResponse;
import com.example.cloudnestbackend.dto.DocumentVersionResponse;
import com.example.cloudnestbackend.dto.SharingActivityResponse;
import com.example.cloudnestbackend.entity.Document;
import com.example.cloudnestbackend.entity.DocumentAccess;
import com.example.cloudnestbackend.entity.DocumentVersion;
import com.example.cloudnestbackend.entity.User;
import com.example.cloudnestbackend.repository.DocumentAccessRepository;
import com.example.cloudnestbackend.repository.DocumentRepository;
import com.example.cloudnestbackend.repository.DocumentVersionRepository;
import com.example.cloudnestbackend.repository.UserRepository;
import com.example.cloudnestbackend.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentAccessRepository accessRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Transactional
    public Document uploadDocument(MultipartFile file, String ownerEmail) throws IOException {
        System.out.println("DEBUG: Upload started for user: " + ownerEmail);
        System.out.println("DEBUG: File name: " + file.getOriginalFilename() + ", Size: " + file.getSize());

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("User not found in database: " + ownerEmail));

        // Check if document with same name exists for this user
        Optional<Document> existingDoc = documentRepository.findByOwnerEmailAndName(ownerEmail,
                file.getOriginalFilename());
        if (existingDoc.isPresent()) {
            System.out.println(
                    "DEBUG: Document name collision. Adding new version instead. ID: " + existingDoc.get().getId());
            uploadNewVersion(existingDoc.get().getId(), file, ownerEmail);
            return existingDoc.get();
        }

        UUID documentId = UUID.randomUUID();
        String s3Key = "documents/" + documentId + "/v1-" + file.getOriginalFilename();

        try {
            s3Service.uploadFile(s3Key, file);
            System.out.println("DEBUG: S3 Upload success. Key: " + s3Key);
        } catch (Exception e) {
            System.err.println("DEBUG: S3 Upload FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("S3 Storage Error: " + e.getMessage());
        }

        Document doc = new Document();
        doc.setId(documentId);
        doc.setName(file.getOriginalFilename());
        doc.setOwnerEmail(ownerEmail);
        doc.setS3Key(s3Key);
        doc.setOwner(owner);
        doc.setCurrentVersion(1);
        doc.setSize(file.getSize());
        System.out.println("DEBUG: Document size set to: " + doc.getSize());

        Document savedDoc = documentRepository.save(doc);
        System.out.println(
                "DEBUG: Document saved. ID: " + savedDoc.getId() + ", Size in saved object: " + savedDoc.getSize());

        DocumentVersion version = new DocumentVersion(savedDoc, 1, s3Key, file.getSize());
        versionRepository.save(version);
        System.out.println("DEBUG: DocumentVersion saved with size: " + version.getSize());

        return savedDoc;
    }

    @Transactional
    public void uploadNewVersion(UUID documentId, MultipartFile file, String email) throws IOException {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Only the owner can upload new versions");
        }

        int nextVersion = doc.getCurrentVersion() + 1;
        String s3Key = "documents/" + documentId + "/v" + nextVersion + "-" + file.getOriginalFilename();

        s3Service.uploadFile(s3Key, file);

        DocumentVersion version = new DocumentVersion(doc, nextVersion, s3Key, file.getSize());
        versionRepository.save(version);

        doc.setCurrentVersion(nextVersion);
        doc.setS3Key(s3Key);
        doc.setSize(file.getSize());
        documentRepository.save(doc);
    }

    public DashboardSummaryResponse getDashboardSummary(String email) {
        List<DocumentDashboardResponse> owned = getOwnedDocuments(email);
        List<DocumentDashboardResponse> shared = getSharedDocuments(email);

        List<DocumentDashboardResponse> allFiles = new ArrayList<>(owned);
        allFiles.addAll(shared);

        // Sort by created date desc for recent uploads
        allFiles.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // Calculate total storage from owned files
        long totalStorageBytes = owned.stream()
                .mapToLong(DocumentDashboardResponse::getSize)
                .sum();

        // Fetch sharing activity (files this user shared with others)
        List<SharingActivityResponse> sharing = accessRepository.findByDocumentOwnerEmail(email).stream()
                .map(access -> SharingActivityResponse.builder()
                        .documentId(access.getDocument().getId())
                        .fileName(access.getDocument().getName())
                        .sharedWith(access.getUserEmail())
                        .type(access.getExpiresAt() != null ? "Time-Limited" : "Permanent")
                        .sharedAt(access.getCreatedAt())
                        .build())
                .sorted((a, b) -> {
                    if (a.getSharedAt() == null && b.getSharedAt() == null)
                        return 0;
                    if (a.getSharedAt() == null)
                        return 1;
                    if (b.getSharedAt() == null)
                        return -1;
                    return b.getSharedAt().compareTo(a.getSharedAt());
                })
                .limit(20) // Increased limit
                .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
                .totalFiles(allFiles.size())
                .totalStorageBytes(totalStorageBytes)
                .quotaBytes(21474836480L) // 20 GB in bytes
                .recentUploads(allFiles.stream().limit(5).collect(Collectors.toList()))
                .recentRestores(new ArrayList<>())
                .sharingActivity(sharing)
                .build();
    }

    public List<DocumentDashboardResponse> getOwnedDocuments(String email) {
        List<Document> owned = documentRepository.findByOwnerEmail(email);
        return owned.stream()
                .map(doc -> mapToDashboardResponse(doc, true))
                .collect(Collectors.toList());
    }

    public List<DocumentDashboardResponse> getSharedDocuments(String email) {
        List<DocumentAccess> accesses = accessRepository.findByUserEmailAndRevokedFalse(email);
        return accesses.stream()
                .map(access -> mapToSharedDashboardResponse(access))
                .collect(Collectors.toList());
    }

    private DocumentDashboardResponse mapToDashboardResponse(Document doc, boolean isOwner) {
        System.out.println("DEBUG: Mapping doc: " + doc.getName() + ", Size: " + doc.getSize());
        return DocumentDashboardResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .isOwner(isOwner)
                .versionCount(doc.getCurrentVersion())
                .ownerEmail(doc.getOwnerEmail())
                .ownerName(doc.getOwner() != null ? doc.getOwner().getFullName() : doc.getOwnerEmail())
                .createdAt(doc.getCreatedAt())
                .uploadedAt(doc.getCreatedAt())
                .size(doc.getSize())
                .build();
    }

    private DocumentDashboardResponse mapToSharedDashboardResponse(DocumentAccess access) {
        Document doc = access.getDocument();
        System.out.println("DEBUG: Mapping shared doc: " + doc.getName() + ", Size: " + doc.getSize());
        return DocumentDashboardResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .isOwner(false)
                .versionCount(doc.getCurrentVersion())
                .ownerEmail(doc.getOwnerEmail())
                .ownerName(doc.getOwner() != null ? doc.getOwner().getFullName() : doc.getOwnerEmail())
                .createdAt(doc.getCreatedAt())
                .uploadedAt(doc.getCreatedAt())
                .size(doc.getSize())
                .accessType(access.getExpiresAt() != null ? "Time-Limited" : "Permanent")
                .expiresAt(access.getExpiresAt())
                .accessId(access.getId())
                .build();
    }

    @Transactional
    public String grantAccess(UUID documentId, String reviewerEmail, String ownerEmail, Instant expiresAt) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getOwnerEmail().equals(ownerEmail)) {
            throw new RuntimeException("Only the owner can grant access");
        }

        // Default to 3 days if not provided
        Instant expiry = (expiresAt != null) ? expiresAt : Instant.now().plus(3, ChronoUnit.DAYS);

        String token = TokenUtil.generate();
        DocumentAccess access = new DocumentAccess(
                doc,
                reviewerEmail,
                token,
                expiry);

        accessRepository.save(access);
        return token;
    }

    public List<DocumentAccessResponse> getAccessList(UUID documentId, String email) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Only the owner can view access list");
        }

        return accessRepository.findByDocumentId(documentId).stream()
                .map(access -> DocumentAccessResponse.builder()
                        .id(access.getId())
                        .documentId(access.getDocument().getId())
                        .userEmail(access.getUserEmail())
                        .expiresAt(access.getExpiresAt())
                        .revoked(access.isRevoked())
                        .createdAt(access.getCreatedAt())
                        .accessType(access.getExpiresAt() != null ? "Time-Limited" : "Permanent")
                        .build())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeAccessById(UUID accessId, String email) {
        DocumentAccess access = accessRepository.findById(accessId)
                .orElseThrow(() -> new RuntimeException("Access record not found"));

        Document doc = access.getDocument();
        if (!doc.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Not authorized to revoke this access");
        }

        access.setRevoked(true);
        accessRepository.save(access);
    }

    @Transactional
    public void updateAccess(UUID accessId, Instant expiresAt, String email) {
        DocumentAccess access = accessRepository.findById(accessId)
                .orElseThrow(() -> new RuntimeException("Access record not found"));

        Document doc = access.getDocument();
        if (!doc.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Not authorized to update this access");
        }

        access.setExpiresAt(expiresAt);
        accessRepository.save(access);
    }

    @Transactional
    public void revokeAccess(String token, String email) {
        DocumentAccess access = accessRepository.findByAccessToken(token)
                .orElseThrow(() -> new RuntimeException("Access token invalid"));

        // Allow either the owner of the document or the reviewer themselves to
        // revoke/remove
        Document doc = access.getDocument();
        if (!doc.getOwnerEmail().equals(email) && !access.getUserEmail().equals(email)) {
            throw new RuntimeException("Not authorized to revoke this access");
        }

        access.setRevoked(true);
        accessRepository.save(access);
    }

    @Transactional
    public void deleteDocument(UUID documentId, String email) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Only the owner can delete a document permanently");
        }

        // Delete all S3 objects for all versions
        List<DocumentVersion> versions = versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);
        for (DocumentVersion v : versions) {
            s3Service.deleteFile(v.getS3Key());
        }

        // Repositories handle cascade or manual deletion
        accessRepository.deleteByDocumentId(documentId);
        versionRepository.deleteByDocumentId(documentId);
        documentRepository.delete(doc);
    }

    public String getDownloadUrl(UUID documentId, String email) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Check if owner or has access
        boolean isOwner = doc.getOwnerEmail().equals(email);
        boolean hasAccess = accessRepository.findByDocumentId(documentId).stream()
                .anyMatch(a -> a.getUserEmail().equals(email) && !a.isRevoked()
                        && a.getExpiresAt().isAfter(Instant.now()));

        if (!isOwner && !hasAccess) {
            throw new RuntimeException("Not authorized to access this document");
        }

        return s3Service.generatePresignedDownloadUrl(doc.getS3Key());
    }

    public List<DocumentVersionResponse> getFileVersions(UUID documentId, String email) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Only the owner can view version history");
        }

        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(v -> DocumentVersionResponse.builder()
                        .id(v.getId())
                        .versionNumber(v.getVersionNumber())
                        .s3Key(v.getS3Key())
                        .size(v.getSize())
                        .uploadedAt(v.getUploadedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void restoreVersion(UUID documentId, UUID versionId, String email) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Only the owner can restore versions");
        }

        DocumentVersion version = versionRepository.findByIdAndDocumentId(versionId, documentId)
                .orElseThrow(() -> new RuntimeException("Version not found for this document"));

        // Update document to point to this version's S3 key and size
        doc.setS3Key(version.getS3Key());
        doc.setSize(version.getSize());
        documentRepository.save(doc);
    }

    public String generateReviewUrl(String token) {
        DocumentAccess access = accessRepository.findByAccessToken(token)
                .orElseThrow(() -> new RuntimeException("Access invalid"));

        if (access.isRevoked() || access.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Access is revoked or expired");
        }

        return s3Service.generatePresignedDownloadUrl(access.getDocument().getS3Key());
    }
}
