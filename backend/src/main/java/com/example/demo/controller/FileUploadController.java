package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final String UPLOAD_DIR = "uploads/";

    @PostMapping
    public ResponseEntity<List<String>> uploadFiles(@RequestParam("files") List<MultipartFile> files) {
        List<String> fileUrls = new ArrayList<>();

        try {
            // Se la cartella "uploads" non esiste, la crea automaticamente
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            for (MultipartFile file : files) {
                // Genera un nome univoco (es. 550e8400-e29b-41d4-a716-446655440000.jpg)
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".jpg";
                String newFilename = UUID.randomUUID().toString() + extension;

                // Salva fisicamente il file su disco
                Path filePath = uploadPath.resolve(newFilename);
                Files.copy(file.getInputStream(), filePath);

                // Costruisce l'URL pubblico da salvare nel Database
                String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/uploads/")
                        .path(newFilename)
                        .toUriString();

                fileUrls.add(fileDownloadUri);
            }
            return ResponseEntity.ok(fileUrls);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
