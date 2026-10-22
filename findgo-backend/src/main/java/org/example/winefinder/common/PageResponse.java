package org.example.winefinder.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** Spring 내부 Page 구현과 무관하게 프론트에 고정된 응답을 제공합니다. */
@Schema(description = "페이지 조회 결과")
public record PageResponse<T>(
        @Schema(description = "현재 페이지의 물품 목록") List<T> content,
        @Schema(description = "현재 페이지 번호. 0부터 시작", example = "0") int page,
        @Schema(description = "페이지당 건수", example = "20") int size,
        @Schema(description = "조건에 맞는 전체 물품 수", example = "42") long totalElements,
        @Schema(description = "전체 페이지 수", example = "3") int totalPages,
        @Schema(description = "첫 페이지 여부", example = "true") boolean first,
        @Schema(description = "마지막 페이지 여부", example = "false") boolean last) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
