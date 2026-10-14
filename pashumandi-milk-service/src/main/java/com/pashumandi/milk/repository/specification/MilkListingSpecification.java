package com.pashumandi.milk.repository.specification;

import com.pashumandi.milk.dto.listing.MilkListingSearchCriteria;
import com.pashumandi.milk.entity.MilkListing;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MilkListingSpecification {

    public static Specification<MilkListing> buildSearchSpecification(MilkListingSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Rule: Only published listings with stock > 0 are public
            predicates.add(cb.equal(root.get("status"), ListingStatus.PUBLISHED));
            predicates.add(cb.greaterThan(root.get("availableQuantityLitres"), BigDecimal.ZERO));

            if (criteria == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (criteria.getMilkType() != null) {
                predicates.add(cb.equal(root.get("milkType"), criteria.getMilkType()));
            }

            if (criteria.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerLitre"), criteria.getMinPrice()));
            }

            if (criteria.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerLitre"), criteria.getMaxPrice()));
            }

            if (criteria.getMinQuantity() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("availableQuantityLitres"), criteria.getMinQuantity()));
            }

            if (criteria.getMaxQuantity() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("availableQuantityLitres"), criteria.getMaxQuantity()));
            }

            if (criteria.getState() != null && !criteria.getState().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("state")), "%" + criteria.getState().trim().toLowerCase() + "%"));
            }

            if (criteria.getDistrict() != null && !criteria.getDistrict().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("district")), "%" + criteria.getDistrict().trim().toLowerCase() + "%"));
            }

            if (criteria.getPincode() != null && !criteria.getPincode().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("pincode"), criteria.getPincode().trim()));
            }

            if (criteria.getFulfillmentType() != null) {
                if (criteria.getFulfillmentType() == FulfillmentType.DELIVERY) {
                    predicates.add(root.get("fulfillmentType").in(FulfillmentType.DELIVERY, FulfillmentType.BOTH));
                } else if (criteria.getFulfillmentType() == FulfillmentType.PICKUP) {
                    predicates.add(root.get("fulfillmentType").in(FulfillmentType.PICKUP, FulfillmentType.BOTH));
                } else {
                    predicates.add(cb.equal(root.get("fulfillmentType"), FulfillmentType.BOTH));
                }
            }

            if (criteria.getMinFatPercentage() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fatPercentage"), criteria.getMinFatPercentage()));
            }

            if (criteria.getMaxFatPercentage() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fatPercentage"), criteria.getMaxFatPercentage()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
