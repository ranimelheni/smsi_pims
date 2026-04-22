package com.pims.plateform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class FileUploadService {

    @Value("${app.upload.base-dir:./uploads}")
    private String baseDir;

    // ── Whitelist stricte par type d'upload ───────────────────────────────
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS = Map.of(
        "cv",       Set.of("pdf", "doc", "docx"),
        "certif",   Set.of("pdf", "jpg", "jpeg", "png"),
        "document", Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")
    );

    private static final Map<String, Set<String>> ALLOWED_MIMES = Map.of(
        "cv", Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ),
        "certif", Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
        ),
        "document", Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain"
        )
    );

    // ── Magic bytes (signatures fichiers) — anti-RCE ──────────────────────
    private static final Map<String, byte[]> MAGIC_BYTES = new LinkedHashMap<>();
    static {
        MAGIC_BYTES.put("pdf",  new byte[]{0x25, 0x50, 0x44, 0x46});           // %PDF
        MAGIC_BYTES.put("docx", new byte[]{0x50, 0x4B, 0x03, 0x04});           // PK (ZIP)
        MAGIC_BYTES.put("doc",  new byte[]{(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0}); // OLE
        MAGIC_BYTES.put("xlsx", new byte[]{0x50, 0x4B, 0x03, 0x04});           // PK (ZIP)
        MAGIC_BYTES.put("xls",  new byte[]{(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0});
        MAGIC_BYTES.put("png",  new byte[]{(byte)0x89, 0x50, 0x4E, 0x47});     // PNG
        MAGIC_BYTES.put("jpg",  new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF});  // JPEG
        MAGIC_BYTES.put("jpeg", new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF});
    }

    // Taille max : 10 Mo
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;

    // ── Upload principal ──────────────────────────────────────────────────
    public UploadResult upload(MultipartFile file, String typeUpload, Long orgId)
            throws IOException {

        // 1. Vérification taille
        if (file.isEmpty())
            throw new SecurityException("Fichier vide");
        if (file.getSize() > MAX_SIZE_BYTES)
            throw new SecurityException("Fichier trop volumineux (max 10 Mo)");

        // 2. Extension
        String nomOriginal = sanitizeFileName(file.getOriginalFilename());
        String extension   = getExtension(nomOriginal).toLowerCase();

        Set<String> extAutorisees = ALLOWED_EXTENSIONS.get(typeUpload);
        if (extAutorisees == null || !extAutorisees.contains(extension))
            throw new SecurityException("Extension non autorisée : " + extension
                + ". Autorisées : " + extAutorisees);

        // 3. MIME type déclaré
        String mimeType = file.getContentType();
        Set<String> mimesAutorises = ALLOWED_MIMES.get(typeUpload);
        if (mimesAutorises == null || !mimesAutorises.contains(mimeType))
            throw new SecurityException("Type MIME non autorisé : " + mimeType);

        // 4. Vérification magic bytes (contenu réel)
        byte[] headerBytes = new byte[8];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(headerBytes);
            if (read < 4) throw new SecurityException("Fichier trop court ou corrompu");
        }
        if (!checkMagicBytes(extension, headerBytes))
            throw new SecurityException("Contenu du fichier incohérent avec l'extension déclarée");

        // 5. Nom de stockage sécurisé (UUID — jamais le nom original)
        String nomStockage = UUID.randomUUID() + "." + extension;

        // 6. Dossier de destination
        Path dossier = Paths.get(baseDir, typeUpload, orgId.toString(),
            String.valueOf(LocalDate.now().getYear()));
        Files.createDirectories(dossier);

        Path destination = dossier.resolve(nomStockage);
        file.transferTo(destination.toFile());

        log.info("Upload OK : type={} org={} fichier={} taille={}",
            typeUpload, orgId, nomStockage, file.getSize());

        return new UploadResult(
            nomOriginal,
            destination.toString(),
            nomStockage,
            mimeType,
            extension,
            file.getSize()
        );
    }

    // ── Suppression fichier ───────────────────────────────────────────────
    public void delete(String chemin) {
        if (chemin == null || chemin.isBlank()) return;
        try {
            Path p = Paths.get(chemin);
            // Sécurité : vérifier que le chemin est dans baseDir
            if (!p.toAbsolutePath().startsWith(Paths.get(baseDir).toAbsolutePath())) {
                log.warn("Tentative de suppression hors du répertoire autorisé : {}", chemin);
                return;
            }
            Files.deleteIfExists(p);
            log.info("Fichier supprimé : {}", chemin);
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier : {}", chemin);
        }
    }

    // ── Helpers privés ────────────────────────────────────────────────────
    private String sanitizeFileName(String nom) {
        if (nom == null) return "fichier";
        // Retirer chemin, conserver uniquement le nom
        nom = Paths.get(nom).getFileName().toString();
        // Retirer tout caractère dangereux
        nom = nom.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        // Limiter la longueur
        if (nom.length() > 200) nom = nom.substring(0, 200);
        return nom;
    }

    private String getExtension(String nom) {
        int dot = nom.lastIndexOf('.');
        if (dot < 0 || dot == nom.length() - 1) return "";
        return nom.substring(dot + 1);
    }

    private boolean checkMagicBytes(String extension, byte[] header) {
        byte[] expected = MAGIC_BYTES.get(extension);
        if (expected == null) return true; // Pas de signature connue → laisser passer
        if (header.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if (header[i] != expected[i]) return false;
        }
        return true;
    }

    // ── DTO résultat ──────────────────────────────────────────────────────
    public record UploadResult(
        String nomOriginal,
        String cheminComplet,
        String nomStockage,
        String typeMime,
        String extension,
        long taille
    ) {}
}