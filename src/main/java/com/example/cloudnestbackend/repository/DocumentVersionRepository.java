package com.example.cloudnestbackend.repository;

import com.example.cloudnestbackend.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    Optional<DocumentVersion> findByIdAndDocumentId(UUID id, UUID documentId);

    void deleteByDocumentId(UUID documentId);
}
