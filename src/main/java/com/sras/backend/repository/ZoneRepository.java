package com.sras.backend.repository;

import com.sras.backend.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Integer> {
    Optional<Zone> findByZoneCode(String zoneCode);
    boolean existsByZoneCode(String zoneCode);
}
