package org.example.winefinder.item.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.winefinder.item.dto.*;
import org.example.winefinder.item.entity.*;
import org.example.winefinder.item.repository.ItemRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {
    private final ItemRepository itemRepository;

    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        validate(request.type(), request.name(), request.location(), request.storageLocation(), request.lostFoundAt());
        Item item = new Item(request.type(), request.name(), request.description(),
                request.status() == null ? ItemStatus.OPEN : request.status(), request.location(),
                request.storageLocation(), request.imageUrl(), request.lostFoundAt());
        return ItemResponse.from(itemRepository.save(item));
    }

    public List<ItemResponse> findAll(String keyword, ItemType type) {
        String pattern = null;
        if (keyword != null && !keyword.isBlank()) {
            // LIKE의 특수 문자도 사용자가 입력한 문자 그대로 검색합니다.
            String escaped = keyword.strip().replace("!", "!!")
                    .replace("%", "!%").replace("_", "!_");
            pattern = "%" + escaped + "%";
        }
        return itemRepository.search(pattern, type, Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(ItemResponse::from).toList();
    }

    public ItemResponse findById(Long id) {
        return ItemResponse.from(getItem(id));
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest request) {
        validate(request.type(), request.name(), request.location(), request.storageLocation(), request.lostFoundAt());
        if (request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        Item item = getItem(id);
        item.update(request.type(), request.name(), request.description(), request.status(),
                request.location(), request.storageLocation(), request.imageUrl(), request.lostFoundAt());
        return ItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        itemRepository.delete(getItem(id));
    }

    private Item getItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id));
    }

    // 기존 의존성을 유지하며 요청을 검증합니다.
    private void validate(ItemType type, String name, String location, String storageLocation, LocalDate date) {
        if (type == null || date == null || name == null || name.isBlank()
                || location == null || location.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type, name, location and lostFoundAt are required");
        }
        if (name.length() > 255 || location.length() > 255
                || (storageLocation != null && storageLocation.length() > 255)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name, location and storageLocation must not exceed 255 characters");
        }
    }
}
