package org.example.winefinder.item.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.example.winefinder.auth.AuthService;
import org.example.winefinder.item.dto.ItemResponse;
import org.example.winefinder.item.entity.*;
import org.example.winefinder.item.service.ItemService;
import org.example.winefinder.common.PageResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "홈·마이페이지", description = "홈 통계 및 내 등록 물품")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MyPageController {
    private final ItemService items;
    private final AuthService auth;

    @Operation(summary = "내가 등록한 물품 조회", description = "로그인 세션이 필요합니다. 서버가 작성자 ID를 로그인 사용자로 제한하며 검색·필터·정렬·페이지 조회를 지원합니다.")
    @GetMapping("/me/items")
    public PageResponse<ItemResponse> myItems(@Parameter(hidden = true) Authentication user, @RequestParam(required = false) @Parameter(description = "이름·설명·장소·보관 장소의 부분 문자열 검색. 최대 100자", example = "우산") String keyword,
            @RequestParam(required = false) @Parameter(description = "물품 유형. 목록에서는 생략 시 전체, 추천에서는 필수", example = "LOST") ItemType type, @RequestParam(required = false) @Parameter(description = "해결 상태. 생략하면 전체", example = "OPEN") ItemStatus status,
            @RequestParam(defaultValue = "latest") @Parameter(description = "정렬: latest=최신순, oldest=오래된 순", example = "latest") String sort, @RequestParam(defaultValue = "0") @Parameter(description = "페이지 번호. 0부터 시작", example = "0") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "페이지당 건수. 1~100", example = "20") int size) {
        return PageResponse.from(items.findAll(keyword, type, status, sort, page, size, auth.current(user).getId()));
    }
    @Operation(summary = "홈 통계 및 최근 물품 조회", description = "전체·분실·습득·해결 건수와 최근 분실물·습득물을 각각 최대 3건 반환합니다. 로그인 없이 조회합니다.")
    @GetMapping("/home")
    public ItemService.HomeResponse home() { return items.home(); }
}
