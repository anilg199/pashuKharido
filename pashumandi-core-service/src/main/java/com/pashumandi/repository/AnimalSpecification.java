package com.pashumandi.repository;

import com.pashumandi.entity.Animal;
import com.pashumandi.enums.AnimalGender;
import com.pashumandi.enums.AnimalStatus;
import com.pashumandi.enums.AnimalType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AnimalSpecification {

    public static Specification<Animal> filterPublishedAnimals(
            AnimalType animalType,
            String breed,
            AnimalGender gender,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minAgeMonths,
            Integer maxAgeMonths,
            String state,
            String district,
            String pincode,
            BigDecimal minMilkYield,
            BigDecimal maxMilkYield,
            Boolean pregnant,
            Boolean vaccinated
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Strictly filter by PUBLISHED status
            predicates.add(cb.equal(root.get("status"), AnimalStatus.PUBLISHED));

            if (animalType != null) {
                predicates.add(cb.equal(root.get("animalType"), animalType));
            }

            if (StringUtils.hasText(breed)) {
                predicates.add(cb.like(cb.lower(root.get("breed")), "%" + breed.toLowerCase().trim() + "%"));
            }

            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (minAgeMonths != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("ageMonths"), minAgeMonths));
            }

            if (maxAgeMonths != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("ageMonths"), maxAgeMonths));
            }

            if (StringUtils.hasText(state)) {
                predicates.add(cb.like(cb.lower(root.get("state")), "%" + state.toLowerCase().trim() + "%"));
            }

            if (StringUtils.hasText(district)) {
                predicates.add(cb.like(cb.lower(root.get("district")), "%" + district.toLowerCase().trim() + "%"));
            }

            if (StringUtils.hasText(pincode)) {
                predicates.add(cb.equal(root.get("pincode"), pincode.trim()));
            }

            if (minMilkYield != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("milkYieldLitresPerDay"), minMilkYield));
            }

            if (maxMilkYield != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("milkYieldLitresPerDay"), maxMilkYield));
            }

            if (pregnant != null) {
                predicates.add(cb.equal(root.get("pregnant"), pregnant));
            }

            if (vaccinated != null) {
                predicates.add(cb.equal(root.get("vaccinated"), vaccinated));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Animal> boundingBoxPublished(
            BigDecimal minLat, BigDecimal maxLat, BigDecimal minLon, BigDecimal maxLon
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), AnimalStatus.PUBLISHED));
            predicates.add(cb.isNotNull(root.get("latitude")));
            predicates.add(cb.isNotNull(root.get("longitude")));

            if (minLat != null && maxLat != null) {
                predicates.add(cb.between(root.get("latitude"), minLat, maxLat));
            }
            if (minLon != null && maxLon != null) {
                predicates.add(cb.between(root.get("longitude"), minLon, maxLon));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
