package com.marketmind.documents.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class Sha256ChecksumServiceTest {

    private final Sha256ChecksumService checksumService = new Sha256ChecksumService();

    @Test
    void shouldCalculateSha256Checksum() {
        String checksum = checksumService.sha256(new ByteArrayInputStream(
                "abc".getBytes(StandardCharsets.UTF_8)));

        assertThat(checksum)
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223"
                        + "b00361a396177a9cb410ff61f20015ad");
    }
}
