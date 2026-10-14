package com.pashumandi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pashumandi.dto.animal.AnimalCreateRequest;
import com.pashumandi.dto.animal.AnimalStatusUpdateRequest;
import com.pashumandi.dto.animal.AnimalUpdateRequest;
import com.pashumandi.dto.image.CloudinaryUploadResult;
import com.pashumandi.entity.Animal;
import com.pashumandi.entity.AnimalImage;
import com.pashumandi.entity.User;
import com.pashumandi.enums.AnimalGender;
import com.pashumandi.enums.AnimalStatus;
import com.pashumandi.enums.AnimalType;
import com.pashumandi.enums.UserRole;
import com.pashumandi.enums.UserStatus;
import com.pashumandi.repository.AnimalImageRepository;
import com.pashumandi.repository.AnimalRepository;
import com.pashumandi.repository.UserRepository;
import com.pashumandi.security.JwtService;
import com.pashumandi.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AnimalListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private AnimalImageRepository animalImageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ImageStorageService imageStorageService;

    private User sellerUser;
    private User otherSellerUser;
    private User adminUser;
    private String sellerToken;
    private String otherSellerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        animalImageRepository.deleteAll();
        animalRepository.deleteAll();
        userRepository.deleteAll();

        sellerUser = User.builder()
                .firstName("Ramesh")
                .lastName("Yadav")
                .phone("9876511111")
                .email("ramesh@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.SELLER)
                .status(UserStatus.ACTIVE)
                .state("Uttar Pradesh")
                .district("Varanasi")
                .village("Shivpur")
                .pincode("221003")
                .build();
        sellerUser = userRepository.save(sellerUser);
        sellerToken = jwtService.generateAccessToken(sellerUser);

        otherSellerUser = User.builder()
                .firstName("Suresh")
                .lastName("Verma")
                .phone("9876522222")
                .email("suresh@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.SELLER)
                .status(UserStatus.ACTIVE)
                .state("Bihar")
                .district("Patna")
                .village("Danapur")
                .pincode("801503")
                .build();
        otherSellerUser = userRepository.save(otherSellerUser);
        otherSellerToken = jwtService.generateAccessToken(otherSellerUser);

        adminUser = User.builder()
                .firstName("Admin")
                .lastName("System")
                .phone("9876599999")
                .email("admin@pashumandi.com")
                .password(passwordEncoder.encode("AdminPass123"))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .state("Delhi")
                .district("New Delhi")
                .village("Connaught Place")
                .pincode("110001")
                .build();
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);
    }

    private byte[] createTestPngBytes() {
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("1. Create valid animal listing as draft by default")
    void shouldCreateAnimalListingAsDraftByDefault() throws Exception {
        AnimalCreateRequest request = AnimalCreateRequest.builder()
                .title("Healthy Sahiwal Cow")
                .description("Purebred Sahiwal cow, high milk yield, 2nd lactation.")
                .animalType(AnimalType.COW)
                .breed("Sahiwal")
                .ageMonths(42)
                .gender(AnimalGender.FEMALE)
                .price(new BigDecimal("75000.00"))
                .negotiable(true)
                .weightKg(new BigDecimal("420.00"))
                .milkYieldLitresPerDay(new BigDecimal("14.00"))
                .lactationStatus("2nd Lactation")
                .pregnant(false)
                .vaccinated(true)
                .healthDescription("Dewormed and healthy.")
                .state("Uttar Pradesh")
                .district("Varanasi")
                .village("Shivpur")
                .pincode("221003")
                .latitude(new BigDecimal("25.3500000"))
                .longitude(new BigDecimal("82.9500000"))
                .build();

        mockMvc.perform(post("/api/v1/animals")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.title", is("Healthy Sahiwal Cow")))
                .andExpect(jsonPath("$.data.animalType", is("COW")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.seller.id", is(sellerUser.getId().intValue())))
                .andExpect(jsonPath("$.data.seller.phone", is(sellerUser.getPhone())));

        List<Animal> allAnimals = animalRepository.findAll();
        assertEquals(1, allAnimals.size());
        Animal created = allAnimals.get(0);
        assertEquals(sellerUser.getId(), created.getUser().getId());
        assertEquals(AnimalStatus.DRAFT, created.getStatus());
        assertEquals(0, new BigDecimal("75000.00").compareTo(created.getPrice()));
    }

    @Test
    @DisplayName("2. Create valid animal listing as published when explicitly requested")
    void shouldCreateAnimalListingAsPublished() throws Exception {
        AnimalCreateRequest request = AnimalCreateRequest.builder()
                .title("Murrah Buffalo")
                .description("Top quality Murrah buffalo.")
                .animalType(AnimalType.BUFFALO)
                .breed("Murrah")
                .ageMonths(36)
                .gender(AnimalGender.FEMALE)
                .price(new BigDecimal("85000.00"))
                .negotiable(false)
                .status(AnimalStatus.PUBLISHED)
                .state("Haryana")
                .district("Rohtak")
                .pincode("124001")
                .build();

        mockMvc.perform(post("/api/v1/animals")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")));
    }

    @Test
    @DisplayName("3. Reject invalid listing data (blank title, negative price, missing gender)")
    void shouldRejectInvalidListingData() throws Exception {
        AnimalCreateRequest request = AnimalCreateRequest.builder()
                .title("") // blank
                .animalType(AnimalType.COW)
                .price(new BigDecimal("-100.00")) // negative
                .gender(null) // missing
                .build();

        mockMvc.perform(post("/api/v1/animals")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.validationErrors.title", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.price", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.gender", notNullValue()));
    }

    @Test
    @DisplayName("4. Seller ID is resolved strictly from authentication principal")
    void shouldResolveSellerStrictlyFromPrincipal() throws Exception {
        String jsonPayload = """
                {
                    "title": "Desi Cow",
                    "animalType": "COW",
                    "gender": "FEMALE",
                    "ageMonths": 24,
                    "price": 30000.00,
                    "sellerId": 99999,
                    "userId": 88888
                }
                """;

        mockMvc.perform(post("/api/v1/animals")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seller.id", is(sellerUser.getId().intValue())));
    }

    @Test
    @DisplayName("5. Public browsing returns only published listings")
    void shouldReturnOnlyPublishedListingsForPublicBrowsing() throws Exception {
        // Create 1 published and 1 draft
        Animal published = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Published Gir Cow")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.PUBLISHED)
                .price(new BigDecimal("50000.00"))
                .build());

        Animal draft = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Draft Buffalo")
                .animalType(AnimalType.BUFFALO)
                .status(AnimalStatus.DRAFT)
                .price(new BigDecimal("40000.00"))
                .build());

        Animal inactive = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Inactive Goat")
                .animalType(AnimalType.GOAT)
                .status(AnimalStatus.INACTIVE)
                .price(new BigDecimal("15000.00"))
                .build());

        mockMvc.perform(get("/api/v1/animals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(published.getId().intValue())))
                .andExpect(jsonPath("$.data.content[0].title", is("Published Gir Cow")));
    }

    @Test
    @DisplayName("6. Listing details respect visibility rules (public vs owner/admin)")
    void shouldRespectVisibilityRulesForListingDetails() throws Exception {
        Animal draft = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Private Draft Listing")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.DRAFT)
                .price(new BigDecimal("50000.00"))
                .build());

        // Anonymous user gets 404
        mockMvc.perform(get("/api/v1/animals/" + draft.getId()))
                .andExpect(status().isNotFound());

        // Other seller gets 404
        mockMvc.perform(get("/api/v1/animals/" + draft.getId())
                        .header("Authorization", "Bearer " + otherSellerToken))
                .andExpect(status().isNotFound());

        // Owner gets 200 OK
        mockMvc.perform(get("/api/v1/animals/" + draft.getId())
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(draft.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("DRAFT")));

        // Admin gets 200 OK
        mockMvc.perform(get("/api/v1/animals/" + draft.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(draft.getId().intValue())));
    }

    @Test
    @DisplayName("7. Update owned listing successfully")
    void shouldUpdateOwnedListing() throws Exception {
        Animal animal = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Original Title")
                .animalType(AnimalType.COW)
                .breed("Desi")
                .ageMonths(30)
                .gender(AnimalGender.FEMALE)
                .price(new BigDecimal("40000.00"))
                .status(AnimalStatus.DRAFT)
                .build());

        AnimalUpdateRequest updateRequest = AnimalUpdateRequest.builder()
                .title("Updated Title")
                .description("Updated description")
                .animalType(AnimalType.COW)
                .breed("Gir Cross")
                .ageMonths(32)
                .gender(AnimalGender.FEMALE)
                .price(new BigDecimal("45000.00"))
                .negotiable(true)
                .state("Uttar Pradesh")
                .district("Varanasi")
                .build();

        mockMvc.perform(put("/api/v1/animals/" + animal.getId())
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Updated Title")))
                .andExpect(jsonPath("$.data.breed", is("Gir Cross")))
                .andExpect(jsonPath("$.data.price", is(45000.00)));

        Animal refreshed = animalRepository.findById(animal.getId()).orElseThrow();
        assertEquals("Updated Title", refreshed.getTitle());
        assertEquals("Gir Cross", refreshed.getBreed());
    }

    @Test
    @DisplayName("8. Reject listing update by another user with 403 Forbidden")
    void shouldRejectUpdateByAnotherUser() throws Exception {
        Animal animal = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Seller 1 Cow")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.DRAFT)
                .price(new BigDecimal("40000.00"))
                .build());

        AnimalUpdateRequest updateRequest = AnimalUpdateRequest.builder()
                .title("Hacked Title")
                .animalType(AnimalType.COW)
                .gender(AnimalGender.FEMALE)
                .ageMonths(20)
                .price(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(put("/api/v1/animals/" + animal.getId())
                        .header("Authorization", "Bearer " + otherSellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN_OPERATION")));
    }

    @Test
    @DisplayName("9. Deactivate listing sets status to INACTIVE without deleting images")
    void shouldDeactivateListingSoftDelete() throws Exception {
        Animal animal = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Listing to Deactivate")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.PUBLISHED)
                .price(new BigDecimal("50000.00"))
                .build());

        AnimalImage img = animalImageRepository.save(AnimalImage.builder()
                .animal(animal)
                .imageUrl("https://res.cloudinary.com/test/image/upload/v1/cow.png")
                .cloudinaryPublicId("test/cow")
                .displayOrder(0)
                .build());

        mockMvc.perform(delete("/api/v1/animals/" + animal.getId())
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("deactivated successfully")));

        Animal refreshed = animalRepository.findById(animal.getId()).orElseThrow();
        assertEquals(AnimalStatus.INACTIVE, refreshed.getStatus());

        // Verify image was NOT deleted
        assertEquals(1, animalImageRepository.countByAnimalId(animal.getId()));
    }

    @Test
    @DisplayName("10. Valid and invalid status transitions")
    void shouldValidateStatusTransitions() throws Exception {
        Animal animal = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Sahiwal Cow")
                .animalType(AnimalType.COW)
                .breed("Sahiwal")
                .ageMonths(36)
                .gender(AnimalGender.FEMALE)
                .price(new BigDecimal("60000.00"))
                .status(AnimalStatus.DRAFT)
                .build());

        // 10a. DRAFT -> PUBLISHED: Valid
        mockMvc.perform(patch("/api/v1/animals/" + animal.getId() + "/status")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnimalStatusUpdateRequest(AnimalStatus.PUBLISHED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")));

        // 10b. PUBLISHED -> SOLD: Valid
        mockMvc.perform(patch("/api/v1/animals/" + animal.getId() + "/status")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnimalStatusUpdateRequest(AnimalStatus.SOLD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SOLD")));

        // 10c. SOLD -> PUBLISHED: Invalid (Sold listings are terminal)
        mockMvc.perform(patch("/api/v1/animals/" + animal.getId() + "/status")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnimalStatusUpdateRequest(AnimalStatus.PUBLISHED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("11. Retrieve seller's own listings across all statuses")
    void shouldRetrieveSellerOwnListings() throws Exception {
        animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Seller Cow 1")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.DRAFT)
                .price(new BigDecimal("30000.00"))
                .build());

        animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Seller Cow 2")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.PUBLISHED)
                .price(new BigDecimal("40000.00"))
                .build());

        animalRepository.save(Animal.builder()
                .user(otherSellerUser)
                .title("Other Seller Buffalo")
                .animalType(AnimalType.BUFFALO)
                .status(AnimalStatus.PUBLISHED)
                .price(new BigDecimal("50000.00"))
                .build());

        mockMvc.perform(get("/api/v1/animals/my-listings")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(2)))
                .andExpect(jsonPath("$.data.content[*].title", containsInAnyOrder("Seller Cow 1", "Seller Cow 2")));
    }

    @Test
    @DisplayName("12. Search and filter published listings by type, breed, price, and location")
    void shouldSearchAndFilterListings() throws Exception {
        animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Pure Murrah Buffalo")
                .animalType(AnimalType.BUFFALO)
                .breed("Murrah")
                .gender(AnimalGender.FEMALE)
                .price(new BigDecimal("70000.00"))
                .status(AnimalStatus.PUBLISHED)
                .district("Karnal")
                .state("Haryana")
                .milkYieldLitresPerDay(new BigDecimal("15.00"))
                .build());

        animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Desi Buffalo")
                .animalType(AnimalType.BUFFALO)
                .breed("Desi")
                .gender(AnimalGender.FEMALE)
                .price(new BigDecimal("40000.00"))
                .status(AnimalStatus.PUBLISHED)
                .district("Patna")
                .state("Bihar")
                .milkYieldLitresPerDay(new BigDecimal("8.00"))
                .build());

        // Search by breed "murrah" and minPrice 50000
        mockMvc.perform(get("/api/v1/animals/search")
                        .param("breed", "murrah")
                        .param("minPrice", "50000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].title", is("Pure Murrah Buffalo")));
    }

    @Test
    @DisplayName("13. Reject invalid search ranges (minPrice > maxPrice)")
    void shouldRejectInvalidSearchRanges() throws Exception {
        mockMvc.perform(get("/api/v1/animals/search")
                        .param("minPrice", "80000.00")
                        .param("maxPrice", "50000.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("14. Reject invalid sorting fields")
    void shouldRejectInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/animals")
                        .param("sortBy", "malicious_column"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("15. Return featured listings deterministically")
    void shouldReturnFeaturedListings() throws Exception {
        for (int i = 1; i <= 8; i++) {
            animalRepository.save(Animal.builder()
                    .user(sellerUser)
                    .title("Animal #" + i)
                    .animalType(AnimalType.COW)
                    .price(new BigDecimal("50000.00"))
                    .status(AnimalStatus.PUBLISHED)
                    .createdAt(LocalDateTime.now().minusHours(8 - i))
                    .build());
        }

        mockMvc.perform(get("/api/v1/animals/featured").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(5)))
                .andExpect(jsonPath("$.data[0].title", is("Animal #8")));
    }

    @Test
    @DisplayName("16. Nearby listings within radius with distance calculation")
    void shouldFindNearbyListings() throws Exception {
        // Location A: Near Varanasi (lat 25.3176, lon 82.9739)
        Animal nearAnimal = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Near Varanasi Cow")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.PUBLISHED)
                .price(new BigDecimal("50000.00"))
                .latitude(new BigDecimal("25.3200000"))
                .longitude(new BigDecimal("82.9800000"))
                .build());

        // Location B: Far away near Delhi (lat 28.6139, lon 77.2090)
        Animal farAnimal = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Far Delhi Cow")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.PUBLISHED)
                .price(new BigDecimal("50000.00"))
                .latitude(new BigDecimal("28.6139000"))
                .longitude(new BigDecimal("77.2090000"))
                .build());

        // Search near Varanasi with 50 km radius
        mockMvc.perform(get("/api/v1/animals/nearby")
                        .param("latitude", "25.3176")
                        .param("longitude", "82.9739")
                        .param("radiusKm", "50.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(nearAnimal.getId().intValue())))
                .andExpect(jsonPath("$.data.content[0].distanceKm", notNullValue()));
    }

    @Test
    @DisplayName("17. Image upload for newly created listing updates thumbnail and appears in detail and browse")
    void shouldUploadImageAndReflectInDetailAndBrowse() throws Exception {
        // 17a. Create listing
        Animal animal = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Newly Created Cow")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.PUBLISHED)
                .price(new BigDecimal("60000.00"))
                .build());

        // 17b. Mock Cloudinary upload
        when(imageStorageService.uploadImage(any(), any())).thenReturn(
                CloudinaryUploadResult.builder()
                        .secureUrl("https://res.cloudinary.com/test/cow_thumb.png")
                        .publicId("test/cow_thumb")
                        .format("png")
                        .bytes(1024L)
                        .build()
        );

        // 17c. Upload image
        byte[] imgBytes = createTestPngBytes();
        MockMultipartFile file = new MockMultipartFile("files", "cow.png", "image/png", imgBytes);

        mockMvc.perform(multipart("/api/v1/animals/" + animal.getId() + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].imageUrl", is("https://res.cloudinary.com/test/cow_thumb.png")));

        // 17d. Browse endpoint includes primaryImageUrl
        mockMvc.perform(get("/api/v1/animals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].primaryImageUrl", is("https://res.cloudinary.com/test/cow_thumb.png")));

        // 17e. Detail endpoint includes images array and primaryImageUrl
        mockMvc.perform(get("/api/v1/animals/" + animal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryImageUrl", is("https://res.cloudinary.com/test/cow_thumb.png")))
                .andExpect(jsonPath("$.data.imageCount", is(1)))
                .andExpect(jsonPath("$.data.images[0].imageUrl", is("https://res.cloudinary.com/test/cow_thumb.png")));
    }

    @Test
    @DisplayName("18. Reject image upload by a different seller with 403 Forbidden")
    void shouldRejectImageUploadByDifferentSeller() throws Exception {
        Animal animal = animalRepository.save(Animal.builder()
                .user(sellerUser)
                .title("Seller 1 Animal")
                .animalType(AnimalType.COW)
                .status(AnimalStatus.PUBLISHED)
                .price(new BigDecimal("60000.00"))
                .build());

        byte[] imgBytes = createTestPngBytes();
        MockMultipartFile file = new MockMultipartFile("files", "cow.png", "image/png", imgBytes);

        mockMvc.perform(multipart("/api/v1/animals/" + animal.getId() + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + otherSellerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN_OPERATION")));
    }
}
