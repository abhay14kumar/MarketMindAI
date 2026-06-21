package com.marketmind.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketmind.discovery.domain.DiscoveredDocumentStatus;
import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.support.DiscoveryTestFixtures;
import com.marketmind.discovery.support.InMemoryDiscoveryRepository;

import org.junit.jupiter.api.Test;

class DiscoveryServiceTest {

    @Test
    void shouldMarkRepeatedUrlsExistingAndIncrementSeenCount() {
        InMemoryDiscoveryRepository repository = new InMemoryDiscoveryRepository();
        DiscoveryService service = DiscoveryTestFixtures.service(repository);
        DiscoveryRunCommand command = new DiscoveryRunCommand(
                DiscoverySourceType.TEST_SOURCE, null, "RELIANCE", 20);

        var first = service.run(command);
        var second = service.run(command);
        var documents = service.getDocuments(
                new DiscoveryDocumentFilter(null, null, null, null), 0, 100);

        assertThat(first.job().newDocuments()).isEqualTo(4);
        assertThat(second.job().existingDocuments()).isEqualTo(4);
        assertThat(documents.content())
                .allMatch(document -> document.status()
                        == DiscoveredDocumentStatus.EXISTING)
                .allMatch(document -> document.seenCount() == 2);
    }
}
