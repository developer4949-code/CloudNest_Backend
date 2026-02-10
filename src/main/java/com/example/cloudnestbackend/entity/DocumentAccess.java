package com.example.cloudnestbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_access")
@Getter
@Setter
@NoArgsConstructor
public class DocumentAccess {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "access_token", unique = true, nullable = false)
    private String accessToken;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private boolean revoked = false;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public DocumentAccess(Document document, String userEmail, String accessToken, Instant expiresAt) {
        this.document = document;
        this.userEmail = userEmail;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.createdAt = Instant.now();
    }
}
