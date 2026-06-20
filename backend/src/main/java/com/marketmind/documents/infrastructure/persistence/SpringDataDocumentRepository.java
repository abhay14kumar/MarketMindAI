package com.marketmind.documents.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataDocumentRepository extends JpaRepository<DocumentJpaEntity, UUID> {

    Optional<DocumentJpaEntity> findBySourceUrl(String sourceUrl);
}
