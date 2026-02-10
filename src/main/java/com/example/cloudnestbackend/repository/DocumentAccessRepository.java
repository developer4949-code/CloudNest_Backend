package com.example.cloudnestbackend.repository;

import com.example.cloudnestbackend.entity.DocumentAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentAccessRepository extends JpaRepository<DocumentAccess, UUID> {
    List<DocumentAccess> findByUserEmailAndRevokedFalse(String userEmail);

    Optional<DocumentAccess> findByAccessToken(String accessToken);

    Optional<DocumentAccess> findByDocumentIdAndUserEmail(UUID documentId, String userEmail);

    List<DocumentAccess> findByDocumentId(UUID documentId);

    @org.springframework.data.jpa.repository.Query("SELECT da FROM DocumentAccess da JOIN da.document d WHERE d.ownerEmail = :ownerEmail AND da.revoked = false")
    List<DocumentAccess> findByDocumentOwnerEmail(String ownerEmail);

    void deleteByDocumentId(UUID documentId);
}
