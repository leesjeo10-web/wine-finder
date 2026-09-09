package org.example.winefinder.item.controller;

import org.example.winefinder.item.entity.ItemType;
import org.example.winefinder.item.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ItemControllerTests {
    private final ItemService service = mock(ItemService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ItemController(service)).build();

    @Test
    void acceptsCombinedSearchParameters() throws Exception {
        mvc.perform(get("/api/items").param("keyword", "우산").param("type", "FOUND"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(service).findAll("우산", ItemType.FOUND);
    }

    @Test
    void acceptsUnfilteredList() throws Exception {
        mvc.perform(get("/api/items"))
                .andExpect(status().isOk());
        verify(service).findAll(null, null);
    }

    @Test
    void invalidTypeReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/items").param("type", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
