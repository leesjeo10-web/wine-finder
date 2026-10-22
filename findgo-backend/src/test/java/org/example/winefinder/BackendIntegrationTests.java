package org.example.winefinder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Map;
import javax.imageio.ImageIO;
import org.example.winefinder.auth.MemberRepository;
import org.example.winefinder.image.ImageRepository;
import org.example.winefinder.item.repository.ItemRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class BackendIntegrationTests {
    static final Path UPLOADS;
    static {
        try { UPLOADS = Files.createTempDirectory("findgo-test-images-"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) { registry.add("app.upload-dir", () -> UPLOADS.toString()); }
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper json;
    @Autowired MemberRepository members;
    @Autowired ItemRepository items;
    @Autowired ImageRepository images;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        images.deleteAll(); items.deleteAll(); members.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterAll
    static void cleanup() throws Exception {
        try (var paths = Files.walk(UPLOADS)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
    private AbstractMockHttpServletRequestBuilder<?> secured(AbstractMockHttpServletRequestBuilder<?> request, MockHttpSession session) throws Exception {
        var csrf = json.readTree(mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        return request.session(session).header(csrf.get("headerName").asText(), csrf.get("token").asText());
    }
    private MockHttpSession signupAndLogin(String email) throws Exception {
        var session = new MockHttpSession();
        mvc.perform(secured(post("/api/auth/signup"), session).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "name", "테스트 학생", "password", "password123!"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(secured(post("/api/auth/login"), session).param("email", email).param("password", "password123!"))
                .andExpect(status().isNoContent());
        return session;
    }
    private String itemBody(String type, String name, String imageUrl) {
        return json.writeValueAsString(Map.of("type", type, "name", name, "description", "물품 특징", "location", "도서관",
                "storageLocation", "학생회실", "lostFoundAt", LocalDate.now().toString(), "status", "OPEN", "imageUrl", imageUrl));
    }
    private long createItem(MockHttpSession session, String type, String name, String imageUrl) throws Exception {
        var response = mvc.perform(secured(post("/api/items"), session).contentType(MediaType.APPLICATION_JSON)
                        .content(itemBody(type, name, imageUrl)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").isNumber())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test
    void signupLoginMeAndLogoutUseRealAuthentication() throws Exception {
        var session = signupAndLogin("Alice@school.kr");
        var member = members.findByEmail("alice@school.kr").orElseThrow();
        assertNotEquals("password123!", member.getPasswordHash());
        assertTrue(member.getPasswordHash().startsWith("{pbkdf2"));
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@school.kr"));
        mvc.perform(secured(post("/api/auth/logout"), session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test
    void rejectsDuplicateInvalidSignupAndWrongPassword() throws Exception {
        signupAndLogin("alice@school.kr");
        var s = new MockHttpSession();
        mvc.perform(secured(post("/api/auth/signup"), s).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "ALICE@school.kr", "name", "학생", "password", "password123!"))))
                .andExpect(status().isConflict());
        mvc.perform(secured(post("/api/auth/signup"), s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\",\"name\":\"\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(secured(post("/api/auth/login"), s).param("email", "alice@school.kr").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void unauthenticatedWritesAndMissingCsrfAreRejected() throws Exception {
        mvc.perform(get("/api/items")).andExpect(status().isOk());
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(itemBody("LOST", "우산", "")))
                .andExpect(status().isForbidden());
        mvc.perform(secured(post("/api/items"), new MockHttpSession()).contentType(MediaType.APPLICATION_JSON)
                .content(itemBody("LOST", "우산", ""))).andExpect(status().isUnauthorized());
        var s = signupAndLogin("owner@school.kr");
        mvc.perform(post("/api/items").session(s).contentType(MediaType.APPLICATION_JSON).content(itemBody("LOST", "우산", "")))
                .andExpect(status().isForbidden());
    }
    @Test
    void ownerCanManageItemsButOtherMemberCannot() throws Exception {
        var owner = signupAndLogin("owner@school.kr");
        var other = signupAndLogin("other@school.kr");
        long id = createItem(owner, "LOST", "우산", "");
        mvc.perform(get("/api/me/items").session(owner)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/me/items").session(other)).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(secured(put("/api/items/" + id), other).contentType(MediaType.APPLICATION_JSON)
                .content(itemBody("FOUND", "변경", ""))).andExpect(status().isForbidden());
        mvc.perform(secured(delete("/api/items/" + id), other)).andExpect(status().isForbidden());
        mvc.perform(secured(patch("/api/items/" + id + "/status"), other).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\"}")).andExpect(status().isForbidden());
        mvc.perform(secured(put("/api/items/" + id), owner).contentType(MediaType.APPLICATION_JSON)
                .content(itemBody("FOUND", "검정 우산", ""))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("검정 우산"));
        mvc.perform(secured(patch("/api/items/" + id + "/status"), owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(secured(delete("/api/items/" + id), owner)).andExpect(status().isNoContent());
        mvc.perform(get("/api/items/" + id)).andExpect(status().isNotFound());
    }
    @Test
    void searchFiltersSortingStatisticsAndRecommendations() throws Exception {
        var s = signupAndLogin("owner@school.kr");
        long first = createItem(s, "LOST", "검정 우산", "");
        long second = createItem(s, "FOUND", "검정 우산 50%_!", "");
        createItem(s, "FOUND", "파랑 가방", "");
        mvc.perform(get("/api/items").param("keyword", "50%_!").param("type", "FOUND").param("status", "OPEN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(second));
        mvc.perform(get("/api/items").param("sort", "oldest").param("size", "1"))
                .andExpect(jsonPath("$.content[0].id").value(first)).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/items").param("keyword", "도서관")).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/items/similar").param("name", "검정 우산").param("type", "LOST"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(second)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/home")).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.lost").value(1))
                .andExpect(jsonPath("$.found").value(2));
        mvc.perform(get("/api/items").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/items").param("sort", "invalid")).andExpect(status().isBadRequest());
    }
    @Test
    void uploadReencodesImageAndChecksOwnership() throws Exception {
        var owner = signupAndLogin("owner@school.kr");
        var other = signupAndLogin("other@school.kr");
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var request = multipart("/api/images").file(new MockMultipartFile("file", "../photo.png", "image/png", bytes.toByteArray()));
        var response = mvc.perform(secured(request, owner)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String url = json.readTree(response).get("imageUrl").asText();
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG));
        createItem(owner, "FOUND", "우산", url);
        mvc.perform(secured(post("/api/items"), other).contentType(MediaType.APPLICATION_JSON).content(itemBody("LOST", "우산", url)))
                .andExpect(status().isForbidden());
        mvc.perform(secured(multipart("/api/images").file(new MockMultipartFile("file", "fake.png", "image/png", "invalid".getBytes())), owner))
                .andExpect(status().isBadRequest());
        mvc.perform(secured(post("/api/items"), owner).contentType(MediaType.APPLICATION_JSON)
                .content(itemBody("LOST", "우산", "https://example.com/pic.png"))).andExpect(status().isBadRequest());
    }
    @Test
    void corsAllowsOnlyConfiguredFrontend() throws Exception {
        mvc.perform(options("/api/items").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "X-CSRF-TOKEN,Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/items").header("Origin", "https://unknown.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
    @Test
    void openApiIncludesKoreanOperationAndFieldDescriptions() throws Exception {
        var response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var spec = json.readTree(response);
        assertEquals("찾GO API", spec.at("/info/title").asText());
        assertEquals("물품 등록", spec.at("/paths/~1api~1items/post/summary").asText());
        assertEquals("회원가입", spec.at("/paths/~1api~1auth~1signup/post/summary").asText());
        assertFalse(spec.at("/components/schemas/ItemCreateRequest/properties/name/description").asText().isBlank());
        assertEquals("검정 우산", spec.at("/components/schemas/ItemCreateRequest/properties/name/example").asText());
        assertTrue(spec.at("/components/schemas/ItemCreateRequest/required").toString().contains("lostFoundAt"));
        assertTrue(spec.at("/components/schemas/ImageUploadResponse/properties/imageUrl/description").asText().contains("사진"));
    }

}
