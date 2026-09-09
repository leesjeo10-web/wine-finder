package org.example.winefinder.item.dto;

import java.time.LocalDate;
import org.example.winefinder.item.entity.ItemType;
import org.example.winefinder.item.entity.ItemStatus;

/** 등록 요청. status 생략 시 OPEN을 사용합니다. */
public record ItemCreateRequest(
        ItemType type,
        String name,
        String description,
        ItemStatus status,
        String location,
        String storageLocation,
        String imageUrl,
        LocalDate lostFoundAt
) {}
