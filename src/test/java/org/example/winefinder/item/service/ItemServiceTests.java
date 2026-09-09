package org.example.winefinder.item.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.example.winefinder.item.dto.ItemCreateRequest;
import org.example.winefinder.item.dto.ItemUpdateRequest;
import org.example.winefinder.item.entity.Item;
import org.example.winefinder.item.entity.ItemStatus;
import org.example.winefinder.item.entity.ItemType;
import org.example.winefinder.item.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemServiceTests {
    private final ItemRepository repository = mock(ItemRepository.class);
    private final ItemService service = new ItemService(repository);
    private final LocalDate date = LocalDate.of(2026, 9, 9);

    @Test
    void searchCombinesKeywordAndTypeAndReturnsDto() {
        Item item = new Item(ItemType.FOUND, "검정 우산", null, ItemStatus.OPEN,
                "도서관", null, null, date);
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        when(repository.search("%우산%", ItemType.FOUND, sort)).thenReturn(List.of(item));

        var responses = service.findAll("  우산  ", ItemType.FOUND);

        assertEquals(1, responses.size());
        assertEquals("검정 우산", responses.getFirst().name());
        assertEquals(ItemType.FOUND, responses.getFirst().type());
        verify(repository).search("%우산%", ItemType.FOUND, sort);
    }

    @Test
    void blankKeywordMeansNoKeywordFilter() {
        service.findAll("   ", null);
        verify(repository).search(null, null, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Test
    void omittedKeywordSupportsTypeOnlyFilter() {
        service.findAll(null, ItemType.LOST);
        verify(repository).search(null, ItemType.LOST, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Test
    void searchTreatsWildcardsAsLiteralCharacters() {
        service.findAll("50%_!", null);
        verify(repository).search("%50!%!_!!%", null, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Test
    void createDefaultsToOpen() {
        when(repository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(new ItemCreateRequest(ItemType.LOST, "우산", null,
                null, "도서관", null, null, date));
        assertEquals(ItemStatus.OPEN, response.status());
        assertEquals("우산", response.name());
        verify(repository).save(any(Item.class));
    }

    @Test
    void invalidRequestDoesNotPersist() {
        var error = assertThrows(ResponseStatusException.class, () -> service.create(
                new ItemCreateRequest(ItemType.LOST, " ", null, null, "도서관", null, null, date)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void missingItemReturnsNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.findById(1L));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void updateReplacesOptionalFieldsAndStatus() {
        Item item = new Item(ItemType.FOUND, "우산", "검정", ItemStatus.OPEN,
                "도서관", "학생회실", "image", date);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        var response = service.update(1L, new ItemUpdateRequest(ItemType.FOUND, "우산", null,
                ItemStatus.RESOLVED, "도서관", null, null, date));
        assertEquals(ItemStatus.RESOLVED, response.status());
        assertNull(response.description());
        assertNull(item.getStorageLocation());
        assertNull(item.getImageUrl());
    }

    @Test
    void deleteMissingItemReturnsNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.delete(1L));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).delete(any(Item.class));
    }
}
