package org.example.winefinder.item.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.winefinder.auth.AuthService;
import org.example.winefinder.item.dto.*;
import org.example.winefinder.item.entity.*;
import org.example.winefinder.item.service.ItemService;
import org.example.winefinder.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "물품", description = "분실물·습득물 조회 및 작성자 관리")
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService items;
    private final AuthService auth;

    @Operation(summary = "물품 등록", description = "로그인과 CSRF 토큰이 필요합니다. 작성자는 서버에서 지정하며 사진은 먼저 업로드한 imageUrl을 사용합니다. 성공 시 201입니다.")
    @PostMapping
    public ResponseEntity<ItemResponse> create(@RequestBody ItemCreateRequest request, @Parameter(hidden = true) Authentication user) {
        var response = items.create(request, auth.current(user).getId());
        return ResponseEntity.created(URI.create("/api/items/" + response.id())).body(response);
    }
    @Operation(summary = "물품 검색 및 목록 조회", description = "로그인 없이 조회할 수 있습니다. 검색어·유형·해결 상태를 함께 필터링하고 페이지와 정렬을 지정할 수 있습니다.")
    @GetMapping
    public PageResponse<ItemResponse> findAll(@RequestParam(required = false) @Parameter(description = "이름·설명·장소·보관 장소의 부분 문자열 검색. 최대 100자", example = "우산") String keyword,
            @RequestParam(required = false) @Parameter(description = "물품 유형. 목록에서는 생략 시 전체, 추천에서는 필수", example = "LOST") ItemType type, @RequestParam(required = false) @Parameter(description = "해결 상태. 생략하면 전체", example = "OPEN") ItemStatus status,
            @RequestParam(defaultValue = "latest") @Parameter(description = "정렬: latest=최신순, oldest=오래된 순", example = "latest") String sort, @RequestParam(defaultValue = "0") @Parameter(description = "페이지 번호. 0부터 시작", example = "0") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "페이지당 건수. 1~100", example = "20") int size) {
        return PageResponse.from(items.findAll(keyword, type, status, sort, page, size, null));
    }
    @Operation(summary = "비슷한 물품 추천", description = "반대 유형의 미해결 물품 중 최근 100건에서 이름·설명의 키워드를 비교해 최대 3건을 반환합니다. AI 이미지 분석은 사용하지 않습니다.")
    @GetMapping("/similar")
    public List<ItemResponse> similar(@RequestParam @Parameter(description = "추천할 물품 이름. 1~100자", example = "검정 우산") String name, @RequestParam @Parameter(description = "물품 유형. 목록에서는 생략 시 전체, 추천에서는 필수", example = "LOST") ItemType type,
                                     @RequestParam(required = false) @Parameter(description = "추천 결과에서 제외할 물품 ID. 수정 중인 글에 사용", example = "1") Long excludeId) {
        return items.similar(name, type, excludeId);
    }
    @Operation(summary = "물품 상세 조회", description = "로그인 없이 조회할 수 있습니다. 존재하지 않는 ID는 404입니다.")
    @GetMapping("/{id}")
    public ItemResponse findById(@PathVariable @Parameter(description = "물품 ID", example = "1") Long id) { return items.findById(id); }
    @Operation(summary = "물품 전체 수정", description = "로그인한 작성자만 수정할 수 있습니다. CSRF 토큰이 필요하며 선택 필드를 비우면 기존 값도 제거합니다. 다른 작성자의 글은 403입니다.")
    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable @Parameter(description = "물품 ID", example = "1") Long id, @RequestBody ItemUpdateRequest request, @Parameter(hidden = true) Authentication user) {
        return items.update(id, request, auth.current(user).getId());
    }
    public record StatusRequest(
            @Schema(description = "OPEN=찾는 중, RESOLVED=해결 완료", example = "RESOLVED", requiredMode = Schema.RequiredMode.REQUIRED) ItemStatus status) {}
    @Operation(summary = "해결 상태 변경", description = "로그인한 작성자가 OPEN 또는 RESOLVED로 변경합니다. CSRF 토큰이 필요합니다.")
    @PatchMapping("/{id}/status")
    public ItemResponse status(@PathVariable @Parameter(description = "물품 ID", example = "1") Long id, @RequestBody StatusRequest request, @Parameter(hidden = true) Authentication user) {
        return items.status(id, request.status(), auth.current(user).getId());
    }
    @Operation(summary = "물품 삭제", description = "로그인한 작성자만 삭제할 수 있습니다. CSRF 토큰이 필요하며 성공하면 본문 없이 204를 반환합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Parameter(description = "물품 ID", example = "1") Long id, @Parameter(hidden = true) Authentication user) {
        items.delete(id, auth.current(user).getId());
        return ResponseEntity.noContent().build();
    }
}
