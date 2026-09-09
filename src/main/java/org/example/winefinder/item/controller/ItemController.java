package org.example.winefinder.item.controller;

import java.net.URI; //
import java.util.List;
import lombok.RequiredArgsConstructor; // 값이 없는 생성자 ??
import org.example.winefinder.item.dto.ItemCreateRequest;
import org.example.winefinder.item.dto.ItemUpdateRequest;
import org.example.winefinder.item.dto.ItemResponse;
import org.example.winefinder.item.entity.ItemType;
import org.example.winefinder.item.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponse> create(@RequestBody ItemCreateRequest request) {
        ItemResponse response = itemService.create(request);
        return ResponseEntity.created(URI.create("/api/items/" + response.id())).body(response);
    }

    @GetMapping
    public List<ItemResponse> findAll(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) ItemType type) {
        return itemService.findAll(keyword, type);
    }

    @GetMapping("/{id}")
    public ItemResponse findById(@PathVariable Long id) {
        return itemService.findById(id);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @RequestBody ItemUpdateRequest request) {
        return itemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
