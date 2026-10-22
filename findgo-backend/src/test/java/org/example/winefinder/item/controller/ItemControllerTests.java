package org.example.winefinder.item.controller;

import org.example.winefinder.auth.AuthService;
import org.example.winefinder.item.entity.ItemType;
import org.example.winefinder.item.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ItemControllerTests {
    private final ItemService service = mock(ItemService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ItemController(service, mock(AuthService.class))).build();

    @Test
    void acceptsCombinedSearchParameters() throws Exception {
        when(service.findAll("우산", ItemType.FOUND, null, "latest", 0, 20, null)).thenReturn(Page.empty());
        mvc.perform(get("/api/items").param("keyword", "우산").param("type", "FOUND"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        verify(service).findAll("우산", ItemType.FOUND, null, "latest", 0, 20, null);
    }
    @Test
    void invalidTypeReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/items").param("type", "UNKNOWN")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
