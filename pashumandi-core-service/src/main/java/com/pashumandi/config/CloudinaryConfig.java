package com.pashumandi.config;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CloudinaryConfig {

    private final CloudinaryProperties properties;

    @Bean
    @ConditionalOnMissingBean(Cloudinary.class)
    public Cloudinary cloudinary() {
        String cloudName = StringUtils.hasText(properties.getCloudName()) ? properties.getCloudName().trim() : "xtg7v6vt";
        String apiKey = StringUtils.hasText(properties.getApiKey()) ? properties.getApiKey().trim() : "unconfigured";
        String apiSecret = StringUtils.hasText(properties.getApiSecret()) ? properties.getApiSecret().trim() : "unconfigured";

        if ("unconfigured".equals(apiKey) || "unconfigured".equals(apiSecret)) {
            log.warn("Cloudinary credentials are not fully set in environment variables. Image uploads will require CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET.");
        } else {
            log.info("Configuring Cloudinary client for cloud-name: {}", cloudName);
        }

        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        config.put("secure", true);

        return new Cloudinary(config);
    }
}
