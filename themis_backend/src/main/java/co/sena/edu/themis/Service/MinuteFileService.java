package co.sena.edu.themis.Service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MinuteFileService {

    private static final String DIR_NAME = "minutes-docx";

    public Path getStorageDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir"), DIR_NAME);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    public String saveDocx(byte[] bytes, String baseName) throws IOException {
        if (baseName == null || baseName.isBlank()) {
            baseName = "acta";
        }
        String safeBase = sanitize(baseName);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = safeBase + "-" + timestamp + ".docx";
        Path path = getStorageDir().resolve(filename);
        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return filename;
    }

    public Resource loadAsResource(String filename) throws MalformedURLException, IOException {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
        Path file = getStorageDir().resolve(filename);
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Archivo no encontrado");
        }
        return new UrlResource(file.toUri());
    }

    private String sanitize(String input) {
        String n = Normalizer.normalize(input, Normalizer.Form.NFD);
        n = n.replaceAll("[^A-Za-z0-9_-]", "-");
        n = n.replaceAll("-+", "-");
        if (n.isBlank()) return "acta";
        return n;
    }
}

