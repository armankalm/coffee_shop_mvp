package com.coffeeshop.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void store_savesFileAndReturnsPath() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-image.jpg", "image/jpeg", "fake-image-content".getBytes());

        String result = fileStorageService.store(file);

        assertThat(result).startsWith("/uploads/products/");
        assertThat(result).endsWith(".jpg");

        String filename = result.substring(result.lastIndexOf('/') + 1);
        Path storedFile = tempDir.resolve(filename);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.readString(storedFile)).isEqualTo("fake-image-content");
    }

    @Test
    void store_fileWithoutExtension_works() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noext", "application/octet-stream", "data".getBytes());

        String result = fileStorageService.store(file);

        assertThat(result).startsWith("/uploads/products/");
        assertThat(result).doesNotContain(".");
    }

    @Test
    void store_nullFilename_works() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "image/png", "data".getBytes());

        String result = fileStorageService.store(file);

        assertThat(result).startsWith("/uploads/products/");
    }
}
