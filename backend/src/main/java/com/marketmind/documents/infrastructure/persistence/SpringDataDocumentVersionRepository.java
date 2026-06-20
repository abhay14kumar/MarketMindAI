package com.marketmind.documents.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataDocumentVersionRepository
        extends JpaRepository<DocumentVersionJpaEntity, UUID> {

    Optional<DocumentVersionJpaEntity> findFirstByChecksumSha256IgnoreCase(String checksumSha256);

    List<DocumentVersionJpaEntity> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);
}
