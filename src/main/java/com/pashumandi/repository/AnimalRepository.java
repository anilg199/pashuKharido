package com.pashumandi.repository;

import com.pashumandi.entity.Animal;
import com.pashumandi.enums.AnimalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long>, JpaSpecificationExecutor<Animal> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Animal a WHERE a.id = :id")
    Optional<Animal> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT a FROM Animal a LEFT JOIN FETCH a.images LEFT JOIN FETCH a.user WHERE a.id = :id")
    Optional<Animal> findByIdWithDetails(@Param("id") Long id);

    Page<Animal> findByStatus(AnimalStatus status, Pageable pageable);

    Page<Animal> findByUserId(Long userId, Pageable pageable);

    List<Animal> findTop10ByStatusOrderByCreatedAtDesc(AnimalStatus status);
}
