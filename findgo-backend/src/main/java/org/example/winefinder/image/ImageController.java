package org.example.winefinder.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.example.winefinder.auth.AuthService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사진", description = "물품당 사진 한 장 첨부를 위한 업로드 및 조회")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService images;
    private final AuthService auth;

    @Schema(description = "사진 업로드 결과")
    public record ImageUploadResponse(
            @Schema(description = "물품 등록·수정 시 imageUrl에 넣을 사진 경로", example = "/api/images/123e4567-e89b-12d3-a456-426614174000.jpg") String imageUrl) {}

    @Operation(summary = "물품 사진 업로드", description = "로그인과 CSRF 토큰이 필요합니다. file 필드에 JPG/PNG 사진 한 장을 전송하세요. 5MB·2천만 화소 이하를 허용하고 긴 변 최대 1600px JPEG로 저장합니다. WEBP는 프론트에서 JPEG로 변환하세요.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUploadResponse upload(@RequestParam @Parameter(description = "5MB 이하의 JPG 또는 PNG 사진 한 장") MultipartFile file, @Parameter(hidden = true) Authentication user) {
        return new ImageUploadResponse(images.upload(file, auth.current(user).getId()));
    }
    @Operation(summary = "물품 사진 조회", description = "업로드 시 반환된 imageUrl로 공개 JPEG 사진을 조회합니다. 존재하지 않는 파일은 404입니다.")
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> read(@PathVariable @Parameter(description = "업로드 응답 URL에 포함된 JPEG 파일명", example = "123e4567-e89b-12d3-a456-426614174000.jpg") String filename) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff").body(images.read(filename));
    }
}
