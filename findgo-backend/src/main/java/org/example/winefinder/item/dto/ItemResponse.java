package org.example.winefinder.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import org.example.winefinder.item.entity.ItemType;
import org.example.winefinder.item.entity.ItemStatus;
import java.time.LocalDateTime;
import org.example.winefinder.item.entity.Item;

@Schema(description = "물품 상세 정보")
public record ItemResponse(
        @Schema(description = "물품 ID. 서버에서 생성합니다.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,
        @Schema(description = "작성자 ID. 서버가 로그인 사용자로 지정합니다. 기존 데이터는 없을 수 있습니다.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long userId,
        @Schema(description = "물품 유형: LOST=분실물, FOUND=습득물", example = "FOUND")
        ItemType type,
        @Schema(description = "물품 이름. 공백만 입력할 수 없으며 최대 100자입니다.", example = "검정 우산")
        String name,
        @Schema(description = "물품 특징과 설명. 공백만 입력할 수 없으며 최대 2000자입니다.", example = "손잡이에 파란 스티커가 있습니다.")
        String description,
        @Schema(description = "진행 상태: OPEN=찾는 중, RESOLVED=해결 완료", example = "OPEN")
        ItemStatus status,
        @Schema(description = "분실 또는 습득 장소. 최대 100자입니다.", example = "도서관")
        String location,
        @Schema(description = "습득물 보관 장소. 최대 100자이며 분실물 등록 시 비웁니다.", example = "학생회실")
        String storageLocation,
        @Schema(description = "본인이 업로드한 사진 URL 한 개. 사진이 없으면 null 또는 빈 문자열입니다.", example = "/api/images/123e4567-e89b-12d3-a456-426614174000.jpg")
        String imageUrl,
        @Schema(description = "분실 또는 습득 날짜. 미래 날짜는 허용하지 않습니다.", example = "2026-10-01")
        LocalDate lostFoundAt,
        @Schema(description = "서버에서 생성한 등록 일시", example = "2026-10-01T09:00:00", accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime createdAt
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getUserId(), item.getType(), item.getName(),
                item.getDescription(), item.getStatus(), item.getLocation(),
                item.getStorageLocation(), item.getImageUrl(), item.getLostFoundAt(),
                item.getCreatedAt());
    }
}
