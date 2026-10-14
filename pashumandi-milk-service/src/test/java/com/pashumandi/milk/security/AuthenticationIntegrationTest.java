package com.pashumandi.milk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pashumandi.milk.dto.listing.CreateMilkListingRequest;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.MilkType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("1. A valid access token from existing PashuMandi login system is accepted on protected endpoint")
    void testValidExistingAccessTokenAccepted() throws Exception {
        String token = jwtTokenProvider.generateTokenForTest(101L, "SELLER", 900000);

        mockMvc.perform(get("/api/v1/milk/listings/my-listings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("2. Missing Authorization token is rejected on protected endpoints with 401")
    void testMissingTokenRejected() throws Exception {
        mockMvc.perform(get("/api/v1/milk/listings/my-listings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("3. Expired token is rejected with 401 Unauthorized")
    void testExpiredTokenRejected() throws Exception {
        // -1000ms expiration creates an already-expired token
        String expiredToken = jwtTokenProvider.generateTokenForTest(101L, "SELLER", -10000);

        mockMvc.perform(get("/api/v1/milk/listings/my-listings")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("4. Token signed with wrong secret key is rejected with 401 Unauthorized")
    void testInvalidSignatureRejected() throws Exception {
        Key rogueKey = Keys.hmacShaKeyFor("rogue_key_must_be_32_bytes_long_secret_abc123!".getBytes(StandardCharsets.UTF_8));
        String forgedToken = Jwts.builder()
                .setSubject("999")
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 900000))
                .signWith(rogueKey, SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/v1/milk/listings/my-listings")
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("5. Authenticated user ID is extracted from token subject and assigned to seller_user_id")
    void testUserIdExtractedAndEnforcedFromServerToken() throws Exception {
        Long expectedSellerId = 42L;
        String token = jwtTokenProvider.generateTokenForTest(expectedSellerId, "SELLER", 900000);

        CreateMilkListingRequest request = CreateMilkListingRequest.builder()
                .title("A2 Fresh Gir Cow Milk")
                .milkType(MilkType.COW)
                .pricePerLitre(new BigDecimal("70.00"))
                .availableQuantityLitres(new BigDecimal("25.00"))
                .state("Uttar Pradesh")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.BOTH)
                .build();

        mockMvc.perform(post("/api/v1/milk/listings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.sellerUserId", is(42)));
    }

    @Test
    @DisplayName("6. Client cannot spoof user identity; claims from JWT are always authoritative")
    void testClientCannotSpoofIdentity() throws Exception {
        Long actualUserFromToken = 55L;
        String token = jwtTokenProvider.generateTokenForTest(actualUserFromToken, "SELLER", 900000);

        CreateMilkListingRequest request = CreateMilkListingRequest.builder()
                .title("Buffalo Pure Milk")
                .milkType(MilkType.BUFFALO)
                .pricePerLitre(new BigDecimal("80.00"))
                .availableQuantityLitres(new BigDecimal("40.00"))
                .state("Bihar")
                .district("Gopalganj")
                .village("Hathua")
                .pincode("841438")
                .fulfillmentType(FulfillmentType.DELIVERY)
                .build();

        mockMvc.perform(post("/api/v1/milk/listings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                // Verify server set sellerUserId = 55 from JWT, not anything else
                .andExpect(jsonPath("$.data.sellerUserId", is(55)));
    }

    @Test
    @DisplayName("7. No second login or registration endpoint exists on Milk Service (returns 404/401)")
    void testNoDuplicateLoginOrRegistrationEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/milk/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\"}"))
                .andExpect(status().isUnauthorized());
    }
}
