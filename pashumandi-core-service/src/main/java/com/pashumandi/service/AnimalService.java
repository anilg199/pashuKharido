package com.pashumandi.service;

import com.pashumandi.dto.animal.*;
import com.pashumandi.dto.common.PagedResponse;
import com.pashumandi.entity.Animal;
import com.pashumandi.entity.User;
import com.pashumandi.enums.AnimalGender;
import com.pashumandi.enums.AnimalStatus;
import com.pashumandi.enums.AnimalType;
import com.pashumandi.exception.ResourceNotFoundException;
import com.pashumandi.exception.UnauthorizedAccessException;
import com.pashumandi.repository.AnimalRepository;
import com.pashumandi.repository.AnimalSpecification;
import com.pashumandi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "price", "title", "ageMonths", "updatedAt", "status"
    );
    private static final int MAX_PAGE_SIZE = 50;

    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new animal listing assigned to the authenticated user.
     */
    @Transactional
    public AnimalResponse createAnimal(AnimalCreateRequest request, Long authenticatedUserId) {
        User seller = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found with id: " + authenticatedUserId));

        AnimalStatus initialStatus = request.getStatus() != null ? request.getStatus() : AnimalStatus.DRAFT;
        if (initialStatus != AnimalStatus.DRAFT && initialStatus != AnimalStatus.PUBLISHED) {
            throw new IllegalArgumentException("Initial listing status must be either DRAFT or PUBLISHED.");
        }

        if (initialStatus == AnimalStatus.PUBLISHED) {
            validatePublicationRequirements(
                    request.getTitle(),
                    request.getAnimalType(),
                    request.getGender(),
                    request.getAgeMonths(),
                    request.getPrice()
            );
        }

        Animal animal = Animal.builder()
                .user(seller)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .animalType(request.getAnimalType())
                .breed(request.getBreed())
                .ageMonths(request.getAgeMonths())
                .gender(request.getGender())
                .price(request.getPrice())
                .negotiable(request.getNegotiable() != null ? request.getNegotiable() : false)
                .status(initialStatus)
                .weightKg(request.getWeightKg())
                .milkYieldLitresPerDay(request.getMilkYieldLitresPerDay())
                .lactationStatus(request.getLactationStatus())
                .pregnant(request.getPregnant())
                .vaccinated(request.getVaccinated())
                .healthDescription(request.getHealthDescription())
                .state(request.getState())
                .district(request.getDistrict())
                .village(request.getVillage())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        Animal savedAnimal = animalRepository.save(animal);
        log.info("Created animal listing {} for seller {}", savedAnimal.getId(), seller.getId());
        return AnimalResponse.fromEntity(savedAnimal);
    }

    /**
     * Public browsing of published listings with pagination and sorting.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AnimalResponse> getPublishedAnimals(int page, int size, String sortBy, String direction) {
        Pageable pageable = createValidatedPageable(page, size, sortBy, direction);
        Page<Animal> animalPage = animalRepository.findByStatus(AnimalStatus.PUBLISHED, pageable);
        return PagedResponse.from(animalPage, AnimalResponse::fromEntity);
    }

    /**
     * Retrieves listing details. Public access is granted only if the listing is PUBLISHED.
     * Unpublished listings require the caller to be the owner or an ADMIN.
     */
    @Transactional(readOnly = true)
    public AnimalResponse getAnimalDetails(Long animalId, Long authenticatedUserId, boolean isAdmin) {
        Animal animal = animalRepository.findByIdWithDetails(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal listing not found with id: " + animalId));

        if (animal.getStatus() != AnimalStatus.PUBLISHED) {
            if (!isAdmin && (authenticatedUserId == null || animal.getUser() == null || !animal.getUser().getId().equals(authenticatedUserId))) {
                throw new ResourceNotFoundException("Animal listing not found with id: " + animalId);
            }
        }

        return AnimalResponse.fromEntity(animal);
    }

    /**
     * Updates an owned listing.
     */
    @Transactional
    public AnimalResponse updateAnimal(Long animalId, AnimalUpdateRequest request, Long authenticatedUserId, boolean isAdmin) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal listing not found with id: " + animalId));

        verifyOwnership(animal, authenticatedUserId, isAdmin);

        if (animal.getStatus() == AnimalStatus.SOLD) {
            throw new IllegalArgumentException("Sold listings cannot be modified.");
        }

        if (animal.getStatus() == AnimalStatus.PUBLISHED) {
            validatePublicationRequirements(
                    request.getTitle(),
                    request.getAnimalType(),
                    request.getGender(),
                    request.getAgeMonths(),
                    request.getPrice()
            );
        }

        animal.setTitle(request.getTitle().trim());
        animal.setDescription(request.getDescription());
        animal.setAnimalType(request.getAnimalType());
        animal.setBreed(request.getBreed());
        animal.setAgeMonths(request.getAgeMonths());
        animal.setGender(request.getGender());
        animal.setPrice(request.getPrice());
        animal.setNegotiable(request.getNegotiable() != null ? request.getNegotiable() : false);
        animal.setWeightKg(request.getWeightKg());
        animal.setMilkYieldLitresPerDay(request.getMilkYieldLitresPerDay());
        animal.setLactationStatus(request.getLactationStatus());
        animal.setPregnant(request.getPregnant());
        animal.setVaccinated(request.getVaccinated());
        animal.setHealthDescription(request.getHealthDescription());
        animal.setState(request.getState());
        animal.setDistrict(request.getDistrict());
        animal.setVillage(request.getVillage());
        animal.setPincode(request.getPincode());
        animal.setLatitude(request.getLatitude());
        animal.setLongitude(request.getLongitude());
        animal.setUpdatedAt(LocalDateTime.now());

        Animal updated = animalRepository.save(animal);
        log.info("Updated animal listing {}", animalId);
        return AnimalResponse.fromEntity(updated);
    }

    /**
     * Deactivates a listing by transitioning its status to INACTIVE (soft-delete).
     */
    @Transactional
    public void deactivateAnimal(Long animalId, Long authenticatedUserId, boolean isAdmin) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal listing not found with id: " + animalId));

        verifyOwnership(animal, authenticatedUserId, isAdmin);

        animal.setStatus(AnimalStatus.INACTIVE);
        animal.setUpdatedAt(LocalDateTime.now());
        animalRepository.save(animal);
        log.info("Deactivated animal listing {}", animalId);
    }

    /**
     * Retrieves listings owned by the authenticated seller across all lifecycle statuses.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AnimalResponse> getMyListings(Long authenticatedUserId, int page, int size, String sortBy, String direction) {
        Pageable pageable = createValidatedPageable(page, size, sortBy, direction);
        Page<Animal> animalPage = animalRepository.findByUserId(authenticatedUserId, pageable);
        return PagedResponse.from(animalPage, AnimalResponse::fromEntity);
    }

    /**
     * Updates the status of an animal listing, strictly validating lifecycle transitions.
     */
    @Transactional
    public AnimalResponse updateAnimalStatus(Long animalId, AnimalStatus newStatus, Long authenticatedUserId, boolean isAdmin) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Target status must not be null.");
        }

        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal listing not found with id: " + animalId));

        verifyOwnership(animal, authenticatedUserId, isAdmin);

        AnimalStatus currentStatus = animal.getStatus();
        if (currentStatus == newStatus) {
            return AnimalResponse.fromEntity(animal);
        }

        // Validate lifecycle transitions
        if (currentStatus == AnimalStatus.SOLD) {
            throw new IllegalArgumentException("Sold listings cannot be transitioned to another status.");
        }

        if (newStatus == AnimalStatus.PUBLISHED) {
            validatePublicationRequirements(
                    animal.getTitle(),
                    animal.getAnimalType(),
                    animal.getGender(),
                    animal.getAgeMonths(),
                    animal.getPrice()
            );
        }

        if (currentStatus == AnimalStatus.DRAFT && newStatus == AnimalStatus.SOLD) {
            throw new IllegalArgumentException("Draft listings cannot be marked as sold directly. Listing must be published first.");
        }

        if (currentStatus == AnimalStatus.INACTIVE && newStatus == AnimalStatus.SOLD) {
            throw new IllegalArgumentException("Inactive listings cannot be marked as sold directly. Listing must be published first.");
        }

        animal.setStatus(newStatus);
        animal.setUpdatedAt(LocalDateTime.now());
        Animal saved = animalRepository.save(animal);
        log.info("Transitioned status of animal {} from {} to {}", animalId, currentStatus, newStatus);
        return AnimalResponse.fromEntity(saved);
    }

    /**
     * Returns a deterministic list of featured published listings (most recently published).
     */
    @Transactional(readOnly = true)
    public List<AnimalSummaryResponse> getFeaturedListings(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 20));
        Pageable pageable = PageRequest.of(0, boundedLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Animal> page = animalRepository.findByStatus(AnimalStatus.PUBLISHED, pageable);
        return page.getContent().stream()
                .map(AnimalSummaryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Searches published listings with dynamic filters and pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AnimalResponse> searchAnimals(
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
            Boolean vaccinated,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price.");
        }
        if (minAgeMonths != null && maxAgeMonths != null && minAgeMonths > maxAgeMonths) {
            throw new IllegalArgumentException("Minimum age cannot be greater than maximum age.");
        }
        if (minMilkYield != null && maxMilkYield != null && minMilkYield.compareTo(maxMilkYield) > 0) {
            throw new IllegalArgumentException("Minimum milk yield cannot be greater than maximum milk yield.");
        }

        Pageable pageable = createValidatedPageable(page, size, sortBy, direction);
        Specification<Animal> spec = AnimalSpecification.filterPublishedAnimals(
                animalType, breed, gender, minPrice, maxPrice, minAgeMonths, maxAgeMonths,
                state, district, pincode, minMilkYield, maxMilkYield, pregnant, vaccinated
        );

        Page<Animal> pageResult = animalRepository.findAll(spec, pageable);
        return PagedResponse.from(pageResult, AnimalResponse::fromEntity);
    }

    /**
     * Finds published listings within the specified radius of given latitude and longitude coordinates.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AnimalResponse> getNearbyAnimals(
            Double latitude,
            Double longitude,
            Double radiusKm,
            int page,
            int size
    ) {
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0.");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0.");
        }

        double searchRadius = (radiusKm == null || radiusKm <= 0) ? 50.0 : Math.min(radiusKm, 500.0);
        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        // 1 degree latitude approx 111 km
        double latDelta = searchRadius / 111.0;
        BigDecimal minLat = BigDecimal.valueOf(Math.max(-90.0, latitude - latDelta));
        BigDecimal maxLat = BigDecimal.valueOf(Math.min(90.0, latitude + latDelta));

        double cosLat = Math.cos(Math.toRadians(latitude));
        double lonDelta = cosLat > 0.0001 ? searchRadius / (111.0 * cosLat) : 180.0;
        BigDecimal minLon = BigDecimal.valueOf(Math.max(-180.0, longitude - lonDelta));
        BigDecimal maxLon = BigDecimal.valueOf(Math.min(180.0, longitude + lonDelta));

        Specification<Animal> spec = AnimalSpecification.boundingBoxPublished(minLat, maxLat, minLon, maxLon);
        List<Animal> candidates = animalRepository.findAll(spec);

        // Calculate exact spherical distance using Haversine formula
        record ScoredAnimal(Animal animal, double distance) {}

        List<ScoredAnimal> nearbyList = new ArrayList<>();
        for (Animal a : candidates) {
            if (a.getLatitude() != null && a.getLongitude() != null) {
                double dist = calculateHaversineDistance(
                        latitude, longitude,
                        a.getLatitude().doubleValue(), a.getLongitude().doubleValue()
                );
                if (dist <= searchRadius) {
                    nearbyList.add(new ScoredAnimal(a, dist));
                }
            }
        }

        // Sort by distance ascending
        nearbyList.sort(Comparator.comparingDouble(ScoredAnimal::distance));

        int totalElements = nearbyList.size();
        int totalPages = (int) Math.ceil((double) totalElements / validSize);
        int fromIndex = validPage * validSize;
        int toIndex = Math.min(fromIndex + validSize, totalElements);

        List<AnimalResponse> content;
        if (fromIndex >= totalElements) {
            content = Collections.emptyList();
        } else {
            content = nearbyList.subList(fromIndex, toIndex).stream()
                    .map(scored -> AnimalResponse.fromEntity(scored.animal(), scored.distance()))
                    .collect(Collectors.toList());
        }

        return PagedResponse.<AnimalResponse>builder()
                .content(content)
                .page(validPage)
                .size(validSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(validPage == 0)
                .last(validPage >= totalPages - 1)
                .hasNext(validPage < totalPages - 1)
                .hasPrevious(validPage > 0)
                .build();
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private Pageable createValidatedPageable(int page, int size, String sortBy, String direction) {
        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        String sortProperty = StringUtils.hasText(sortBy) ? sortBy.trim() : "createdAt";
        if (!ALLOWED_SORT_FIELDS.contains(sortProperty)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid sort field '%s'. Allowed sort fields are: %s",
                    sortProperty, ALLOWED_SORT_FIELDS
            ));
        }

        Sort.Direction sortDir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(validPage, validSize, Sort.by(sortDir, sortProperty));
    }

    private void validatePublicationRequirements(
            String title, AnimalType animalType, AnimalGender gender, Integer ageMonths, BigDecimal price
    ) {
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Cannot publish listing: Title is required.");
        }
        if (animalType == null) {
            throw new IllegalArgumentException("Cannot publish listing: Animal type is required.");
        }
        if (gender == null) {
            throw new IllegalArgumentException("Cannot publish listing: Gender is required.");
        }
        if (ageMonths == null || ageMonths < 0) {
            throw new IllegalArgumentException("Cannot publish listing: Valid age in months is required.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cannot publish listing: Price must be greater than zero.");
        }
    }

    private void verifyOwnership(Animal animal, Long authenticatedUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (authenticatedUserId == null || animal.getUser() == null || !animal.getUser().getId().equals(authenticatedUserId)) {
            throw new UnauthorizedAccessException("You are not authorized to modify this animal listing.");
        }
    }
}
