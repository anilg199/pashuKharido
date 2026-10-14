package com.pashumandi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
@EnableJpaAuditing
public class PashuMandiApplication {

    static {
        loadDotEnvIfPresent();
    }

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        if (System.getProperty("spring.profiles.active") == null
                && System.getenv("SPRING_PROFILES_ACTIVE") == null
                && System.getProperty("SPRING_PROFILES_ACTIVE") == null) {
            System.setProperty("spring.profiles.active", "dev");
        }
        SpringApplication.run(PashuMandiApplication.class, args);
    }

    private static void loadDotEnvIfPresent() {
        Path envFile = Paths.get(".env");
        if (!Files.exists(envFile)) {
            envFile = Paths.get(System.getProperty("user.dir", "."), ".env");
        }
        if (Files.exists(envFile)) {
            try {
                List<String> lines = Files.readAllLines(envFile);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int idx = line.indexOf('=');
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }
}
