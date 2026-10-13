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
        if (!StringUtils.hasText(properties.getCloudName())) {
            throw new IllegalStateException("Cloudinary configuration failed: 'cloudinary.cloud-name' is required. " +
                    "Please set the CLOUDINARY_CLOUD_NAME environment variable.");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("Cloudinary configuration failed: 'cloudinary.api-key' is required. " +
                    "Please set the CLOUDINARY_API_KEY environment variable.");
        }
        if (!StringUtils.hasText(properties.getApiSecret())) {
            throw new IllegalStateException("Cloudinary configuration failed: 'cloudinary.api-secret' is required. " +
                    "Please set the CLOUDINARY_API_SECRET environment variable.");
        }

        log.info("Configuring Cloudinary client for cloud-name: {}", properties.getCloudName().trim());

        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", properties.getCloudName().trim());
        config.put("api_key", properties.getApiKey().trim());
        config.put("api_secret", properties.getApiSecret().trim());
        config.put("secure", true);

        return new Cloudinary(config);
    }
}
