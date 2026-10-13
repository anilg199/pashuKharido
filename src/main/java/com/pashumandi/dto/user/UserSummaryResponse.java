package com.pashumandi.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pashumandi.entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String state;
    private String district;
    private String profileImageUrl;

    public static UserSummaryResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserSummaryResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .state(user.getState())
                .district(user.getDistrict())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
