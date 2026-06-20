package com.marketmind.documents.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataDownloadJobRepository extends JpaRepository<DownloadJobJpaEntity, UUID> {
}
