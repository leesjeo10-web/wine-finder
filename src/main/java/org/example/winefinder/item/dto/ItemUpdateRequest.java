package org.example.winefinder.item.dto;

import java.time.LocalDate;
import org.example.winefinder.item.entity.ItemType;
import org.example.winefinder.item.entity.ItemStatus;

/** 전체 수정 요청. 선택 필드의 null은 기존 값을 지웁니다. */
public record ItemUpdateRequest(
        ItemType type,
        String name,
        String description,
        ItemStatus status,
        String location,
        String storageLocation,
        String imageUrl,
        LocalDate lostFoundAt
) {}
