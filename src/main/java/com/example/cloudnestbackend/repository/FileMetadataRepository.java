package com.example.cloudnestbackend.repository;

import com.example.cloudnestbackend.entity.FileMetadata;
import com.example.cloudnestbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByUser(User user);

    Optional<FileMetadata> findByS3Key(String s3Key);
}
