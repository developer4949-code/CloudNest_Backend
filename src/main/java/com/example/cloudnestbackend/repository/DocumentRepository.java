package com.example.cloudnestbackend.repository;

import com.example.cloudnestbackend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByOwnerEmail(String email);

    Optional<Document> findByOwnerEmailAndName(String email, String name);
}
