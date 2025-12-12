package com.example.vmserver.repository;

import com.example.vmserver.model.VMQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VMQueueRepository extends JpaRepository<VMQueue, Long> {
    
    // Найти активную запись по ID станции
    Optional<VMQueue> findByVmStationIdAndActiveTrue(Long stationId);
    
    // Найти активную запись по ID пользователя
    Optional<VMQueue> findByCurrentUserIdAndActiveTrue(Long userId);
    
    // Найти все активные записи для пользователя
    List<VMQueue> findByCurrentUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);
    
    // Найти все неактивные записи
    List<VMQueue> findByActiveFalse();
    
    // Найти все активные записи
    List<VMQueue> findByActiveTrue();
    
    // Проверить, есть ли активная запись для станции
    boolean existsByVmStationIdAndActiveTrue(Long stationId);
    
    // Проверить, есть ли активная запись для пользователя
    boolean existsByCurrentUserIdAndActiveTrue(Long userId);
    
    // Найти запись по станции и пользователю
    @Query("SELECT q FROM VMQueue q WHERE q.vmStation.id = :stationId AND q.currentUser.id = :userId AND q.active = true")
    Optional<VMQueue> findActiveByStationAndUser(@Param("stationId") Long stationId, @Param("userId") Long userId);
}