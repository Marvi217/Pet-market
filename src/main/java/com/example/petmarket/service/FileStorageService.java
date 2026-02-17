package com.example.petmarket.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    //Możliwe wstrzyknięcie wirusa najlepiej dodać Apache Tika do sprawdzenia zawartości

    @Value("${file.upload.dir:uploads/}")
    private String uploadDir;

    @Value("${file.upload.products.dir:uploads/products/}")
    private String productsUploadDir;

    @Value("${file.upload.categories.dir:uploads/categories/}")
    private String categoriesUploadDir;

    @Value("${file.upload.brands.dir:uploads/brands/}")
    private String brandsUploadDir;

    @Value("${file.upload.users.dir:uploads/users/}")
    private String usersUploadDir;

    public String storeFile(MultipartFile file, String directory) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Nie można zapisać pustego pliku");
        }

        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        if (originalFileName.contains("..")) {
            throw new IOException("Nazwa pliku zawiera nieprawidłową ścieżkę: " + originalFileName);
        }

        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        Path targetDirectory = getUploadPath(directory);

        if (!Files.exists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        Path targetLocation = targetDirectory.resolve(newFileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        log.info("Zapisano plik: {} -> {}", originalFileName, targetLocation);

        return "/" + directory + "/" + newFileName;
    }

    public void deleteFile(String fileUrl) throws IOException {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        String directory = extractDirectory(fileUrl);

        Path filePath = getUploadPath(directory).resolve(fileName);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("Usunięto plik: {}", filePath);
        } else {
            log.warn("Plik nie istnieje: {}", filePath);
        }
    }

    private Path getUploadPath(String directory) {
        String baseDir = switch (directory.toLowerCase()) {
            case "products" -> productsUploadDir;
            case "categories" -> categoriesUploadDir;
            case "users" -> usersUploadDir;
            case "brands" -> brandsUploadDir;
            default -> uploadDir + directory + "/";
        };

        return Paths.get(baseDir).toAbsolutePath().normalize();
    }

    private String extractDirectory(String fileUrl) {
        String[] parts = fileUrl.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "uploads";
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        return contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp");
    }

    public boolean isValidFileSize(MultipartFile file, long maxSizeInBytes) {
        return file != null && file.getSize() <= maxSizeInBytes;
    }
}