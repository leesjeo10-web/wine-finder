package org.example.winefinder.item.dto;

import java.time.LocalDate;
import org.example.winefinder.item.entity.ItemType;
import org.example.winefinder.item.entity.ItemStatus;
import java.time.LocalDateTime;
import org.example.winefinder.item.entity.Item;

public record ItemResponse(
        Long id,
        ItemType type,
        String name,
        String description,
        ItemStatus status,
        String location,
        String storageLocation,
        String imageUrl,
        LocalDate lostFoundAt,
        LocalDateTime createdAt
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getType(), item.getName(),
                item.getDescription(), item.getStatus(), item.getLocation(),
                item.getStorageLocation(), item.getImageUrl(), item.getLostFoundAt(),
                item.getCreatedAt());
    }
}
