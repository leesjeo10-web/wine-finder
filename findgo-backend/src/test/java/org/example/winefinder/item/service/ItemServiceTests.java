package org.example.winefinder.item.service;

import java.time.LocalDate;
import java.util.*;
import org.example.winefinder.image.ImageService;
import org.example.winefinder.item.dto.*;
import org.example.winefinder.item.entity.*;
import org.example.winefinder.item.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemServiceTests {
    private final ItemRepository repository = mock(ItemRepository.class);
    private final ImageService images = mock(ImageService.class);
    private final ItemService service = new ItemService(repository, images);
    private final LocalDate date = LocalDate.now();

    private Item item(Long owner) {
        Item item = new Item(ItemType.FOUND, "우산", "검정", ItemStatus.OPEN, "도서관", "학생회실", null, date);
        if (owner != null) item.assignOwner(owner);
        return item;
    }
    @Test
    void createAssignsOwnerAndDefaultsToOpen() {
        when(repository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));
        var response = service.create(new ItemCreateRequest(ItemType.LOST, "우산", "검정", null,
                "도서관", null, null, date), 7L);
        assertEquals(ItemStatus.OPEN, response.status());
        assertEquals(7L, response.userId());
    }
    @Test
    void invalidDateDoesNotPersist() {
        var error = assertThrows(ResponseStatusException.class, () -> service.create(
                new ItemCreateRequest(ItemType.LOST, "우산", "검정", null, "도서관", null, null, date.plusDays(1)), 7L));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(repository, images);
    }
    @Test
    void anotherMemberCannotModifyOrDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(item(7L)));
        var request = new ItemUpdateRequest(ItemType.FOUND, "바뀐 이름", "설명", ItemStatus.OPEN, "학교", null, null, date);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.update(1L, request, 8L)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.status(1L, ItemStatus.RESOLVED, 8L)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.delete(1L, 8L)).getStatusCode().value());
        verify(repository, never()).delete(any(Item.class));
    }
    @Test
    void legacyItemsWithNoOwnerAreReadOnly() {
        when(repository.findById(1L)).thenReturn(Optional.of(item(null)));
        assertEquals("우산", service.findById(1L).name());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.delete(1L, 7L)).getStatusCode().value());
    }
    @Test
    void missingItemReturnsNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.findById(1L)).getStatusCode().value());
    }
    @Test
    void updateCanClearImageAndChangeStatus() {
        Item item = item(7L);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        var response = service.update(1L, new ItemUpdateRequest(ItemType.FOUND, "우산", "검정", ItemStatus.RESOLVED,
                "도서관", null, null, date), 7L);
        assertEquals(ItemStatus.RESOLVED, response.status());
        assertNull(response.storageLocation());
        assertNull(response.imageUrl());
    }
}
