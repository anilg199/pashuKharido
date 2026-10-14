package com.pashumandi.dto.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudinaryUploadResult {

    private String secureUrl;
    private String publicId;
    private String format;
    private Long bytes;
}
