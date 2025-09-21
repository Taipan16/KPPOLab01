package com.example.vmserver.repository;

import com.example.vmserver.model.VMStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VMStationRepository extends JpaRepository<VMStation, Long> {
    //Получить станцию по ip
    VMStation findByIp(String ip);
}
