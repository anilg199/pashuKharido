package com.pashumandi.repository;

import com.pashumandi.entity.AnimalImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalImageRepository extends JpaRepository<AnimalImage, Long> {

    List<AnimalImage> findByAnimalIdOrderByDisplayOrderAscIdAsc(Long animalId);

    Optional<AnimalImage> findByAnimalIdAndId(Long animalId, Long id);

    long countByAnimalId(Long animalId);

    @Query("SELECT COALESCE(MAX(ai.displayOrder), -1) FROM AnimalImage ai WHERE ai.animal.id = :animalId")
    int findMaxDisplayOrderByAnimalId(@Param("animalId") Long animalId);
}
