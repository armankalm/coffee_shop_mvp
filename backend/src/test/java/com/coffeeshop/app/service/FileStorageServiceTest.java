package com.coffeeshop.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void store_invalidContentType_throwsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noext", "application/octet-stream", "data".getBytes());

        assertThatThrownBy(() -> fileStorageService.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only image files are allowed");
    }

    @Test
    void store_nullContentType_throwsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", null, "data".getBytes());

        assertThatThrownBy(() -> fileStorageService.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only image files are allowed");
    }

    @Test
    void store_pngFile_works() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "data".getBytes());

        String result = fileStorageService.store(file);

        assertThat(result).startsWith("/uploads/products/");
        assertThat(result).endsWith(".png");
    }
}
