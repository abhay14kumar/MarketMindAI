package com.marketmind.documents.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataDocumentSourceRepository
        extends JpaRepository<DocumentSourceJpaEntity, UUID> {

    boolean existsByCodeIgnoreCase(String code);
}
