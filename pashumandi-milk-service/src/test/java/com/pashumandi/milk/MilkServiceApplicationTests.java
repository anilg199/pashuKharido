package com.pashumandi.milk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class MilkServiceApplicationTests {

    @Test
    @DisplayName("Verify Spring Boot application context and configuration beans start cleanly")
    void contextLoads() {
        assertTrue(true);
    }
}
