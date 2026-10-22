package org.example.winefinder.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository images;
    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;
    private static final String PREFIX = "/api/images/";

    @Transactional
    public String upload(MultipartFile file, Long userId) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 0바이트 초과, 5MB 이하로 올려 주세요.");
        }
        Path target = null;
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            BufferedImage source;
            try {
                reader.setInput(input);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("jpeg") && !format.equalsIgnoreCase("png")) throw invalid();
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 20_000_000) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진 해상도는 2천만 화소 이하로 올려 주세요.");
                }
                source = reader.read(0);
            } finally { reader.dispose(); }
            double scale = Math.min(1, 1600.0 / Math.max(source.getWidth(), source.getHeight()));
            var normalized = new BufferedImage(Math.max(1, (int) (source.getWidth() * scale)),
                    Math.max(1, (int) (source.getHeight() * scale)), BufferedImage.TYPE_INT_RGB);
            var graphics = normalized.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, normalized.getWidth(), normalized.getHeight());
                graphics.drawImage(source, 0, 0, normalized.getWidth(), normalized.getHeight(), null);
            } finally { graphics.dispose(); }
            String name = UUID.randomUUID() + ".jpg";
            Path directory = Path.of(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            target = directory.resolve(name);
            try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                if (!ImageIO.write(normalized, "jpeg", output)) throw new IOException("JPEG encoder unavailable");
            }
            Path savedFile = target;
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                try { Files.deleteIfExists(savedFile); } catch (IOException ignored) { }
                            }
                        }
                    });
            images.saveAndFlush(new ItemImage(name, userId));
            return PREFIX + name;
        } catch (ResponseStatusException error) {
            throw error;
        } catch (IOException error) {
            if (target != null) {
                try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진을 읽거나 저장하지 못했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public void requireOwned(String url, Long userId) {
        if (url == null || url.isBlank()) return;
        if (!url.matches("/api/images/[0-9a-f-]{36}\\.jpg")) throw invalid();
        var image = images.findById(url.substring(PREFIX.length())).orElseThrow(ImageService::invalid);
        if (!image.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 올린 사진만 첨부할 수 있습니다.");
        }
    }

    public Resource read(String filename) {
        if (!filename.matches("[0-9a-f-]{36}\\.jpg")) throw notFound();
        var file = new FileSystemResource(Path.of(uploadDir).toAbsolutePath().normalize().resolve(filename));
        if (!file.isReadable()) throw notFound();
        return file;
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPG 또는 PNG 사진을 업로드한 뒤 반환된 imageUrl을 사용하세요.");
    }
    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "사진을 찾을 수 없습니다.");
    }
}
