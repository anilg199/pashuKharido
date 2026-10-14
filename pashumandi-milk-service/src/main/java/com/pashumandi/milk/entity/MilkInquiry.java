package com.pashumandi.milk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "milk_inquiries", indexes = {
        @Index(name = "idx_milk_inq_sender", columnList = "sender_user_id"),
        @Index(name = "idx_milk_inq_seller", columnList = "seller_user_id"),
        @Index(name = "idx_milk_inq_listing", columnList = "listing_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private MilkListing listing;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "seller_user_id", nullable = false)
    private Long sellerUserId;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
