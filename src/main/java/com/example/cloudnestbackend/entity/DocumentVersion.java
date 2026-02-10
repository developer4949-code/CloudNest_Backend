package com.example.cloudnestbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_versions")
@Getter
@Setter
@NoArgsConstructor
public class DocumentVersion {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private long size = 0;

    @Column(name = "uploaded_at")
    private Instant uploadedAt = Instant.now();

    public DocumentVersion(Document document, int versionNumber, String s3Key, long size) {
        this.document = document;
        this.versionNumber = versionNumber;
        this.s3Key = s3Key;
        this.size = size;
        this.uploadedAt = Instant.now();
    }
}
